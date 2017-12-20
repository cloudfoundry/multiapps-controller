package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;

import org.activiti.engine.delegate.DelegateExecution;

import com.sap.cloud.lm.sl.common.SLException;

public abstract class TimeoutAsyncActivitiStep extends AsyncActivitiStep {

    private static final String EXECUTION_OF_STEP_HAS_TIMED_OUT = "Execution of step {0} has timed out";

    @Override
    public StepPhase executeStep(ExecutionWrapper execution) throws Exception {
        boolean hasTimeoutOut = hasTimeout(execution.getContext());
        if (hasTimeoutOut) {
            throw new SLException(MessageFormat.format(EXECUTION_OF_STEP_HAS_TIMED_OUT, getClass().getName()));
        }
        return super.executeStep(execution);
    }

    private boolean hasTimeout(DelegateExecution context) {
        long stepStartTime = getStepStartTime(context);
        long currentTime = System.currentTimeMillis();
        return (currentTime - stepStartTime) >= getTimeout() * 1000;
    }

    private long getStepStartTime(DelegateExecution context) {
        Long stepStartTime = (Long) context.getVariable("stepStartTime");
        if (stepStartTime == null) {
            stepStartTime = System.currentTimeMillis();
            context.setVariable("stepStartTime", stepStartTime);
        }
        return stepStartTime;
    }

    public abstract Integer getTimeout();
}
