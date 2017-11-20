package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;

import org.junit.Test;

import com.sap.activiti.common.ExecutionStatus;

public abstract class AsyncStepOperationTest<AsyncStep extends SyncActivitiStep> extends SyncActivitiStepTest<AsyncStep> {

    protected abstract List<AsyncStepOperation> getAsyncOperations();

    @Test
    public void testExecuteOperations() throws Exception {
        step.createStepLogger(context);
        ExecutionWrapper wrapper = step.createExecutionWrapper(context);

        for (AsyncStepOperation operation : getAsyncOperations()) {
            ExecutionStatus result = operation.executeOperation(wrapper);
            validateOperationExecutionResult(result);
        }

    }

    protected abstract void validateOperationExecutionResult(ExecutionStatus result);
}
