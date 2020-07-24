package com.sap.cloud.lm.sl.cf.process.util;

import java.util.function.Supplier;

import javax.inject.Named;

import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.CloudServiceBrokerException;
import org.springframework.http.HttpStatus;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceInstanceExtended;
import com.sap.cloud.lm.sl.cf.process.Messages;

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