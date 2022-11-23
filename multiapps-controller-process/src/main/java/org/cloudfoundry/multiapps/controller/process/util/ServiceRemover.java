package org.cloudfoundry.multiapps.controller.process.util;

import java.text.MessageFormat;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.util.ExceptionMessageTailMapper.CloudComponents;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.http.HttpStatus;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.CloudControllerException;
import com.sap.cloudfoundry.client.facade.CloudException;
import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.CloudServiceBrokerException;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceInstance;

@Named
public class ServiceRemover {

    private final ApplicationConfiguration configuration;

    @Inject
    public ServiceRemover(ApplicationConfiguration configuration) {
        this.configuration = configuration;
    }

    public void deleteService(ProcessContext context, CloudServiceInstance serviceInstance) {
        CloudControllerClient client = context.getControllerClient();
        StepLogger stepLogger = context.getStepLogger();
        try {
            deleteService(client, stepLogger, serviceInstance);
        } catch (CloudException e) {
            processException(context, e, serviceInstance);
        }
    }

    private void deleteService(CloudControllerClient client, StepLogger stepLogger, CloudServiceInstance serviceInstance) {
        stepLogger.info(Messages.DELETING_SERVICE, serviceInstance.getName());
        client.deleteServiceInstance(serviceInstance);
        stepLogger.debug(Messages.SERVICE_DELETED, serviceInstance.getName());
    }

    private void processException(ProcessContext context, Exception e, CloudServiceInstance serviceInstance) {
        if (e instanceof CloudOperationException) {
            e = evaluateCloudOperationException(context, (CloudOperationException) e, serviceInstance.getName(),
                                                serviceInstance.getLabel());
            if (e == null) {
                return;
            }
        }
        wrapAndThrowException(e, serviceInstance);
    }

    private CloudOperationException evaluateCloudOperationException(ProcessContext context, CloudOperationException e, String serviceName,
                                                                    String label) {
        if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
            context.getStepLogger()
                   .warn(MessageFormat.format(Messages.COULD_NOT_DELETE_SERVICE, serviceName), e,
                         ExceptionMessageTailMapper.map(configuration, CloudComponents.SERVICE_BROKERS, label));
            return null;
        }
        if (e.getStatusCode() == HttpStatus.BAD_GATEWAY) {
            context.setVariable(Variables.SERVICE_OFFERING, label);
            return new CloudServiceBrokerException(e);
        }
        return new CloudControllerException(e);

    }

    private void wrapAndThrowException(Exception e, CloudServiceInstance serviceInstance) {
        String msg = buildErrorDeletingServiceExceptionMessage(e, serviceInstance);
        throw new SLException(e, msg);
    }

    private String buildErrorDeletingServiceExceptionMessage(Exception e, CloudServiceInstance serviceInstance) {
        return MessageFormat.format(Messages.ERROR_DELETING_SERVICE, serviceInstance.getName(), serviceInstance.getLabel(),
                                    serviceInstance.getPlan(), e.getMessage());
    }

}
