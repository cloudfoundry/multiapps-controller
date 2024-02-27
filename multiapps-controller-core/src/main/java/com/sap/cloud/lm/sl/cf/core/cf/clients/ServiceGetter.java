package com.sap.cloud.lm.sl.cf.core.cf.clients;

import java.util.Map;

import javax.inject.Inject;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class ServiceGetter {

    private AbstractServiceGetter serviceInstanceGetter;
    private AbstractServiceGetter userProvidedServiceInstanceGetter;

    @Inject
    public ServiceGetter(@Qualifier("serviceInstanceGetter") AbstractServiceGetter serviceInstanceGetter,
                         @Qualifier("userProvidedServiceInstanceGetter") AbstractServiceGetter userProvidedServiceInstanceGetter) {

        this.serviceInstanceGetter = serviceInstanceGetter;
        this.userProvidedServiceInstanceGetter = userProvidedServiceInstanceGetter;
    }

    public Map<String, Object> getServiceInstance(CloudControllerClient client, String serviceName, String spaceId) {
        Map<String, Object> serviceInstance = serviceInstanceGetter.getServiceInstance(client, serviceName, spaceId);
        if (serviceInstance == null || serviceInstance.isEmpty()) {
            serviceInstance = userProvidedServiceInstanceGetter.getServiceInstance(client, serviceName, spaceId);
        }
        return serviceInstance;
    }

    public Map<String, Object> getServiceInstanceEntity(CloudControllerClient client, String serviceName, String spaceId) {
        Map<String, Object> serviceInstance = serviceInstanceGetter.getServiceInstanceEntity(client, serviceName, spaceId);
        if (serviceInstance == null || serviceInstance.isEmpty()) {
            serviceInstance = userProvidedServiceInstanceGetter.getServiceInstanceEntity(client, serviceName, spaceId);
        }
        return serviceInstance;
    }
}
