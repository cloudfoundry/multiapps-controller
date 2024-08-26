package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.time.Duration;

import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.helpers.ApplicationAttributes;
import org.cloudfoundry.multiapps.controller.process.Constants;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.TimeoutType;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;

public abstract class TimeoutAsyncFlowableStep extends AsyncFlowableStep {
    private static final String DEFAULT_TIMEOUT = "default";

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

    protected Duration calculateTimeout(ProcessContext context, TimeoutType timeoutType) {
        Duration timeoutFinal = context.getVariableIfSet(timeoutType.getProcessVariable());
        String timeoutParameterName = timeoutType.getProcessVariableAndGlobalLevelParamName();

        if (timeoutFinal == null) {
            timeoutFinal = extractTimeoutFromAppAttributes(context, timeoutType);
            if (timeoutFinal != null) {
                timeoutParameterName = timeoutType.getModuleLevelParamName();
            }
        }

        if (timeoutFinal == null) {
            timeoutFinal = extractTimeoutFromDescriptorParameters(context, timeoutType);
        }

        if (timeoutFinal == null) {
            timeoutFinal = Duration.ofSeconds(timeoutType.getProcessVariable()
                                                         .getDefaultValue()
                                                         .getSeconds());
            timeoutParameterName = DEFAULT_TIMEOUT;
        }

        logTimeout(timeoutType.getModuleLevelParamName(), timeoutParameterName, timeoutFinal.toSeconds());
        return timeoutFinal;
    }

    private Duration extractTimeoutFromAppAttributes(ProcessContext context, TimeoutType timeoutType) {
        CloudApplicationExtended app = context.getVariable(Variables.APP_TO_PROCESS);
        ApplicationAttributes appAttributes = ApplicationAttributes.fromApplication(app, app.getEnv());
        Number timeout = appAttributes.get(timeoutType.getModuleLevelParamName(), Number.class);
        if (timeout != null && (timeout.intValue() < 0 || timeout.intValue() > timeoutType.getMaxAllowedValue())) {
            throw new ContentException(Messages.PARAMETER_0_MUST_BE_POSITIVE_WITH_MAX_VALUE_1,
                                       timeoutType.getModuleLevelParamName(),
                                       timeoutType.getMaxAllowedValue());
        }
        return timeout != null ? Duration.ofSeconds(timeout.intValue()) : null;
    }

    private Duration extractTimeoutFromDescriptorParameters(ProcessContext context, TimeoutType timeoutType) {
        DeploymentDescriptor descriptor = context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR);
        Object timeoutGlobalLevelValue = descriptor.getParameters()
                                                   .get(timeoutType.getProcessVariableAndGlobalLevelParamName());
        validateTimeoutGlobalValue(timeoutGlobalLevelValue, timeoutType);
        return timeoutGlobalLevelValue != null ? Duration.ofSeconds((Integer) timeoutGlobalLevelValue) : null;
    }

    private void validateTimeoutGlobalValue(Object timeout, TimeoutType timeoutType) {
        if (timeout != null && (timeout instanceof String || ((Number) timeout).intValue() < 0
            || ((Number) timeout).intValue() > timeoutType.getMaxAllowedValue())) {
            throw new ContentException(Messages.PARAMETER_0_MUST_BE_POSITIVE_WITH_MAX_VALUE_1,
                                       timeoutType.getProcessVariableAndGlobalLevelParamName(),
                                       timeoutType.getMaxAllowedValue());
        }
    }

    private void logTimeout(String moduleLevelParameterName, String timeoutParameterName, Number timeout) {
        String timeoutTypeName = processString(moduleLevelParameterName);
        if (timeoutParameterName.equals(DEFAULT_TIMEOUT)) {
            getStepLogger().debug(Messages.TIMEOUT_DEFAULT_VALUE_MESSAGE, timeoutTypeName, timeout);
        } else {
            getStepLogger().debug(Messages.TIMEOUT_MESSAGE, timeoutTypeName, timeoutParameterName, timeout);
        }
    }

    private String processString(String input) {
        return input.replace("-", " ")
                    .replace("timeout", "")
                    .trim();
    }

}
