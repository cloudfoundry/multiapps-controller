package org.cloudfoundry.multiapps.controller.process.util;

import java.util.UUID;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceBinding;

public class ServiceBindingUtil {

    private ServiceBindingUtil() {
    }
    
    public static CloudServiceBinding getServiceBinding(CloudControllerClient client, UUID applicationGuid, String serviceName) {
        return client.getServiceBindingForApplication(applicationGuid, client.getRequiredServiceInstanceGuid(serviceName));
    }

}
