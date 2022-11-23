package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceKey;
import com.sap.cloudfoundry.client.facade.domain.ServiceCredentialBindingOperation;

public class PollServiceKeyLastOperationExecution implements AsyncExecution {

    @Override
    public AsyncExecutionState execute(ProcessContext context) {
        CloudControllerClient controllerClient = context.getControllerClient();
        CloudServiceKey serviceKeyToProcess = context.getVariable(Variables.SERVICE_KEY_TO_PROCESS);
        CloudServiceKey serviceKey = controllerClient.getServiceKey(serviceKeyToProcess.getServiceInstance()
                                                                                       .getName(),
                                                                    serviceKeyToProcess.getName());
        return checkServiceKeyLastOperation(serviceKey, context);
    }

    protected AsyncExecutionState checkServiceKeyLastOperation(CloudServiceKey serviceKey, ProcessContext context) {
        context.getStepLogger()
               .debug(Messages.POLLING_SERVICE_KEY_0_WITH_STATE_1, serviceKey.getName(), serviceKey.getServiceKeyOperation()
                                                                                                   .getState());
        var lastOperation = serviceKey.getServiceKeyOperation();
        if (lastOperation.getState() == ServiceCredentialBindingOperation.State.IN_PROGRESS
            || lastOperation.getState() == ServiceCredentialBindingOperation.State.INITIAL) {
            return AsyncExecutionState.RUNNING;
        }
        if (lastOperation.getState() == ServiceCredentialBindingOperation.State.FAILED) {
            return doOnError(serviceKey, context);
        }
        return AsyncExecutionState.FINISHED;
    }

    protected AsyncExecutionState doOnError(CloudServiceKey serviceKey, ProcessContext context) {
        CloudServiceInstanceExtended serviceInstanceToProcess = context.getVariable(Variables.SERVICE_TO_PROCESS);
        if (serviceInstanceToProcess.isOptional()) {
            context.getStepLogger()
                   .warn(Messages.OPERATION_FOR_OPTIONAL_SERVICE_KEY_0_FAILED_WITH_DESCRIPTION_1, serviceKey.getName(),
                         serviceKey.getServiceKeyOperation()
                                   .getDescription());
            return AsyncExecutionState.FINISHED;
        }
        context.getStepLogger()
               .error(Messages.OPERATION_FOR_SERVICE_KEY_0_FAILED_WITH_DESCRIPTION_1, serviceKey.getName(),
                      serviceKey.getServiceKeyOperation()
                                .getDescription());
        return AsyncExecutionState.ERROR;
    }

    @Override
    public String getPollingErrorMessage(ProcessContext context) {
        CloudServiceKey serviceKeyToDelete = context.getVariable(Variables.SERVICE_KEY_TO_PROCESS);
        return MessageFormat.format(Messages.ERROR_WHILE_POLLING_SERVICE_KEY_OPERATION_0, serviceKeyToDelete.getName());
    }
}
