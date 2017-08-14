package com.sap.cloud.lm.sl.cf.process.util;

import java.text.MessageFormat;
import java.util.function.Supplier;

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.process.message.Messages;

public class ServiceOperationExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceOperationExecutor.class);

    public void executeServiceOperation(CloudServiceExtended service, Runnable serviceOperation) {
        executeServiceOperation(service, () -> {
            serviceOperation.run();
            return null;
        });
    }

    public <T> T executeServiceOperation(CloudServiceExtended service, Supplier<T> serviceOperationSupplier) {
        try {
            return serviceOperationSupplier.get();
        } catch (CloudFoundryException e) {
            if (!service.isOptional()) {
                throw e;
            }
            LOGGER.warn(MessageFormat.format(Messages.COULD_NOT_EXECUTE_OPERATION_OVER_OPTIONAL_SERVICE, service.getName(), e));
            return null;
        }
    }
}