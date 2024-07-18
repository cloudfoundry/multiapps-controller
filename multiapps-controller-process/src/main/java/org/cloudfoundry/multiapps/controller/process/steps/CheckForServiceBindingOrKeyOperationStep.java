package org.cloudfoundry.multiapps.controller.process.steps;

import java.time.Duration;
import java.util.List;

import jakarta.inject.Named;

import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@Named("checkForServiceBindingOrKeyOperationStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CheckForServiceBindingOrKeyOperationStep extends TimeoutAsyncFlowableStep {

    @Override
    protected StepPhase executeAsyncStep(ProcessContext context) throws Exception {
        boolean isServiceBindingKeyOperationInProgress = context.getVariable(Variables.IS_SERVICE_BINDING_KEY_OPERATION_IN_PROGRESS);

        if (!isServiceBindingKeyOperationInProgress) {
            context.setVariable(Variables.IS_SERVICE_BINDING_KEY_OPERATION_IN_PROGRESS, Boolean.FALSE);
            return StepPhase.DONE;
        }

        getStepLogger().info(Messages.WAITING_FOR_SERVICE_OPERATION_TO_FINISH);
        return StepPhase.POLL;
    }

    @Override
    protected List<AsyncExecution> getAsyncStepExecutions(ProcessContext context) {
        return List.of(new PollServiceBindingOrKeyOperationExecution());
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_WAITING_FOR_OPERATION_TO_FINISH;
    }

    @Override
    public Duration getTimeout(ProcessContext context) {
        return context.getVariable(Variables.WAIT_BIND_SERVICE_TIMEOUT);
    }
}
