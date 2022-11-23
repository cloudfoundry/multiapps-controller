package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.List;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.StepLogger;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceBinding;
import com.sap.cloudfoundry.client.facade.domain.ServiceCredentialBindingOperation;

public class PollServiceBindingLastOperationExecution implements AsyncExecution {

    @Override
    public AsyncExecutionState execute(ProcessContext context) {
        CloudControllerClient controllerClient = context.getControllerClient();
        CloudServiceBinding serviceBinding = getServiceBindingForProcessing(context, controllerClient);
        if (serviceBinding == null) {
            return AsyncExecutionState.FINISHED;
        }
        return checkServiceBindingOperationState(serviceBinding, context);
    }

    private CloudServiceBinding getServiceBindingForProcessing(ProcessContext context, CloudControllerClient controllerClient) {
        CloudServiceBinding serviceBindingToDelete = context.getVariable(Variables.SERVICE_BINDING_TO_DELETE);
        if (serviceBindingToDelete != null) {
            return getServiceBinding(context, controllerClient, serviceBindingToDelete);
        }
        return getServiceBindingForAppAndServiceInstance(context, controllerClient);
    }

    private CloudServiceBinding getServiceBinding(ProcessContext context, CloudControllerClient controllerClient,
                                                  CloudServiceBinding serviceBindingToDelete) {
        CloudServiceBinding serviceBinding = controllerClient.getServiceBindingForApplication(serviceBindingToDelete.getApplicationGuid(),
                                                                                              serviceBindingToDelete.getServiceInstanceGuid());
        if (serviceBinding == null) {
            context.getStepLogger()
                   .debug(Messages.SERVICE_BINDING_HAS_ALREADY_BEEN_DELETED);
            return null;
        }
        context.getStepLogger()
               .debug(Messages.SERVICE_BINDING_0_SCHEDULED_FOR_DELETION_IS_IN_STATE_0, serviceBinding.getGuid(),
                      serviceBinding.getServiceBindingOperation()
                                    .getState());
        return serviceBinding;
    }

    private CloudServiceBinding getServiceBindingForAppAndServiceInstance(ProcessContext context, CloudControllerClient controllerClient) {
        CloudApplication app = context.getVariable(Variables.APP_TO_PROCESS);
        String serviceInstanceName = context.getVariable(Variables.SERVICE_TO_UNBIND_BIND);
        UUID applicationGuid = controllerClient.getApplicationGuid(app.getName());
        UUID serviceInstanceGuid = controllerClient.getRequiredServiceInstanceGuid(serviceInstanceName);
        return controllerClient.getServiceBindingForApplication(applicationGuid, serviceInstanceGuid);
    }

    protected AsyncExecutionState checkServiceBindingOperationState(CloudServiceBinding serviceBinding, ProcessContext context) {
        ServiceCredentialBindingOperation lastOperation = serviceBinding.getServiceBindingOperation();
        StepLogger stepLogger = context.getStepLogger();
        stepLogger.debug(MessageFormat.format(Messages.SERVICE_BINDING_OPERATION_WITH_TYPE_IS_IN_STATE, serviceBinding.getGuid(),
                                              lastOperation.getType(), lastOperation.getState()));
        if (lastOperation.getState() == ServiceCredentialBindingOperation.State.FAILED) {
            return completePollingOfFailedOperation(serviceBinding, context);
        }
        if (lastOperation.getState() == ServiceCredentialBindingOperation.State.SUCCEEDED) {
            return AsyncExecutionState.FINISHED;
        }
        return AsyncExecutionState.RUNNING;
    }

    protected AsyncExecutionState completePollingOfFailedOperation(CloudServiceBinding serviceBinding, ProcessContext context) {
        List<CloudServiceInstanceExtended> servicesToBind = context.getVariable(Variables.SERVICES_TO_BIND);
        CloudApplication app = context.getVariable(Variables.APP_TO_PROCESS);
        String serviceInstanceName = context.getVariable(Variables.SERVICE_TO_UNBIND_BIND);
        if (StepsUtil.isServiceOptional(servicesToBind, serviceInstanceName)) {
            context.getStepLogger()
                   .warn(Messages.ERROR_WHILE_POLLING_SERVICE_BINDING_OPERATION_BETWEEN_APP_AND_OPTIONAL_SERVICE, serviceInstanceName,
                         app.getName());
            return AsyncExecutionState.FINISHED;
        }
        context.getStepLogger()
               .error(Messages.ERROR_WHILE_POLLING_SERVICE_BINDING_OPERATION_BETWEEN_APP_AND_SERVICE, app.getName(), serviceInstanceName,
                      serviceBinding.getServiceBindingOperation()
                                    .getDescription());
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
