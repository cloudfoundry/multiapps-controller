package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.facade.CloudControllerClient;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudApplication;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceBinding;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ServiceCredentialBindingOperation;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.StepLogger;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

public class PollServiceBindingsLastOperationExecution implements AsyncExecution {

    @Override
    public AsyncExecutionState execute(ProcessContext context) {
        CloudControllerClient controllerClient = context.getControllerClient();
        List<CloudServiceBinding> serviceBindings = getServiceBindingsForProcessing(context, controllerClient);
        if (serviceBindings.isEmpty()) {
            context.getStepLogger()
                   .debug(Messages.SERVICE_BINDING_HAS_ALREADY_BEEN_DELETED);
            return AsyncExecutionState.FINISHED;
        }
        return checkServiceBindingOperationState(serviceBindings, context);
    }

    private List<CloudServiceBinding> getServiceBindingsForProcessing(ProcessContext context, CloudControllerClient controllerClient) {
        CloudServiceBinding serviceBindingToDelete = context.getVariable(Variables.SERVICE_BINDING_TO_DELETE);
        if (serviceBindingToDelete != null) {
            return getServiceBinding(context, controllerClient, serviceBindingToDelete);
        }
        return getServiceBindingsForAppAndServiceInstance(context, controllerClient);
    }

    private List<CloudServiceBinding> getServiceBinding(ProcessContext context, CloudControllerClient controllerClient,
                                                        CloudServiceBinding serviceBindingToDelete) {
        CloudServiceBinding serviceBinding = controllerClient.getServiceBinding(serviceBindingToDelete.getGuid());
        if (serviceBinding == null) {
            return Collections.emptyList();
        }
        context.getStepLogger()
               .debug(Messages.SERVICE_BINDING_0_SCHEDULED_FOR_DELETION_IS_IN_STATE_0, serviceBinding.getGuid(),
                      serviceBinding.getServiceBindingOperation()
                                    .getState());
        return List.of(serviceBinding);
    }

    private List<CloudServiceBinding> getServiceBindingsForAppAndServiceInstance(ProcessContext context,
                                                                                 CloudControllerClient controllerClient) {
        CloudApplication app = context.getVariable(Variables.APP_TO_PROCESS);
        String serviceInstanceName = context.getVariable(Variables.SERVICE_TO_UNBIND_BIND);
        UUID applicationGuid = controllerClient.getApplicationGuid(app.getName());
        UUID serviceInstanceGuid = controllerClient.getRequiredServiceInstanceGuid(serviceInstanceName);
        return controllerClient.getServiceBindingsForApplication(applicationGuid, serviceInstanceGuid);
    }

    protected AsyncExecutionState checkServiceBindingOperationState(List<CloudServiceBinding> serviceBindings, ProcessContext context) {
        List<CloudServiceBinding> failedServiceBindings = new ArrayList<>();
        boolean hasInProgressBindings = false;
        StepLogger stepLogger = context.getStepLogger();

        for (CloudServiceBinding serviceBinding : serviceBindings) {
            ServiceCredentialBindingOperation lastOperation = serviceBinding.getServiceBindingOperation();
            stepLogger.debug(MessageFormat.format(Messages.SERVICE_BINDING_OPERATION_WITH_TYPE_IS_IN_STATE, serviceBinding.getGuid(),
                                                  lastOperation.getType(), lastOperation.getState()));
            if (lastOperation.getState() == ServiceCredentialBindingOperation.State.FAILED) {
                failedServiceBindings.add(serviceBinding);
            }
            if (lastOperation.getState() == ServiceCredentialBindingOperation.State.IN_PROGRESS
                || lastOperation.getState() == ServiceCredentialBindingOperation.State.INITIAL) {
                hasInProgressBindings = true;
            }
        }

        if (!failedServiceBindings.isEmpty()) {
            return completePollingOfFailedOperation(failedServiceBindings, context);
        }
        if (hasInProgressBindings) {
            return AsyncExecutionState.RUNNING;
        }
        return AsyncExecutionState.FINISHED;
    }

    protected AsyncExecutionState completePollingOfFailedOperation(List<CloudServiceBinding> failedServiceBindings,
                                                                   ProcessContext context) {
        List<CloudServiceInstanceExtended> servicesToBind = context.getVariable(Variables.SERVICES_TO_BIND);
        CloudApplication app = context.getVariable(Variables.APP_TO_PROCESS);
        String serviceInstanceName = context.getVariable(Variables.SERVICE_TO_UNBIND_BIND);
        if (StepsUtil.isServiceOptional(servicesToBind, serviceInstanceName)) {
            context.getStepLogger()
                   .warn(Messages.ERROR_WHILE_POLLING_SERVICE_BINDING_OPERATION_BETWEEN_APP_AND_OPTIONAL_SERVICE, serviceInstanceName,
                         app.getName());
            return AsyncExecutionState.FINISHED;
        }
        if (failedServiceBindings.size() == 1) {
            CloudServiceBinding serviceBinding = failedServiceBindings.get(0);
            context.getStepLogger()
                   .error(Messages.ERROR_WHILE_POLLING_SERVICE_BINDING_OPERATION_BETWEEN_APP_AND_SERVICE, app.getName(),
                          serviceInstanceName,
                          serviceBinding.getServiceBindingOperation()
                                        .getDescription());
            return AsyncExecutionState.ERROR;
        }
        context.getStepLogger()
               .error(Messages.ERROR_WHILE_POLLING_SERVICE_BINDING_OPERATION_BETWEEN_APP_AND_SERVICE_CHECK_OPERATION_LOGS, app.getName(),
                      serviceInstanceName);
        for (CloudServiceBinding serviceBinding : failedServiceBindings) {
            context.getStepLogger()
                   .errorWithoutProgressMessage(Messages.ERROR_WHILE_POLLING_SERVICE_BINDING_GUID,
                                                serviceBinding.getGuid(),
                                                serviceBinding.getServiceBindingOperation()
                                                              .getDescription());
        }
        return AsyncExecutionState.ERROR;
    }

    @Override
    public String getPollingErrorMessage(ProcessContext context) {
        CloudApplication app = context.getVariable(Variables.APP_TO_PROCESS);
        String serviceInstanceName = context.getVariable(Variables.SERVICE_TO_UNBIND_BIND);
        return MessageFormat.format(Messages.ERROR_WHILE_POLLING_SERVICE_BINDING_OPERATIONS_BETWEEN_APP_0_AND_SERVICE_INSTANCE_1,
                                    app.getName(), serviceInstanceName);
    }
}
