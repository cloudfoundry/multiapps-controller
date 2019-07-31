package com.sap.cloud.lm.sl.cf.process.util;

import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudServiceKey;

import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;

public class ServiceOperationUtil {

    private ServiceOperationUtil() {
    }

    public static Map<String, Object> getServiceKeyCredentials(CloudControllerClient client, String serviceName, String serviceKeyName) {
        CloudServiceKey foundServiceKey = getServiceKey(client, serviceName, serviceKeyName);
        return foundServiceKey.getCredentials();
    }

    public static CloudServiceKey getServiceKey(CloudControllerClient client, String serviceName, String serviceKeyName) {
        List<CloudServiceKey> existingServiceKeys = client.getServiceKeys(serviceName);
        return existingServiceKeys.stream()
                                  .filter(existingServiceKey -> existingServiceKey.getName()
                                                                                  .equals(serviceKeyName))
                                  .findFirst()
                                  .orElseThrow(() -> new SLException(Messages.ERROR_RETRIEVING_REQUIRED_SERVICE_KEY_ELEMENT,
                                                                     serviceKeyName,
                                                                     serviceName));
    }

}
