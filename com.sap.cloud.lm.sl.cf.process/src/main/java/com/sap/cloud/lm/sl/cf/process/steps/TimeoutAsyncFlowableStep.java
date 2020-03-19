package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;

import org.flowable.engine.delegate.DelegateExecution;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.common.SLException;

public abstract class TimeoutAsyncFlowableStep extends AsyncFlowableStep {

    @Override
    public StepPhase executeStep(ProcessContext context) throws Exception {
        boolean hasTimedOut = hasTimedOut(context);
        if (hasTimedOut) {
            throw new SLException(MessageFormat.format(Messages.EXECUTION_OF_STEP_HAS_TIMED_OUT, getStepName()));
        }
        return super.executeStep(context);
    }

    private boolean hasTimedOut(ProcessContext context) {
        long stepStartTime = getStepStartTime(context.getExecution());
        long currentTime = System.currentTimeMillis();
        return (currentTime - stepStartTime) >= getTimeout(context) * 1000;
    }

    private long getStepStartTime(DelegateExecution execution) {
        Long stepStartTime = (Long) execution.getVariable(getStepStartTimeVariable());
        if (stepStartTime == null || isInRetry(execution)) {
            stepStartTime = System.currentTimeMillis();
            execution.setVariable(getStepStartTimeVariable(), stepStartTime);
        }

        return stepStartTime;
    }

    private boolean isInRetry(DelegateExecution execution) {
        return StepsUtil.getStepPhase(execution) == StepPhase.RETRY;
    }

    private String getStepStartTimeVariable() {
        return Constants.VAR_STEP_START_TIME + getStepName();
    }

    private String getStepName() {
        return getClass().getSimpleName();
    }

    public abstract Integer getTimeout(ProcessContext context);
}
