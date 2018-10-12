package com.sap.cloud.lm.sl.cf.core.cf.clients;

import java.util.Map;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class ServiceGetter {

    @Autowired
    @Qualifier("serviceInstanceGetter")
    private AbstractServiceGetter serviceInstanceGetter;
    
    @Autowired
    @Qualifier("userProvidedServiceInstanceGetter")
    private AbstractServiceGetter userProvidedServiceInstanceGetter;
    
    public Map<String, Object> getServiceInstance(CloudControllerClient client, String serviceName, String spaceId) {
        Map<String, Object> serviceInstance = serviceInstanceGetter.getServiceInstance(client, serviceName, spaceId);
        if(serviceInstance == null || serviceInstance.isEmpty()) {
            serviceInstance = userProvidedServiceInstanceGetter.getServiceInstance(client, serviceName, spaceId);
        }
        return serviceInstance;
    }
    
    public Map<String, Object> getServiceInstanceEntity(CloudControllerClient client, String serviceName, String spaceId) {
        Map<String, Object> serviceInstance = serviceInstanceGetter.getServiceInstanceEntity(client, serviceName, spaceId);
        if(serviceInstance == null || serviceInstance.isEmpty()) {
            serviceInstance = userProvidedServiceInstanceGetter.getServiceInstanceEntity(client, serviceName, spaceId);
        }
        return serviceInstance;
    } 
}
