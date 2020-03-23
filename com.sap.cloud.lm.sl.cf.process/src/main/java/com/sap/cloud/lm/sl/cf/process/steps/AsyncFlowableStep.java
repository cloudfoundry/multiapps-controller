package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;

import org.flowable.engine.delegate.DelegateExecution;

import com.sap.cloud.lm.sl.cf.process.Constants;

public abstract class AsyncFlowableStep extends SyncFlowableStep {

    private static final Integer DEFAULT_STEP_EXECUTION_INDEX = 0;

    @Override
    protected StepPhase executeStep(ProcessContext context) throws Exception {
        StepPhase stepPhase = StepsUtil.getStepPhase(context.getExecution());
        if (stepPhase == StepPhase.POLL) {
            return executeStepExecution(context);
        }
        context.getExecution()
               .setVariable(Constants.ASYNC_STEP_EXECUTION_INDEX, DEFAULT_STEP_EXECUTION_INDEX);
        return executeAsyncStep(context);
    }

    private StepPhase executeStepExecution(ProcessContext context) throws Exception {
        List<AsyncExecution> stepExecutions = getAsyncStepExecutions(context);
        AsyncExecution stepExecution = getStepExecution(context, stepExecutions);
        try {
            AsyncExecutionState stepExecutionStatus = stepExecution.execute(context);
            return handleStepExecutionStatus(context, stepExecutionStatus, stepExecutions);
        } catch (Exception e) {
            processException(e, stepExecution.getPollingErrorMessage(context), getStepErrorMessageAdditionalDescription(context));
        }
        return StepPhase.RETRY;
    }

    private AsyncExecution getStepExecution(ProcessContext context, List<AsyncExecution> stepOperations) {
        Integer operationIndex = getStepExecutionIndex(context.getExecution());
        return stepOperations.get(operationIndex);
    }

    private Integer getStepExecutionIndex(DelegateExecution execution) {
        return (Integer) execution.getVariable(Constants.ASYNC_STEP_EXECUTION_INDEX);
    }

    private StepPhase handleStepExecutionStatus(ProcessContext context, AsyncExecutionState stepExecutionState,
                                                List<AsyncExecution> stepExecutions) {
        if (stepExecutionState == AsyncExecutionState.FINISHED) {
            StepsUtil.incrementVariable(context.getExecution(), Constants.ASYNC_STEP_EXECUTION_INDEX);
        }

        if (stepExecutionState == AsyncExecutionState.ERROR) {
            return StepPhase.RETRY;
        }

        if (stepExecutionState == AsyncExecutionState.RUNNING) {
            return StepPhase.POLL;
        }
        return determineStepPhase(context.getExecution(), stepExecutions);
    }

    private StepPhase determineStepPhase(DelegateExecution execution, List<AsyncExecution> stepExecutions) {
        Integer stepExecutionIndex = getStepExecutionIndex(execution);
        if (stepExecutionIndex >= stepExecutions.size()) {
            return StepPhase.DONE;
        }

        return StepPhase.POLL;
    }

    @Override
    protected StepPhase getInitialStepPhase(ProcessContext context) {
        StepPhase currentStepPhase = StepsUtil.getStepPhase(context.getExecution());
        if (currentStepPhase == StepPhase.DONE || currentStepPhase == StepPhase.RETRY) {
            return StepPhase.EXECUTE;
        }
        return currentStepPhase;
    }

    protected abstract StepPhase executeAsyncStep(ProcessContext context) throws Exception;

    protected abstract List<AsyncExecution> getAsyncStepExecutions(ProcessContext context);

}
