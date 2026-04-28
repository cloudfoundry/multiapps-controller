package org.cloudfoundry.multiapps.controller.web.configuration.bean.factory;

import com.google.common.base.Joiner;
import io.pivotal.cfenv.core.CfService;
import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.core.util.UriUtil;
import org.cloudfoundry.multiapps.controller.persistence.services.AwsS3ObjectStoreFileStorage;
import org.cloudfoundry.multiapps.controller.persistence.services.AzureObjectStoreFileStorage;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorage;
import org.cloudfoundry.multiapps.controller.persistence.services.GcpObjectStoreFileStorage;
import org.cloudfoundry.multiapps.controller.persistence.services.JCloudsObjectStoreFileStorage;
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

public class ObjectStoreFileStorageFactoryBean implements FactoryBean<FileStorage>, InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(ObjectStoreFileStorageFactoryBean.class);

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
        LOGGER.info(Messages.OBJECT_STORE_PROVIDERS_DETECTED, providersServiceInfo.stream()
                                                                                  .map(ObjectStoreServiceInfo::getProvider)
                                                                                  .collect(Collectors.joining(", ")));
        if (providersServiceInfo.isEmpty()) {
            LOGGER.warn(Messages.NO_OBJECT_STORE_PROVIDERS_DETECTED, serviceName);
            return null;
        }
        Map<String, Exception> exceptions = new HashMap<>();
        String objectStoreProviderName = applicationConfiguration.getObjectStoreClientType();
        if (!isObjectStoreEnvValid(objectStoreProviderName)) {
            return createObjectStoreFromFirstReachableProvider(exceptions, providersServiceInfo);
        }
        Optional<FileStorage> optionalFileStorage = createObjectStoreBasedOnProvider(objectStoreProviderName, providersServiceInfo,
                                                                                     exceptions);
        if (optionalFileStorage.isPresent()) {
            return optionalFileStorage.get();
        }
        throw buildNoValidObjectStoreException(exceptions);
    }

    public List<ObjectStoreServiceInfo> getProvidersServiceInfo() {
        Map<String, Object> credentials = getServiceCredentials();
        if (credentials.isEmpty()) {
            return Collections.emptyList();
        }
        return new ObjectStoreServiceInfoCreator().getAllProvidersServiceInfo(credentials);
    }

    private Map<String, Object> getServiceCredentials() {
        CfService service = environmentServicesFinder.findService(serviceName);
        if (service == null) {
            return Map.of();
        }
        return service.getCredentials()
                      .getMap();
    }

    private boolean isObjectStoreEnvValid(String objectStoreProviderName) {
        return objectStoreProviderName != null && !objectStoreProviderName.isEmpty() && Constants.ENV_TO_OS_PROVIDER.containsKey(
            objectStoreProviderName);
    }

    public FileStorage createObjectStoreFromFirstReachableProvider(Map<String, Exception> exceptions,
                                                                   List<ObjectStoreServiceInfo> providersServiceInfo) {
        for (ObjectStoreServiceInfo objectStoreServiceInfo : providersServiceInfo) {
            Optional<FileStorage> createdObjectStore = tryToCreateObjectStore(objectStoreServiceInfo, exceptions);
            if (createdObjectStore.isPresent()) {
                return createdObjectStore.get();
            }
        }
        throw buildNoValidObjectStoreException(exceptions);
    }

    private Optional<FileStorage> tryToCreateObjectStore(ObjectStoreServiceInfo objectStoreServiceInfo,
                                                         Map<String, Exception> exceptions) {
        try {
            LOGGER.info(Messages.ATTEMPTING_TO_CREATE_OBJECT_STORE_CLIENT,
                        objectStoreServiceInfo.getProvider(),
                        objectStoreServiceInfo.getCredentials()
                                              .get(Constants.BUCKET),
                        objectStoreServiceInfo.getCredentials()
                                              .get(Constants.REGION),
                        objectStoreServiceInfo.getCredentials()
                                              .get(Constants.HOST),
                        objectStoreServiceInfo.getCredentials()
                                              .get(Constants.ENDPOINT));
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
        return switch (objectStoreServiceInfo.getProvider()) {
            case Constants.GOOGLE_CLOUD_STORAGE -> createGcpFileStorage(objectStoreServiceInfo);
            case Constants.AZUREBLOB -> createAzureFileStorage(objectStoreServiceInfo);
            case Constants.AWS_S_3 -> createAwsS3FileStorage(objectStoreServiceInfo);
            default -> {
                BlobStoreContext context = getBlobStoreContext(objectStoreServiceInfo);
                yield createFileStorage(objectStoreServiceInfo, context);
            }
        };
    }

    protected GcpObjectStoreFileStorage createGcpFileStorage(ObjectStoreServiceInfo objectStoreServiceInfo) {
        return new GcpObjectStoreFileStorage(objectStoreServiceInfo.getCredentials());
    }

    protected AzureObjectStoreFileStorage createAzureFileStorage(ObjectStoreServiceInfo objectStoreServiceInfo) {
        return new AzureObjectStoreFileStorage(objectStoreServiceInfo.getCredentials());
    }

    protected AwsS3ObjectStoreFileStorage createAwsS3FileStorage(ObjectStoreServiceInfo objectStoreServiceInfo) {
        return new AwsS3ObjectStoreFileStorage(objectStoreServiceInfo.getCredentials());
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

    private void applyCredentials(ObjectStoreServiceInfo serviceInfo, ContextBuilder contextBuilder) {
        Map<String, Object> credentials = serviceInfo.getCredentials();
        String identity = (String) credentials.get(Constants.ACCESS_KEY_ID);
        String credential = (String) credentials.get(Constants.SECRET_ACCESS_KEY);
        if (StringUtils.isBlank(identity) || StringUtils.isBlank(credential)) {
            throw new IllegalArgumentException(Messages.MISSING_PROPERTIES_FOR_CREATING_THE_SPECIFIC_PROVIDER);
        }
        contextBuilder.credentials(identity, credential);
    }

    private void addCustomRegions(ContextBuilder contextBuilder) {
        Properties properties = new Properties();
        Set<String> mergedRegions = Stream.of(Constants.OBJECT_STORE_CUSTOM_REGIONS, Region.DEFAULT_REGIONS,
                                              applicationConfiguration.getObjectStoreRegions())
                                          .flatMap(Set::stream)
                                          .collect(Collectors.toSet());
        properties.setProperty(Constants.OBJECT_STORE_JCLOUDS_REGIONS, Joiner.on(',')
                                                                             .join(mergedRegions));
        contextBuilder.overrides(properties);
    }

    private void resolveContextEndpoint(ObjectStoreServiceInfo serviceInfo, ContextBuilder contextBuilder) {
        Map<String, Object> credentials = serviceInfo.getCredentials();
        String endpoint = (String) credentials.get(Constants.ENDPOINT);
        String host = (String) credentials.get(Constants.HOST);
        if (StringUtils.isNotEmpty(endpoint)) {
            contextBuilder.endpoint(endpoint);
            return;
        }
        if (StringUtils.isNotEmpty(host)) {
            contextBuilder.endpoint(UriUtil.HTTPS_PROTOCOL + UriUtil.DEFAULT_SCHEME_SEPARATOR + host);
        }
    }

    protected JCloudsObjectStoreFileStorage createFileStorage(ObjectStoreServiceInfo objectStoreServiceInfo, BlobStoreContext context) {
        return new JCloudsObjectStoreFileStorage(context.getBlobStore(),
                                                 (String) objectStoreServiceInfo.getCredentials()
                                                                                .get(Constants.BUCKET));
    }

    private Optional<FileStorage> createObjectStoreBasedOnProvider(String objectStoreProviderName,
                                                                   List<ObjectStoreServiceInfo> providersServiceInfo,
                                                                   Map<String, Exception> exceptions) {
        Optional<ObjectStoreServiceInfo> objectStoreServiceInfoOptional = getAppropriateProvider(objectStoreProviderName,
                                                                                                 providersServiceInfo);
        if (objectStoreServiceInfoOptional.isEmpty()) {
            LOGGER.warn(MessageFormat.format(Messages.NO_OBJECTSTORE_PROVIDER_FOUND_FOR_0, objectStoreProviderName));
            return Optional.empty();
        }
        return tryToCreateObjectStore(objectStoreServiceInfoOptional.get(), exceptions);
    }

    private Optional<ObjectStoreServiceInfo> getAppropriateProvider(String objectStoreProviderName,
                                                                    List<ObjectStoreServiceInfo> providersServiceInfo) {
        String appropriateProvider = Constants.ENV_TO_OS_PROVIDER.get(objectStoreProviderName);
        return providersServiceInfo.stream()
                                   .filter(provider -> appropriateProvider.equals(provider.getProvider()))
                                   .findFirst();
    }

    private IllegalStateException buildNoValidObjectStoreException(Map<String, Exception> exceptions) {
        exceptions.forEach((provider, exception) -> LOGGER.error(
            MessageFormat.format(Messages.CANNOT_CREATE_OBJECT_STORE_CLIENT_WITH_PROVIDER_0, provider),
            exception));
        return new IllegalStateException(Messages.NO_VALID_OBJECT_STORE_CONFIGURATION_FOUND);
    }

    @Override
    public FileStorage getObject() {
        return objectStoreFileStorage;
    }

    @Override
    public Class<?> getObjectType() {
        return FileStorage.class;
    }

}