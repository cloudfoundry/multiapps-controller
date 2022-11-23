package org.cloudfoundry.multiapps.controller.process.util;

import java.util.List;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.springframework.http.HttpStatus;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.CloudControllerException;
import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.CloudServiceBrokerException;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceKey;

public final class ServiceUtil {

    private ServiceUtil() {

    }

    public static List<CloudServiceKey> getExistingServiceKeys(CloudControllerClient client, CloudServiceInstanceExtended service,
                                                               StepLogger stepLogger) {
        try {
            return client.getServiceKeysWithCredentials(service.getName());
        } catch (CloudOperationException e) {
            if (!service.isOptional()) {
                if (e.getStatusCode() == HttpStatus.BAD_GATEWAY) {
                    throw new CloudServiceBrokerException(e);
                }
                throw new CloudControllerException(e);
            }
            stepLogger.warn(e, Messages.COULD_NOT_GET_SERVICE_KEYS_FOR_OPTIONAL_SERVICE, service.getName());
            return null;
        }
    }

}
