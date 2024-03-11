package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;

import org.junit.Test;

public abstract class AsyncStepOperationTest<AsyncStep extends SyncFlowableStep> extends SyncFlowableStepTest<AsyncStep> {

    protected abstract List<AsyncExecution> getAsyncOperations(ExecutionWrapper wrapper);

    @Test
    public void testExecuteOperations() throws Exception {
        step.initializeStepLogger(context);
        ExecutionWrapper wrapper = step.createExecutionWrapper(context);

        for (AsyncExecution operation : getAsyncOperations(wrapper)) {
            AsyncExecutionState result = operation.execute(wrapper);
            validateOperationExecutionResult(result);
        }

    }

    protected abstract void validateOperationExecutionResult(AsyncExecutionState result);
}
