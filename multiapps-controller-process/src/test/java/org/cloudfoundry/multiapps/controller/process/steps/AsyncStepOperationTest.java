package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.List;

public abstract class AsyncStepOperationTest<AsyncStep extends SyncFlowableStep> extends SyncFlowableStepTest<AsyncStep> {

    public void testExecuteOperations() {
        step.initializeStepLogger(execution);
        ProcessContext wrapper = step.createProcessContext(execution);

        for (AsyncExecution operation : getAsyncOperations(wrapper)) {
            AsyncExecutionState result = operation.execute(wrapper);
            validateOperationExecutionResult(result);
        }

    }

    protected abstract List<AsyncExecution> getAsyncOperations(ProcessContext wrapper);

    protected abstract void validateOperationExecutionResult(AsyncExecutionState result);
}
