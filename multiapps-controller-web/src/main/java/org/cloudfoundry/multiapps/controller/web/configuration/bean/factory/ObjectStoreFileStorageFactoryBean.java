package org.cloudfoundry.multiapps.controller.web.configuration.bean.factory;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Joiner;
import io.pivotal.cfenv.core.CfService;
import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.core.util.UriUtil;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorage;
import org.cloudfoundry.multiapps.controller.persistence.services.GcpObjectStoreFileStorage;
import org.cloudfoundry.multiapps.controller.persistence.services.ObjectStoreFileStorage;
import org.cloudfoundry.multiapps.controller.persistence.util.EnvironmentServicesFinder;
import org.cloudfoundry.multiapps.controller.web.Constants;
import org.cloudfoundry.multiapps.controller.web.Messages;
import org.cloudfoundry.multiapps.controller.web.configuration.service.ObjectStoreServiceInfo;
import org.cloudfoundry.multiapps.controller.web.configuration.service.ObjectStoreServiceInfoCreator;
import org.jclouds.ContextBuilder;
import org.jclouds.aws.domain.Region;
import org.jclouds.blobstore.BlobStoreContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class ObjectStoreFileStorageFactoryBean implements FactoryBean<FileStorage>, InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(ObjectStoreFileStorageFactoryBean.class);
    private static final Set<String> CUSTOM_REGIONS = Set.of("eu-south-1");
    private static final String JCLOUDS_REGIONS = "jclouds.regions";

    private final String serviceName;
    private final EnvironmentServicesFinder environmentServicesFinder;
    private final ApplicationConfiguration applicationConfiguration;
    private FileStorage objectStoreFileStorage;

    public ObjectStoreFileStorageFactoryBean(String serviceName, EnvironmentServicesFinder environmentServicesFinder,
                                             ApplicationConfiguration applicationConfiguration) {
        this.serviceName = serviceName;
        this.environmentServicesFinder = environmentServicesFinder;
        this.applicationConfiguration = applicationConfiguration;
    }

    @Override
    public void afterPropertiesSet() {
        this.objectStoreFileStorage = createObjectStoreFileStorage();
    }

    private FileStorage createObjectStoreFileStorage() {
        List<ObjectStoreServiceInfo> providersServiceInfo = getProvidersServiceInfo();
        if (providersServiceInfo.isEmpty()) {
            return null;
        }
        Map<String, Exception> exceptions = new HashMap<>();
        String objectStoreProviderName = applicationConfiguration.getObjectStoreClientType();
        if (!isObjectStoreEnvValid(objectStoreProviderName)) {
            return createObjectStoreFromFirstReachableProvider(exceptions, providersServiceInfo);
        }

        Optional<ObjectStoreServiceInfo> objectStoreServiceInfoOptional = getAppropriateProvider(objectStoreProviderName,
                                                                                                 providersServiceInfo);

        if (objectStoreServiceInfoOptional.isPresent()) {
            ObjectStoreServiceInfo objectStoreServiceInfo = objectStoreServiceInfoOptional.get();
            Optional<FileStorage> createdObjectStore = tryToCreateObjectStore(objectStoreServiceInfo, exceptions);
            if (createdObjectStore.isPresent()) {
                return createdObjectStore.get();
            }
        }

        throw buildNoValidObjectStoreException(exceptions);
    }

    public FileStorage createObjectStoreFromFirstReachableProvider(Map<String, Exception> exceptions,
                                                                   List<ObjectStoreServiceInfo> providersServiceInfo) {
        for (ObjectStoreServiceInfo objectStoreServiceInfo : providersServiceInfo) {
            Optional<FileStorage> createdObjectStoreOptional = tryToCreateObjectStore(objectStoreServiceInfo, exceptions);
            if (createdObjectStoreOptional.isPresent()) {
                return createdObjectStoreOptional.get();
            }
        }

        throw buildNoValidObjectStoreException(exceptions);
    }

    private Optional<ObjectStoreServiceInfo> getAppropriateProvider(String objectStoreProviderName,
                                                                    List<ObjectStoreServiceInfo> providersServiceInfo) {
        String appropriateProvider = Constants.ENV_TO_OS_PROVIDER.get(objectStoreProviderName);
        return providersServiceInfo.stream()
                                   .filter(provider -> appropriateProvider.equals(provider.getProvider()))
                                   .findFirst();
    }

    private boolean isObjectStoreEnvValid(String objectStoreProviderName) {
        return objectStoreProviderName != null && !objectStoreProviderName.isEmpty() && Constants.ENV_TO_OS_PROVIDER.containsKey(
            objectStoreProviderName);
    }

    private IllegalStateException buildNoValidObjectStoreException(Map<String, Exception> exceptions) {
        exceptions.forEach((provider, exception) -> LOGGER.error(
            MessageFormat.format(Messages.CANNOT_CREATE_OBJECT_STORE_CLIENT_WITH_PROVIDER_0, provider),
            exception));
        return new IllegalStateException(Messages.NO_VALID_OBJECT_STORE_CONFIGURATION_FOUND);
    }

    private Optional<FileStorage> tryToCreateObjectStore(ObjectStoreServiceInfo objectStoreServiceInfo,
                                                         Map<String, Exception> exceptions) {
        try {
            FileStorage fileStorage = getFileStorageBasedOnProvider(objectStoreServiceInfo);
            fileStorage.testConnection();
            LOGGER.info(MessageFormat.format(Messages.OBJECT_STORE_WITH_PROVIDER_0_CREATED, objectStoreServiceInfo.getProvider()));
            return Optional.of(fileStorage);
        } catch (Exception e) {
            exceptions.put(objectStoreServiceInfo.getProvider(), e);
            return Optional.empty();
        }
    }

    private FileStorage getFileStorageBasedOnProvider(ObjectStoreServiceInfo objectStoreServiceInfo) {
        if (Constants.GOOGLE_CLOUD_STORAGE.equals(objectStoreServiceInfo.getProvider())) {
            return createGcpFileStorage(objectStoreServiceInfo);
        } else {
            BlobStoreContext context = getBlobStoreContext(objectStoreServiceInfo);
            return createFileStorage(objectStoreServiceInfo, context);
        }
    }

    public List<ObjectStoreServiceInfo> getProvidersServiceInfo() {
        CfService service = environmentServicesFinder.findService(serviceName);
        if (service == null) {
            return Collections.emptyList();
        }
        return new ObjectStoreServiceInfoCreator().getAllProvidersServiceInfo(service);
    }

    private BlobStoreContext getBlobStoreContext(ObjectStoreServiceInfo serviceInfo) {
        ContextBuilder contextBuilder = ContextBuilder.newBuilder(serviceInfo.getProvider());
        applyCredentials(serviceInfo, contextBuilder);
        addCustomRegions(contextBuilder);
        resolveContextEndpoint(serviceInfo, contextBuilder);

        BlobStoreContext context = contextBuilder.buildView(BlobStoreContext.class);
        if (context == null) {
            throw new IllegalStateException(Messages.FAILED_TO_CREATE_BLOB_STORE_CONTEXT);
        }

        return context;
    }

    private void addCustomRegions(ContextBuilder contextBuilder) {
        Properties properties = new Properties();
        Set<String> mergedRegions = Stream.of(CUSTOM_REGIONS, Region.DEFAULT_REGIONS, applicationConfiguration.getObjectStoreRegions())
                                          .flatMap(Set::stream)
                                          .collect(Collectors.toSet());
        properties.setProperty(JCLOUDS_REGIONS, Joiner.on(',')
                                                      .join(mergedRegions));

        contextBuilder.overrides(properties);
    }

    private void applyCredentials(ObjectStoreServiceInfo serviceInfo, ContextBuilder contextBuilder) {
        if (serviceInfo.getCredentialsSupplier() != null) {
            contextBuilder.credentialsSupplier(serviceInfo.getCredentialsSupplier());
        } else {
            String identity = serviceInfo.getIdentity();
            String credential = serviceInfo.getCredential();

            if (StringUtils.isBlank(identity) || StringUtils.isBlank(credential)) {
                throw new IllegalArgumentException(Messages.MISSING_PROPERTIES_FOR_CREATING_THE_SPECIFIC_PROVIDER);
            }

            contextBuilder.credentials(identity, credential);
        }
    }

    private void resolveContextEndpoint(ObjectStoreServiceInfo serviceInfo, ContextBuilder contextBuilder) {
        if (StringUtils.isNotEmpty(serviceInfo.getEndpoint())) {
            contextBuilder.endpoint(serviceInfo.getEndpoint());
            return;
        }
        if (StringUtils.isNotEmpty(serviceInfo.getHost())) {
            contextBuilder.endpoint(UriUtil.HTTPS_PROTOCOL + UriUtil.DEFAULT_SCHEME_SEPARATOR + serviceInfo.getHost());
        }
    }

    protected ObjectStoreFileStorage createFileStorage(ObjectStoreServiceInfo objectStoreServiceInfo, BlobStoreContext context) {
        return new ObjectStoreFileStorage(context.getBlobStore(), objectStoreServiceInfo.getContainer());
    }

    protected GcpObjectStoreFileStorage createGcpFileStorage(ObjectStoreServiceInfo objectStoreServiceInfo) {
        return new GcpObjectStoreFileStorage(objectStoreServiceInfo.getContainer(), objectStoreServiceInfo.getGcpStorage());
    }

    @Override
    public FileStorage getObject() {
        return objectStoreFileStorage;
    }

    @Override
    public Class<?> getObjectType() {
        return ObjectStoreFileStorage.class;
    }

}