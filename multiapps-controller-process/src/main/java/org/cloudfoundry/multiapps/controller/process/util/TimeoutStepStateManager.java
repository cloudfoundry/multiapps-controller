package org.cloudfoundry.multiapps.controller.process.util;

import java.time.Duration;

import org.cloudfoundry.multiapps.controller.process.Constants;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.steps.StepPhase;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

public class TimeoutStepStateManager {

    public boolean hasTimedOut(ProcessContext context, String stepName, Duration timeout) {
        long stepStartTime = getStepStartTime(context, stepName);
        long currentTime = System.currentTimeMillis();
        return (currentTime - stepStartTime) >= timeout.toMillis();
    }

    public long getStepStartTime(ProcessContext context, String stepName) {
        Long stepStartTime = (Long) context.getExecution()
                                           .getVariable(getStepStartTimeVariable(stepName));
        if (shouldResetTimeout(stepStartTime, context)) {
            stepStartTime = System.currentTimeMillis();
            context.getExecution()
                   .setVariable(getStepStartTimeVariable(stepName), stepStartTime);
            clearResetFlag(context);
        }
        return stepStartTime;
    }

    private boolean shouldResetTimeout(Long stepStartTime, ProcessContext context) {
        return isTimeoutNotInitialized(stepStartTime) || isInRetry(context) || mustResetTimeout(context);
    }

    private boolean isTimeoutNotInitialized(Long stepStartTime) {
        return stepStartTime == null;
    }

    private boolean isInRetry(ProcessContext context) {
        return context.getVariable(Variables.STEP_PHASE) == StepPhase.RETRY;
    }

    private boolean mustResetTimeout(ProcessContext context) {
        return context.getVariable(Variables.MUST_RESET_TIMEOUT);
    }

    private void clearResetFlag(ProcessContext context) {
        if (mustResetTimeout(context)) {
            context.setVariable(Variables.MUST_RESET_TIMEOUT, false);
        }
    }

    private String getStepStartTimeVariable(String stepName) {
        return Constants.VAR_STEP_START_TIME + stepName;
    }

}

