package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;

import org.activiti.engine.delegate.DelegateExecution;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.SLException;

public abstract class AsyncActivitiStep extends SyncActivitiStep {

    private static final Integer DEFAULT_STEP_EXECUTION_INDEX = 0;

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) throws Exception {
        StepPhase stepPhase = StepsUtil.getStepPhase(execution);
        if (stepPhase == StepPhase.WAIT) {
            return StepPhase.WAIT;
        }

        if (stepPhase == StepPhase.POLL) {
            return executeStepExecution(execution);
        }
        execution.getContext()
            .setVariable(Constants.ASYNC_STEP_EXECUTION_INDEX, DEFAULT_STEP_EXECUTION_INDEX);
        return executeAsyncStep(execution);
    }

    private StepPhase executeStepExecution(ExecutionWrapper execution) throws Exception {
        List<AsyncExecution> stepExecutions = getAsyncStepExecutions();

        AsyncExecutionState stepExecutionStatus = getStepExecution(execution, stepExecutions).execute(execution);
        return handleStepExecutionStatus(execution, stepExecutionStatus, stepExecutions);
    }

    private AsyncExecution getStepExecution(ExecutionWrapper execution, List<AsyncExecution> stepOperations) {
        Integer operationIndex = getStepExecutionIndex(execution.getContext());
        return stepOperations.get(operationIndex);
    }

    private Integer getStepExecutionIndex(DelegateExecution context) {
        return (Integer) context.getVariable(Constants.ASYNC_STEP_EXECUTION_INDEX);
    }

    private StepPhase handleStepExecutionStatus(ExecutionWrapper execution, AsyncExecutionState stepExecutionState,
        List<AsyncExecution> stepExecutions) {
        if (stepExecutionState == AsyncExecutionState.FINISHED) {
            StepsUtil.incrementVariable(execution.getContext(), Constants.ASYNC_STEP_EXECUTION_INDEX);
        }

        if (stepExecutionState == AsyncExecutionState.ERROR) {
            throw new SLException("Async execution has failed");
        }

        if (stepExecutionState == AsyncExecutionState.RUNNING) {
            return StepPhase.POLL;
        }
        return determineStepPhase(execution.getContext(), stepExecutions);
    }

    @Override
    protected StepPhase getResultStepPhase() {
        return StepPhase.POLL;
    }

    private StepPhase determineStepPhase(DelegateExecution context, List<AsyncExecution> stepExecutions) {
        Integer stepExecutionIndex = getStepExecutionIndex(context);
        if (stepExecutionIndex >= stepExecutions.size()) {
            return StepPhase.DONE;
        }

        return StepPhase.POLL;
    }

    @Override
    protected StepPhase getInitialStepPhase(ExecutionWrapper executionWrapper) {
        StepPhase currentStepPhase = StepsUtil.getStepPhase(executionWrapper);
        if (currentStepPhase == StepPhase.DONE) {
            return StepPhase.EXECUTE;
        }
        return currentStepPhase;
    }

    protected abstract StepPhase executeAsyncStep(ExecutionWrapper execution) throws Exception;

    protected abstract List<AsyncExecution> getAsyncStepExecutions();

}
