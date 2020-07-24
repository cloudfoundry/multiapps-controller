package org.cloudfoundry.multiapps.controller.core.cf.clients;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.springframework.beans.factory.annotation.Qualifier;

@Named
public class ServiceGetter {

    private final AbstractServiceGetter serviceInstanceGetter;
    private final AbstractServiceGetter userProvidedServiceInstanceGetter;

    @Inject
    public ServiceGetter(@Qualifier("serviceInstanceGetter") AbstractServiceGetter serviceInstanceGetter,
                         @Qualifier("userProvidedServiceInstanceGetter") AbstractServiceGetter userProvidedServiceInstanceGetter) {

        this.serviceInstanceGetter = serviceInstanceGetter;
        this.userProvidedServiceInstanceGetter = userProvidedServiceInstanceGetter;
    }

    public Map<String, Object> getServiceInstanceEntity(CloudControllerClient client, String serviceName, String spaceId) {
        Map<String, Object> serviceInstance = serviceInstanceGetter.getServiceInstanceEntity(client, serviceName, spaceId);
        if (serviceInstance == null || serviceInstance.isEmpty()) {
            serviceInstance = userProvidedServiceInstanceGetter.getServiceInstanceEntity(client, serviceName, spaceId);
        }
        return serviceInstance;
    }

}
