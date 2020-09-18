package org.cloudfoundry.multiapps.controller.web.configuration.bean.factory;

import org.cloudfoundry.multiapps.controller.persistence.services.ObjectStoreFileStorage;
import org.cloudfoundry.multiapps.controller.web.configuration.service.ObjectStoreServiceInfo;
import org.cloudfoundry.multiapps.controller.web.configuration.service.ObjectStoreServiceInfoCreator;
import org.cloudfoundry.multiapps.controller.web.util.EnvironmentServicesFinder;
import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStoreContext;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import io.pivotal.cfenv.core.CfService;

public class ObjectStoreFileStorageFactoryBean implements FactoryBean<ObjectStoreFileStorage>, InitializingBean {

    private final String serviceName;
    private final EnvironmentServicesFinder environmentServicesFinder;
    private ObjectStoreFileStorage objectStoreFileService;

    public ObjectStoreFileStorageFactoryBean(String serviceName, EnvironmentServicesFinder environmentServicesFinder) {
        this.serviceName = serviceName;
        this.environmentServicesFinder = environmentServicesFinder;
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
        ContextBuilder contextBuilder = ContextBuilder.newBuilder(serviceInfo.getProvider())
                                                      .credentials(serviceInfo.getIdentity(), serviceInfo.getCredential());
        if (serviceInfo.getEndpoint() != null) {
            contextBuilder.endpoint(serviceInfo.getEndpoint());
        }
        blobStoreContext = contextBuilder.buildView(BlobStoreContext.class);
        return blobStoreContext;
    }

    private ObjectStoreServiceInfo getServiceInfo() {
        CfService service = environmentServicesFinder.findService(serviceName);
        if (service == null) {
            return null;
        }
        return new ObjectStoreServiceInfoCreator().createServiceInfo(service);
    }

    @Override
    public ObjectStoreFileStorage getObject() {
        return objectStoreFileService;
    }

    @Override
    public Class<?> getObjectType() {
        return ObjectStoreFileStorage.class;
    }

}
