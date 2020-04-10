package com.sap.cloud.lm.sl.cf.process.util;

import java.text.MessageFormat;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudException;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.CloudServiceBrokerException;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudServiceBinding;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.cloudfoundry.client.lib.domain.CloudServiceKey;
import org.springframework.http.HttpStatus;

import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.steps.ProcessContext;
import com.sap.cloud.lm.sl.cf.process.util.ExceptionMessageTailMapper.CloudComponents;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.common.SLException;

@Named
public class ServiceRemover {

    private final SecureSerializationFacade secureSerializer = new SecureSerializationFacade();

    private ApplicationConfiguration configuration;

    @Inject
    public ServiceRemover(ApplicationConfiguration configuration) {
        this.configuration = configuration;
    }

    public void deleteService(ProcessContext context, CloudServiceInstance serviceInstance, List<CloudServiceBinding> serviceBindings,
                              List<CloudServiceKey> serviceKeys) {
        CloudControllerClient client = context.getControllerClient();

        try {
            unbindService(client, context.getStepLogger(), serviceInstance, serviceBindings);
            deleteServiceKeys(client, context.getStepLogger(), serviceKeys);
            deleteService(client, context.getStepLogger(), serviceInstance);
        } catch (CloudException e) {
            processException(context, e, serviceInstance);
        }
    }

    private void unbindService(CloudControllerClient client, StepLogger stepLogger, CloudServiceInstance serviceInstance,
                               List<CloudServiceBinding> serviceBindings) {
        if (serviceBindings.isEmpty()) {
            return;
        }
        stepLogger.debug(Messages.SERVICE_BINDINGS_EXISTS, secureSerializer.toJson(serviceBindings));
        for (CloudServiceBinding binding : serviceBindings) {
            CloudApplication application = client.getApplication(binding.getApplicationGuid());

            stepLogger.info(Messages.UNBINDING_SERVICE_FROM_APP, serviceInstance, application.getName());
            client.unbindServiceInstance(application, serviceInstance);
        }
    }

    private void deleteServiceKeys(CloudControllerClient client, StepLogger stepLogger, List<CloudServiceKey> serviceKeys) {
        for (CloudServiceKey serviceKey : serviceKeys) {
            stepLogger.info(Messages.DELETING_SERVICE_KEY_FOR_SERVICE, serviceKey.getName(), serviceKey.getName());
            client.deleteServiceKey(serviceKey);
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
