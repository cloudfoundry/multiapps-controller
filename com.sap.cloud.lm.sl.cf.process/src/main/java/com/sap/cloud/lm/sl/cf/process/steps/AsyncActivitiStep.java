package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;

import org.activiti.engine.delegate.DelegateExecution;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.activiti.common.util.ContextUtil;

public abstract class AsyncActivitiStep extends SyncActivitiStep {

    private static final Integer DEFAULT_OPERATION_INDEX = 0;

    @Override
    protected ExecutionStatus executeStep(ExecutionWrapper execution) throws Exception {
        StepPhase stepPhase = StepsUtil.getStepPhase(execution);
        if (stepPhase == StepPhase.WAIT) {
            return ExecutionStatus.RUNNING;
        }
        if (stepPhase == StepPhase.POLL) {
            return executePollOperation(execution);
        }
        execution.getContext().setVariable("asyncStepOperationIndex", DEFAULT_OPERATION_INDEX);
        return executeAsyncStep(execution);
    }

    private ExecutionStatus executePollOperation(ExecutionWrapper execution) throws Exception {
        List<AsyncStepOperation> stepOperations = getAsyncStepOperations();

        ExecutionStatus operationStatus = getNextOperation(execution, stepOperations).executeOperation(execution);
        return handleOperationStatus(execution, operationStatus);
    }

    private AsyncStepOperation getNextOperation(ExecutionWrapper execution, List<AsyncStepOperation> stepOperations) {
        Integer operationIndex = getOperationIndex(execution.getContext());
        AsyncStepOperation asyncStepOperation = stepOperations.get(operationIndex);
        return asyncStepOperation;
    }

    private Integer getOperationIndex(DelegateExecution context) {
        Object indexObject = context.getVariable("asyncStepOperationIndex");
        if (indexObject == null) {
            context.setVariable("asyncStepOperationIndex", DEFAULT_OPERATION_INDEX);
            return DEFAULT_OPERATION_INDEX;
        }
        return (Integer) indexObject;
    }

    private ExecutionStatus handleOperationStatus(ExecutionWrapper execution, ExecutionStatus operationStatus) {
        if (operationStatus == ExecutionStatus.SUCCESS) {
            ContextUtil.incrementVariable(execution.getContext(), "asyncStepOperationIndex");
        }

        if (operationStatus == ExecutionStatus.FAILED) {
            StepsUtil.setStepPhase(execution, StepPhase.RETRY);
        }
        return operationStatus;
    }

    protected abstract ExecutionStatus executeAsyncStep(ExecutionWrapper execution) throws Exception;

    protected abstract List<AsyncStepOperation> getAsyncStepOperations();

}
