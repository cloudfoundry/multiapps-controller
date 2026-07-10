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

    private final TimeoutStepStateManager timeoutStepStateManager = new TimeoutStepStateManager();

    @Inject
    protected TimeoutValueResolver timeoutValueResolver;

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
        getStepLogger().debug(Messages.OPERATION_TIMEOUT_MESSAGE, operation, resolution.parameterName(), seconds);
    }

    private String resolveOperationName(ProcessContext context, TimeoutType timeoutType) {
        return switch (timeoutType) {
            case UPLOAD, STAGE, START, TASK -> {
                CloudApplicationExtended app = context.getVariable(Variables.APP_TO_PROCESS);
                yield app != null ? MessageFormat.format("Application {0}", app.getName()) : "Application";
            }
            case BIND_SERVICE -> {
                String service = context.getVariable(Variables.SERVICE_TO_UNBIND_BIND);
                yield service != null ? MessageFormat.format("Service binding for {0}", service) : "Service binding";
            }
            case CREATE_SERVICE -> formatServiceOperation("Service creation", context);
            case CREATE_SERVICE_KEY -> formatServiceOperation("Service key creation", context);
        };
    }

    private String formatServiceOperation(String operation, ProcessContext context) {
        CloudServiceInstanceExtended service = context.getVariable(Variables.SERVICE_TO_PROCESS);
        return service != null && service.getResourceName() != null
            ? MessageFormat.format("{0} for {1}", operation, service.getResourceName())
            : operation;
    }

    public abstract Duration getTimeout(ProcessContext context);
}
