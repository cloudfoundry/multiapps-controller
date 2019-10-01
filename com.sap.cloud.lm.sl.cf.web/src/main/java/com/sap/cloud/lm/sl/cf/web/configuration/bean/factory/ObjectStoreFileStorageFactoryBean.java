package com.sap.cloud.lm.sl.cf.web.configuration.bean.factory;

import java.text.MessageFormat;

import org.apache.commons.lang3.StringUtils;
import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStoreContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.Cloud;
import org.springframework.cloud.CloudException;
import org.springframework.cloud.CloudFactory;
import org.springframework.cloud.service.ServiceInfo;

import com.sap.cloud.lm.sl.cf.persistence.services.ObjectStoreFileStorage;
import com.sap.cloud.lm.sl.cf.web.configuration.service.ObjectStoreServiceInfo;

public class ObjectStoreFileStorageFactoryBean implements FactoryBean<ObjectStoreFileStorage>, InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(ObjectStoreFileStorageFactoryBean.class);

    private final String serviceName;
    private ObjectStoreFileStorage objectStoreFileService;

    public ObjectStoreFileStorageFactoryBean(String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    public void afterPropertiesSet() {
        this.objectStoreFileService = createObjectStoreFileStorage();
    }

    private ObjectStoreFileStorage createObjectStoreFileStorage() {
        BlobStoreContext context = getBlobStoreContext();
        return context == null ? null : new ObjectStoreFileStorage(context.getBlobStore(), getServiceInfo().getContainer());
    }

    private BlobStoreContext getBlobStoreContext() {
        BlobStoreContext blobStoreContext;
        ObjectStoreServiceInfo serviceInfo = getServiceInfo();
        if (serviceInfo == null) {
            return null;
        }
        blobStoreContext = ContextBuilder.newBuilder(serviceInfo.getProvider())
                                         .credentials(serviceInfo.getIdentity(), serviceInfo.getCredential())
                                         .buildView(BlobStoreContext.class);
        return blobStoreContext;
    }

    private ObjectStoreServiceInfo getServiceInfo() {
        if (StringUtils.isEmpty(serviceName)) {
            LOGGER.warn("service name not specified in config files");
            return null;
        }
        try {
            CloudFactory cloudFactory = new CloudFactory();
            Cloud cloud = cloudFactory.getCloud();
            ServiceInfo serviceInfo = cloud.getServiceInfo(serviceName);
            if (serviceInfo instanceof ObjectStoreServiceInfo) {
                return (ObjectStoreServiceInfo) serviceInfo;
            }
            LOGGER.warn("Service instance did not match allowed label and plans.");
        } catch (CloudException e) {
            LOGGER.warn(MessageFormat.format("Failed to detect service info for service \"{0}\"!", serviceName), e);
        }
        return null;
    }

    @Override
    public ObjectStoreFileStorage getObject() {
        return objectStoreFileService;
    }

    @Override
    public Class<?> getObjectType() {
        return ObjectStoreFileStorage.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
