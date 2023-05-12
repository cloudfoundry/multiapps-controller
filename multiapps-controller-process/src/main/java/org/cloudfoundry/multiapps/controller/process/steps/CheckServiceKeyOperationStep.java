package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.List;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceKey;
import com.sap.cloudfoundry.client.facade.domain.ServiceCredentialBindingOperation;

@Named("checkServiceKeyOperationStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CheckServiceKeyOperationStep extends AsyncFlowableStep {

    @Override
    protected StepPhase executeAsyncStep(ProcessContext context) throws Exception {
        CloudControllerClient controllerClient = context.getControllerClient();
        CloudServiceKey serviceKeyToProcess = context.getVariable(Variables.SERVICE_KEY_TO_PROCESS);
        CloudServiceKey serviceKey = controllerClient.getServiceKey(serviceKeyToProcess.getServiceInstance()
                                                                                       .getName(),
                                                                    serviceKeyToProcess.getName());
        if (serviceKey == null) {
            getStepLogger().debug(Messages.SERVICE_KEY_DOES_NOT_EXIST_0, serviceKeyToProcess.getName());
            context.setVariable(Variables.SERVICE_KEY_DOES_NOT_EXIST, true);
            return StepPhase.DONE;
        }
        return checkServiceKeyLastOperation(serviceKey);
    }

    private StepPhase checkServiceKeyLastOperation(CloudServiceKey serviceKey) {
        var lastOperation = serviceKey.getServiceKeyOperation();
        getStepLogger().debug(Messages.SERVICE_KEY_0_EXISTS_IN_STATE_1, serviceKey.getName(), serviceKey.getServiceKeyOperation()
                                                                                                        .getState());
        if (lastOperation.getState() == ServiceCredentialBindingOperation.State.IN_PROGRESS
            || lastOperation.getState() == ServiceCredentialBindingOperation.State.INITIAL) {
            return StepPhase.POLL;
        }
        return StepPhase.DONE;
    }

    @Override
    protected List<AsyncExecution> getAsyncStepExecutions(ProcessContext context) {
        return List.of(new PollServiceKeyLastOperationFailSafeExecution());
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        CloudServiceKey serviceKeyToProcess = context.getVariable(Variables.SERVICE_KEY_TO_PROCESS);
        return MessageFormat.format(Messages.ERROR_WHILE_CHECKING_SERVICE_KEY_OPERATION_0, serviceKeyToProcess.getName());
    }
}
