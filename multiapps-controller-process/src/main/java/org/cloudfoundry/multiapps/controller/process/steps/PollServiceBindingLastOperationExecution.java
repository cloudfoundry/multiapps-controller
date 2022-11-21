package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;

import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.StepLogger;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceBinding;
import com.sap.cloudfoundry.client.facade.domain.ServiceBindingOperation;

public class PollServiceBindingLastOperationExecution implements AsyncExecution {

    @Override
    public AsyncExecutionState execute(ProcessContext context) {
        CloudControllerClient controllerClient = context.getControllerClient();
        CloudApplication app = context.getVariable(Variables.APP_TO_PROCESS);
        String serviceInstanceName = context.getVariable(Variables.SERVICE_TO_UNBIND_BIND);
        CloudServiceBinding serviceBinding = controllerClient.getServiceBindingForApplication(app.getName(), serviceInstanceName);
        return checkServiceBindingOperationState(serviceBinding, context);
    }

    private AsyncExecutionState checkServiceBindingOperationState(CloudServiceBinding serviceBinding, ProcessContext context) {
        ServiceBindingOperation lastOperation = serviceBinding.getServiceBindingOperation();
        StepLogger stepLogger = context.getStepLogger();
        stepLogger.debug(MessageFormat.format(Messages.SERVICE_BINDING_OPERATION_0_IS_IN_STATE_1, serviceBinding.getGuid(),
                                              lastOperation.getState()));
        if (lastOperation.getState() == ServiceBindingOperation.State.FAILED) {
            stepLogger.warnWithoutProgressMessage(MessageFormat.format(Messages.SERVICE_BINDING_0_EXISTS_IN_BROKEN_STATE_WILL_BE_RECREATED,
                                                                       serviceBinding.getGuid()));
            context.setVariable(Variables.IS_SERVICE_BINDING_IN_FAILED_STATE, true);
            return AsyncExecutionState.FINISHED;
        }
        if (lastOperation.getState() == ServiceBindingOperation.State.SUCCEEDED) {
            return AsyncExecutionState.FINISHED;
        }
        return AsyncExecutionState.RUNNING;
    }

    @Override
    public String getPollingErrorMessage(ProcessContext context) {
        CloudApplication app = context.getVariable(Variables.APP_TO_PROCESS);
        String serviceInstanceName = context.getVariable(Variables.SERVICE_TO_UNBIND_BIND);
        return MessageFormat.format(Messages.ERROR_WHILE_POLLING_SERVICE_BINDING_OPERATIONS_BETWEEN_APP_0_AND_SERVICE_INSTANCE_1,
                                    app.getName(), serviceInstanceName);
    }
}
