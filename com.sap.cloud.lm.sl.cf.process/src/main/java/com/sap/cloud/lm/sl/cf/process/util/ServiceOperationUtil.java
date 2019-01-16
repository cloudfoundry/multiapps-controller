package com.sap.cloud.lm.sl.cf.process.util;

import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.ServiceKey;

import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;

public class ServiceOperationUtil {
    
    public static Map<String, Object> getServiceKeyCredentials(CloudControllerClient client, String serviceName, String serviceKeyName) {
        List<ServiceKey> existingServiceKeys = client.getServiceKeys(serviceName);
        for (ServiceKey existingServiceKey : existingServiceKeys) {
            if (existingServiceKey.getName()
                .equals(serviceKeyName)) {
                return existingServiceKey.getCredentials();
            }
        }
        throw new SLException(Messages.ERROR_RETRIEVING_REQUIRED_SERVICE_KEY_ELEMENT, serviceKeyName, serviceName);
    }
}
