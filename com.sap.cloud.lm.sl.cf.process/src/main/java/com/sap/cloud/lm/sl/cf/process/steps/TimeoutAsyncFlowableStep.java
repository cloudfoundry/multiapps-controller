package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;

import org.flowable.engine.delegate.DelegateExecution;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;

public abstract class TimeoutAsyncFlowableStep extends AsyncFlowableStep {

    @Override
    public StepPhase executeStep(ExecutionWrapper execution) throws Exception {
        boolean hasTimedOut = hasTimedOut(execution.getContext());
        if (hasTimedOut) {
            throw new SLException(MessageFormat.format(Messages.EXECUTION_OF_STEP_HAS_TIMED_OUT, getStepName()));
        }
        return super.executeStep(execution);
    }

    private boolean hasTimedOut(DelegateExecution context) {
        long stepStartTime = getStepStartTime(context);
        long currentTime = System.currentTimeMillis();
        return (currentTime - stepStartTime) >= getTimeout(context) * 1000;
    }

    private long getStepStartTime(DelegateExecution context) {
        Long stepStartTime = (Long) context.getVariable(getStepStartTimeVariable());
        if (stepStartTime == null) {
            stepStartTime = System.currentTimeMillis();
            context.setVariable(getStepStartTimeVariable(), stepStartTime);
        }
        return stepStartTime;
    }

    private String getStepStartTimeVariable() {
        return Constants.VAR_STEP_START_TIME + getStepName();
    }

    private String getStepName() {
        return getClass().getSimpleName();
    }

    public abstract Integer getTimeout(DelegateExecution context);
}
