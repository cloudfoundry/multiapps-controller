package org.cloudfoundry.multiapps.controller.process.steps;

import java.time.Duration;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.TimeoutType;
import org.cloudfoundry.multiapps.controller.process.util.TimeoutValueResolver;
import org.cloudfoundry.multiapps.controller.process.variables.Variable;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@Named("globalTimeoutSettingStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class GlobalTimeoutSettingStep extends SyncFlowableStep {

    @Inject
    private TimeoutValueResolver timeoutValueResolver;

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        getStepLogger().debug(Messages.EXTRACTING_ALL_TIMEOUT_PARAMETERS_FROM_DESCRIPTOR);

        DeploymentDescriptor descriptor = timeoutValueResolver.getDeploymentDescriptor(context, getStepLogger());
        if (descriptor == null) {
            getStepLogger().debug(Messages.NO_DESCRIPTOR_FOUND_USING_DEFAULT_TIMEOUTS);
            return StepPhase.DONE;
        }

        int successCount = 0;
        for (TimeoutType timeoutType : TimeoutType.values()) {
            if (setTimeoutIfResolved(context, timeoutType)) {
                successCount++;
            }
        }

        getStepLogger().info(Messages.SUCCESSFULLY_EXTRACTED_0_TIMEOUT_PARAMETERS, successCount);
        return StepPhase.DONE;
    }

    private boolean setTimeoutIfResolved(ProcessContext context, TimeoutType timeoutType) {
        try {
            if (isAlreadySetFromOperationParams(context, timeoutType)) {
                logExistingOperationParamsTimeout(context, timeoutType);
                return true;
            }
            return resolveAndSetTimeout(context, timeoutType);
        } catch (ContentException e) {
            getStepLogger().warn(Messages.FAILED_TO_RESOLVE_TIMEOUT_FOR_0_1, timeoutType, e.getMessage());
            return false;
        }
    }

    private boolean isAlreadySetFromOperationParams(ProcessContext context, TimeoutType timeoutType) {
        if (!timeoutType.isServiceScoped()) {
            return false;
        }
        Variable<Boolean> operationParamsFlag = timeoutType.getOperationParamsFlag();
        return operationParamsFlag != null && Boolean.TRUE.equals(context.getVariableIfSet(operationParamsFlag));
    }

    private void logExistingOperationParamsTimeout(ProcessContext context, TimeoutType timeoutType) {
        Duration timeout = context.getVariableIfSet(timeoutType.getProcessVariable());
        String timeoutSeconds = timeout != null ? String.valueOf(timeout.toSeconds()) : "null";
        getStepLogger().debug(Messages.TIMEOUT_0_EQUALS_1_SECONDS_FROM_2,
                              timeoutType.getProcessVariable().getName(),
                              timeoutSeconds,
                              timeoutType.getGlobalLevelParamName());
    }

    private boolean resolveAndSetTimeout(ProcessContext context, TimeoutType timeoutType) {
        TimeoutValueResolver.TimeoutResolution resolution = timeoutValueResolver.resolveTimeout(context, timeoutType, getStepLogger());
        context.setVariable(timeoutType.getProcessVariable(), resolution.timeout());
        getStepLogger().debug(Messages.TIMEOUT_0_EQUALS_1_SECONDS_FROM_2,
                              timeoutType.getProcessVariable().getName(),
                              resolution.timeout().toSeconds(),
                              resolution.parameterName());
        return true;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_EXTRACTING_GLOBAL_TIMEOUTS_FROM_DESCRIPTOR;
    }
}

