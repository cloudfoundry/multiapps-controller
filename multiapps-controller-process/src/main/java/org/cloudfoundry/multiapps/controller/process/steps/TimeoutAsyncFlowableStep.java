package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.time.Duration;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.TimeoutStepStateManager;
import org.cloudfoundry.multiapps.controller.process.util.TimeoutType;
import org.cloudfoundry.multiapps.controller.process.util.TimeoutValueResolver;
import org.cloudfoundry.multiapps.controller.process.util.TimeoutValueResolver.TimeoutResolution;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

public abstract class TimeoutAsyncFlowableStep extends AsyncFlowableStep {
    private static final String DEFAULT_TIMEOUT = "default";
    private final TimeoutStepStateManager timeoutStepStateManager = new TimeoutStepStateManager();
    private final TimeoutValueResolver timeoutValueResolver = new TimeoutValueResolver();

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
        String operationTypeName = determineOperationType(context, timeoutType);
        String timeoutParameterName = resolvedTimeout.parameterName();
        long timeoutSeconds = resolvedTimeout.timeout().toSeconds();
        
        if (timeoutParameterName.equals(DEFAULT_TIMEOUT)) {
            getStepLogger().info(Messages.OPERATION_TIMEOUT_DEFAULT_VALUE_MESSAGE, operationTypeName, timeoutSeconds);
        } else {
            getStepLogger().info(Messages.OPERATION_TIMEOUT_MESSAGE, operationTypeName, timeoutParameterName, timeoutSeconds);
        }
    }

    private String determineOperationType(ProcessContext context, TimeoutType timeoutType) {
        return switch (timeoutType) {
            case UPLOAD, STAGE, START, TASK -> getApplicationOperationName(context);
            case BIND_SERVICE, UNBIND_SERVICE -> getBindingOperationName(context, timeoutType);
            case CREATE_SERVICE, DELETE_SERVICE, UPDATE_SERVICE, CREATE_SERVICE_KEY, DELETE_SERVICE_KEY -> 
                getServiceObjectOperationName(context, timeoutType);
        };
    }

    private String getApplicationOperationName(ProcessContext context) {
        CloudApplicationExtended app = context.getVariable(Variables.APP_TO_PROCESS);
        String appName = app != null ? app.getName() : null;
        return appName != null ? "Application " + appName : "Application";
    }

    private String getBindingOperationName(ProcessContext context, TimeoutType timeoutType) {
        String service = context.getVariable(Variables.SERVICE_TO_UNBIND_BIND);
        String operationName = timeoutType == TimeoutType.BIND_SERVICE ? "Service binding" : "Service unbinding";
        return service != null ? operationName + " for " + service : operationName;
    }

    private String getServiceObjectOperationName(ProcessContext context, TimeoutType timeoutType) {
        String operationName = getServiceOperationName(timeoutType);
        String serviceName = getServiceName(context);
        return serviceName != null ? operationName + " for " + serviceName : operationName;
    }

    private String getServiceOperationName(TimeoutType timeoutType) {
        return switch (timeoutType) {
            case CREATE_SERVICE -> "Service creation";
            case DELETE_SERVICE -> "Service deletion";
            case UPDATE_SERVICE -> "Service update";
            case CREATE_SERVICE_KEY -> "Service key creation";
            case DELETE_SERVICE_KEY -> "Service key deletion";
            default -> "Service operation";
        };
    }

    private String getServiceName(ProcessContext context) {
        Object serviceObj = context.getVariable(Variables.SERVICE_TO_PROCESS);
        if (serviceObj instanceof org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended service) {
            return service.getResourceName();
        }
        return null;
    }

    public abstract Duration getTimeout(ProcessContext context);
}
