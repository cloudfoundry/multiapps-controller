package org.cloudfoundry.multiapps.controller.process.steps;

import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

class CheckForServiceBindingOrKeyOperationStepTest extends SyncFlowableStepTest<CheckForServiceBindingOrKeyOperationStep> {

    @Test
    void testStepFinishedWithDoneIfVarIsFalse() {
        context.setVariable(Variables.IS_SERVICE_BINDING_KEY_OPERATION_IN_PROGRESS, false);
        step.execute(execution);
        assertStepFinishedSuccessfully();
    }

    @Test
    void testStepFinishedWithDoneIfVarIsTrue() {
        context.setVariable(Variables.IS_SERVICE_BINDING_KEY_OPERATION_IN_PROGRESS, true);
        step.execute(execution);
        assertExecutionStepStatus(StepPhase.POLL.toString());
    }

    @Test
    void testGetErrorMessage() {
        step.execute(execution);
        Assertions.assertEquals(Messages.ERROR_WAITING_FOR_OPERATION_TO_FINISH, step.getStepErrorMessage(context));
    }

    @Test
    void testGetTimeoutCustomValue() {
        var timeout = Duration.ofSeconds(10);
        context.setVariable(Variables.WAIT_BIND_SERVICE_TIMEOUT, timeout);
        Assertions.assertEquals(timeout, step.getTimeout(context));
    }

    @Test
    void testAsyncExecutionStatus() {
        List<AsyncExecution> asyncStepExecutions = step.getAsyncStepExecutions(context);
        Assertions.assertEquals(1, asyncStepExecutions.size());
        Assertions.assertTrue(asyncStepExecutions.get(0) instanceof PollServiceBindingOrKeyOperationExecution);
    }

    @Override
    protected CheckForServiceBindingOrKeyOperationStep createStep() {
        return new CheckForServiceBindingOrKeyOperationStep();
    }
}
