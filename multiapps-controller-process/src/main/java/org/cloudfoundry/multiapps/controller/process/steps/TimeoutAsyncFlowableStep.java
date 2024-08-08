package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.time.Duration;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.helpers.ApplicationAttributes;
import org.cloudfoundry.multiapps.controller.process.Constants;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.TimeoutType;
import org.cloudfoundry.multiapps.controller.process.variables.Variable;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

import static org.cloudfoundry.multiapps.controller.process.util.TimeoutType.getTimeoutCommandLineAndGlobalLevelParameterName;
import static org.cloudfoundry.multiapps.controller.process.util.TimeoutType.getTimeoutModuleLevelParameterName;

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
        long stepStartTime = getStepStartTime(context);
        long currentTime = System.currentTimeMillis();
        return (currentTime - stepStartTime) >= getTimeout(context).toMillis();
    }

    private long getStepStartTime(ProcessContext context) {
        Long stepStartTime = (Long) context.getExecution()
                                           .getVariable(getStepStartTimeVariable());
        if (stepStartTime == null || isInRetry(context)) {
            stepStartTime = System.currentTimeMillis();
            context.getExecution()
                   .setVariable(getStepStartTimeVariable(), stepStartTime);
        }

        return stepStartTime;
    }

    private boolean isInRetry(ProcessContext context) {
        return context.getVariable(Variables.STEP_PHASE) == StepPhase.RETRY;
    }

    private String getStepStartTimeVariable() {
        return Constants.VAR_STEP_START_TIME + getStepName();
    }

    private String getStepName() {
        return getClass().getSimpleName();
    }

    public abstract Duration getTimeout(ProcessContext context);

    protected Duration calculateTimeout(ProcessContext context, TimeoutType timeoutType,
                                        Variable<Duration> timeoutCommandLineLevelParameter,
                                        Variable<Duration> timeoutGlobalLevelParameter) {
        CloudApplicationExtended app = context.getVariable(Variables.APP_TO_PROCESS);
        Integer timeoutModuleLevelValue = extractTimeoutFromAppAttributes(app, getTimeoutModuleLevelParameterName(timeoutType));
        Duration timeoutCommandLineLevelValue = context.getVariable(timeoutCommandLineLevelParameter);

        Duration timeoutFinal;
        String timeoutParameterName;
        if (timeoutCommandLineLevelValue != null) {
            timeoutFinal = timeoutCommandLineLevelValue;
            timeoutParameterName = getTimeoutCommandLineAndGlobalLevelParameterName(timeoutType);
        } else if (timeoutModuleLevelValue != null) {
            timeoutFinal = Duration.ofSeconds(timeoutModuleLevelValue);
            timeoutParameterName = getTimeoutModuleLevelParameterName(timeoutType);

        } else {
            timeoutFinal = Duration.ofSeconds((int) context.getVariable(timeoutGlobalLevelParameter)
                                                           .toSeconds());
            timeoutParameterName = getTimeoutCommandLineAndGlobalLevelParameterName(timeoutType);
        }

        logTimeout(timeoutParameterName, timeoutFinal.toSeconds());
        return timeoutFinal;
    }

    private Integer extractTimeoutFromAppAttributes(CloudApplicationExtended app, String timeoutModuleLevelParameterName) {
        ApplicationAttributes appAttributes = ApplicationAttributes.fromApplication(app, app.getEnv());
        Number taskExecutionTimeout = appAttributes.get(timeoutModuleLevelParameterName, Number.class);
        return taskExecutionTimeout != null ? taskExecutionTimeout.intValue() : null;
    }

    private void logTimeout(String timeoutParameterName, Number timeout) {
        getStepLogger().debug(Messages.TIMEOUT_MESSAGE, timeoutParameterName, timeout);
    }

}
