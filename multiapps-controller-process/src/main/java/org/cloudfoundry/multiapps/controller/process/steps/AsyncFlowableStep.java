package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.List;

import org.cloudfoundry.multiapps.controller.process.variables.Variables;

public abstract class AsyncFlowableStep extends SyncFlowableStep {

    private static final Integer DEFAULT_STEP_EXECUTION_INDEX = 0;

    @Override
    protected StepPhase executeStep(ProcessContext context) throws Exception {
        StepPhase stepPhase = context.getVariable(Variables.STEP_PHASE);
        if (stepPhase == StepPhase.POLL) {
            return executeStepExecution(context);
        }
        context.setVariable(Variables.ASYNC_STEP_EXECUTION_INDEX, DEFAULT_STEP_EXECUTION_INDEX);
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

    private AsyncExecution getStepExecution(ProcessContext context, List<AsyncExecution> stepExecutions) {
        Integer executionIndex = getStepExecutionIndex(context);
        return stepExecutions.get(executionIndex);
    }

    private Integer getStepExecutionIndex(ProcessContext context) {
        return context.getVariable(Variables.ASYNC_STEP_EXECUTION_INDEX);
    }

    private StepPhase handleStepExecutionStatus(ProcessContext context, AsyncExecutionState stepExecutionState,
                                                List<AsyncExecution> stepExecutions) {
        if (stepExecutionState == AsyncExecutionState.FINISHED) {
            StepsUtil.incrementVariable(context.getExecution(), Variables.ASYNC_STEP_EXECUTION_INDEX.getName());
        }

        if (stepExecutionState == AsyncExecutionState.ERROR) {
            return StepPhase.RETRY;
        }

        if (stepExecutionState == AsyncExecutionState.RUNNING) {
            return StepPhase.POLL;
        }
        return determineStepPhase(context, stepExecutions);
    }

    private StepPhase determineStepPhase(ProcessContext context, List<AsyncExecution> stepExecutions) {
        Integer stepExecutionIndex = getStepExecutionIndex(context);
        if (stepExecutionIndex >= stepExecutions.size()) {
            return StepPhase.DONE;
        }

        return StepPhase.POLL;
    }

    @Override
    protected StepPhase getInitialStepPhase(ProcessContext context) {
        StepPhase currentStepPhase = context.getVariable(Variables.STEP_PHASE);
        if (currentStepPhase == StepPhase.DONE || currentStepPhase == StepPhase.RETRY) {
            return StepPhase.EXECUTE;
        }
        return currentStepPhase;
    }

    protected abstract StepPhase executeAsyncStep(ProcessContext context) throws Exception;

    protected abstract List<AsyncExecution> getAsyncStepExecutions(ProcessContext context);

}
