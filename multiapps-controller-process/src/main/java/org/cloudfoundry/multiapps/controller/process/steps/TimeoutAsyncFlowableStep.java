package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.time.Duration;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.TimeoutStepStateManager;
import org.cloudfoundry.multiapps.controller.process.util.TimeoutType;
import org.cloudfoundry.multiapps.controller.process.util.TimeoutValueResolver;
import org.cloudfoundry.multiapps.controller.process.util.TimeoutValueResolver.TimeoutResolution;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

import jakarta.inject.Inject;

public abstract class TimeoutAsyncFlowableStep extends AsyncFlowableStep {
    private static final String DEFAULT_TIMEOUT = "default";
    private final TimeoutStepStateManager timeoutStepStateManager = new TimeoutStepStateManager();
    @Inject
    private TimeoutValueResolver timeoutValueResolver;

    @Override
    public StepPhase executeStep(ProcessContext context) throws Exception {
        boolean hasTimedOut = hasTimedOut(context);
        if (hasTimedOut) {
            throw new SLException(MessageFormat.format(Messages.EXECUTION_OF_STEP_HAS_TIMED_OUT, getStepName()));
        }
        return super.executeStep(context);
    }

    private boolean hasTimedOut(ProcessContext context) {
        return timeoutStepStateManager.hasTimedOut(context, getStepName(), getTimeout(context));
    }

    private String getStepName() {
        return getClass().getSimpleName();
    }

    protected Duration calculateTimeout(ProcessContext context, TimeoutType timeoutType) {
        TimeoutResolution resolvedTimeout = timeoutValueResolver.resolveTimeout(context, timeoutType, getStepLogger());
        logTimeout(context, timeoutType, resolvedTimeout);
        return resolvedTimeout.timeout();
    }

    private void logTimeout(ProcessContext context, TimeoutType timeoutType, TimeoutResolution resolvedTimeout) {
        String operationName = resolveOperationName(context, timeoutType);
        String parameterName = resolvedTimeout.parameterName();
        long timeoutSeconds = resolvedTimeout.timeout().toSeconds();

        if (DEFAULT_TIMEOUT.equals(parameterName)) {
            logDefaultTimeout(operationName, timeoutSeconds);
        } else {
            logParameterizedTimeout(operationName, parameterName, timeoutSeconds);
        }
    }

    private void logDefaultTimeout(String operationName, long timeoutSeconds) {
        getStepLogger().debug(Messages.OPERATION_TIMEOUT_DEFAULT_VALUE_MESSAGE, operationName, timeoutSeconds);
    }

    private void logParameterizedTimeout(String operationName, String parameterName, long timeoutSeconds) {
        getStepLogger().debug(Messages.OPERATION_TIMEOUT_MESSAGE, operationName, parameterName, timeoutSeconds);
    }

    private String resolveOperationName(ProcessContext context, TimeoutType timeoutType) {
        return switch (timeoutType) {
            case UPLOAD, STAGE, START, TASK -> resolveAppOperationName(context);
            case BIND_SERVICE -> resolveServiceBindingName(context);
            case CREATE_SERVICE, CREATE_SERVICE_KEY -> resolveServiceOperationName(context, timeoutType);
        };
    }

    private String resolveAppOperationName(ProcessContext context) {
        CloudApplicationExtended app = context.getVariable(Variables.APP_TO_PROCESS);
        return app != null ? "Application " + app.getName() : "Application";
    }

    private String resolveServiceBindingName(ProcessContext context) {
        String service = context.getVariable(Variables.SERVICE_TO_UNBIND_BIND);
        return service != null ? "Service binding for " + service : "Service binding";
    }

    private String resolveServiceOperationName(ProcessContext context, TimeoutType timeoutType) {
        String operationVerb = switch (timeoutType) {
            case CREATE_SERVICE -> "Service creation";
            case CREATE_SERVICE_KEY -> "Service key creation";
            default -> "Service operation";
        };
        
        String serviceName = getServiceResourceName(context);
        return serviceName != null ? operationVerb + " for " + serviceName : operationVerb;
    }

    private String getServiceResourceName(ProcessContext context) {
        Object serviceObj = context.getVariable(Variables.SERVICE_TO_PROCESS);
        if (serviceObj instanceof CloudServiceInstanceExtended service) {
            return service.getResourceName();
        }
        return null;
    }

    public abstract Duration getTimeout(ProcessContext context);
}
