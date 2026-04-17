package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.time.Duration;

import jakarta.inject.Inject;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.TimeoutStepStateManager;
import org.cloudfoundry.multiapps.controller.process.util.TimeoutType;
import org.cloudfoundry.multiapps.controller.process.util.TimeoutValueResolver;
import org.cloudfoundry.multiapps.controller.process.util.TimeoutValueResolver.TimeoutResolution;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

public abstract class TimeoutAsyncFlowableStep extends AsyncFlowableStep {

    private static final String DEFAULT_TIMEOUT = "default";

    private final TimeoutStepStateManager timeoutStepStateManager = new TimeoutStepStateManager();

    @Inject
    private TimeoutValueResolver timeoutValueResolver;

    @Override
    public StepPhase executeStep(ProcessContext context) throws Exception {
        if (timeoutStepStateManager.hasTimedOut(context, getClass().getSimpleName(), getTimeout(context))) {
            throw new SLException(MessageFormat.format(Messages.EXECUTION_OF_STEP_HAS_TIMED_OUT, getClass().getSimpleName()));
        }
        return super.executeStep(context);
    }

    protected Duration calculateTimeout(ProcessContext context, TimeoutType timeoutType) {
        TimeoutResolution resolution = timeoutValueResolver.resolveTimeout(context, timeoutType, getStepLogger());
        logTimeout(context, timeoutType, resolution);
        return resolution.timeout();
    }

    private void logTimeout(ProcessContext context, TimeoutType timeoutType, TimeoutResolution resolution) {
        String operation = resolveOperationName(context, timeoutType);
        long seconds = resolution.timeout().toSeconds();
        if (DEFAULT_TIMEOUT.equals(resolution.parameterName())) {
            getStepLogger().debug(Messages.OPERATION_TIMEOUT_DEFAULT_VALUE_MESSAGE, operation, seconds);
        } else {
            getStepLogger().debug(Messages.OPERATION_TIMEOUT_MESSAGE, operation, resolution.parameterName(), seconds);
        }
    }

    private String resolveOperationName(ProcessContext context, TimeoutType timeoutType) {
        return switch (timeoutType) {
            case UPLOAD, STAGE, START, TASK -> {
                CloudApplicationExtended app = context.getVariable(Variables.APP_TO_PROCESS);
                yield app != null ? "Application " + app.getName() : "Application";
            }
            case BIND_SERVICE -> {
                String service = context.getVariable(Variables.SERVICE_TO_UNBIND_BIND);
                yield service != null ? "Service binding for " + service : "Service binding";
            }
            case CREATE_SERVICE -> formatServiceOperation("Service creation", context);
            case CREATE_SERVICE_KEY -> formatServiceOperation("Service key creation", context);
        };
    }

    private String formatServiceOperation(String operation, ProcessContext context) {
        CloudServiceInstanceExtended service = context.getVariable(Variables.SERVICE_TO_PROCESS);
        return service != null && service.getResourceName() != null
            ? operation + " for " + service.getResourceName()
            : operation;
    }

    public abstract Duration getTimeout(ProcessContext context);
}
