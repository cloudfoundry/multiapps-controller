package org.cloudfoundry.multiapps.controller.process.util;

import java.util.function.Supplier;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.springframework.http.HttpStatus;

import com.sap.cloudfoundry.client.facade.CloudControllerException;
import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.CloudServiceBrokerException;

@Named
public class ServiceOperationExecutor {

    public void executeServiceOperation(CloudServiceInstanceExtended service, Runnable serviceOperation, StepLogger stepLogger) {
        executeServiceOperation(service, () -> {
            serviceOperation.run();
            return null;
        }, stepLogger);
    }

    public <T> T executeServiceOperation(CloudServiceInstanceExtended service, Supplier<T> serviceOperation, StepLogger stepLogger) {
        try {
            return serviceOperation.get();
        } catch (CloudOperationException e) {
            if (!service.isOptional()) {
                if (e.getStatusCode() == HttpStatus.BAD_GATEWAY) {
                    throw new CloudServiceBrokerException(e);
                }
                throw new CloudControllerException(e);
            }
            stepLogger.warn(e, Messages.COULD_NOT_EXECUTE_OPERATION_OVER_OPTIONAL_SERVICE, service.getName());
            return null;
        }
    }

}