package org.cloudfoundry.multiapps.controller.web.configuration.bean.factory;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.pivotal.cfenv.core.CfService;
import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.multiapps.controller.core.util.UriUtil;
import org.cloudfoundry.multiapps.controller.persistence.services.ObjectStoreFileStorage;
import org.cloudfoundry.multiapps.controller.persistence.util.EnvironmentServicesFinder;
import org.cloudfoundry.multiapps.controller.web.Messages;
import org.cloudfoundry.multiapps.controller.web.configuration.service.ObjectStoreServiceInfo;
import org.cloudfoundry.multiapps.controller.web.configuration.service.ObjectStoreServiceInfoCreator;
import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStoreContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class ObjectStoreFileStorageFactoryBean implements FactoryBean<ObjectStoreFileStorage>, InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(ObjectStoreFileStorageFactoryBean.class);

    private final String serviceName;
    private final EnvironmentServicesFinder environmentServicesFinder;
    private ObjectStoreFileStorage objectStoreFileStorage;

    public ObjectStoreFileStorageFactoryBean(String serviceName, EnvironmentServicesFinder environmentServicesFinder) {
        this.serviceName = serviceName;
        this.environmentServicesFinder = environmentServicesFinder;
    }

    @Override
    public void afterPropertiesSet() {
        this.objectStoreFileStorage = createObjectStoreFileStorage();
    }

    private ObjectStoreFileStorage createObjectStoreFileStorage() {
        List<ObjectStoreServiceInfo> providersServiceInfo = getProvidersServiceInfo();
        if (providersServiceInfo.isEmpty()) {
            return null;
        }

        Map<String, Exception> exceptions = new HashMap<>();

        for (ObjectStoreServiceInfo objectStoreServiceInfo : providersServiceInfo) {
            try {
                BlobStoreContext context = getBlobStoreContext(objectStoreServiceInfo);
                ObjectStoreFileStorage fileStorage = createFileStorage(objectStoreServiceInfo, context);
                fileStorage.testConnection();
                LOGGER.info(MessageFormat.format(Messages.OBJECT_STORE_WITH_PROVIDER_0_CREATED, objectStoreServiceInfo.getProvider()));
                return fileStorage;
            } catch (Exception e) {
                exceptions.put(objectStoreServiceInfo.getProvider(), e);
            }
        }
        
        exceptions.forEach(
            (provider, exception) -> LOGGER.error(
                MessageFormat.format(Messages.CANNOT_CREATE_OBJECT_STORE_CLIENT_WITH_PROVIDER_0, provider),
                exception));
        throw new IllegalStateException(Messages.NO_VALID_OBJECT_STORE_CONFIGURATION_FOUND);
    }

    private List<ObjectStoreServiceInfo> getProvidersServiceInfo() {
        CfService service = environmentServicesFinder.findService(serviceName);
        if (service == null) {
            return Collections.emptyList();
        }
        return new ObjectStoreServiceInfoCreator().getAllProvidersServiceInfo(service);
    }

    private BlobStoreContext getBlobStoreContext(ObjectStoreServiceInfo serviceInfo) {
        ContextBuilder contextBuilder = ContextBuilder.newBuilder(serviceInfo.getProvider());

        if (serviceInfo.getCredentialsSupplier() != null) {
            contextBuilder.credentialsSupplier(serviceInfo.getCredentialsSupplier());
        } else if (serviceInfo.getIdentity() != null && serviceInfo.getCredential() != null) {
            contextBuilder.credentials(serviceInfo.getIdentity(), serviceInfo.getCredential());
        } else {
            throw new IllegalArgumentException(Messages.MISSING_PROPERTIES_FOR_CREATING_THE_SPECIFIC_PROVIDER);
        }

        resolveContextEndpoint(serviceInfo, contextBuilder);

        try {
            return contextBuilder.buildView(BlobStoreContext.class);
        } catch (NullPointerException nullPointerException) {
            throw new IllegalArgumentException("JClouds failed to build BlobStoreContext. Possible missing credentials.",
                                               nullPointerException);
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

    @Override
    public ObjectStoreFileStorage getObject() {
        return objectStoreFileStorage;
    }

    @Override
    public Class<?> getObjectType() {
        return ObjectStoreFileStorage.class;
    }

}