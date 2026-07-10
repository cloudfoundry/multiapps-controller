package org.cloudfoundry.multiapps.controller.process.steps;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.TimeoutType;
import org.cloudfoundry.multiapps.controller.process.util.TimeoutValueResolver;
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
            if (isAlreadySetFromOperationParams(context, timeoutType)) {
                logExistingTimeout(context, timeoutType);
                successCount++;
            }
        }

        getStepLogger().debug(Messages.SUCCESSFULLY_EXTRACTED_0_TIMEOUT_PARAMETERS, successCount);
        return StepPhase.DONE;
    }

    private boolean isAlreadySetFromOperationParams(ProcessContext context, TimeoutType timeoutType) {
        return context.getVariableIfSet(timeoutType.getProcessVariable()) != null;
    }

    private void logExistingTimeout(ProcessContext context, TimeoutType timeoutType) {
        try {
            TimeoutValueResolver.TimeoutResolution resolution = timeoutValueResolver.resolveTimeout(context, timeoutType,
                                                                                                   getStepLogger());
            getStepLogger().debug(Messages.TIMEOUT_0_EQUALS_1_SECONDS_FROM_2,
                                  timeoutType.getProcessVariable()
                                             .getName(),
                                  resolution.timeout()
                                            .toSeconds(),
                                  resolution.parameterName());
        } catch (ContentException e) {
            getStepLogger().warn(Messages.FAILED_TO_RESOLVE_TIMEOUT_FOR_0_1, timeoutType, e.getMessage());
        }
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_EXTRACTING_GLOBAL_TIMEOUTS_FROM_DESCRIPTOR;
    }
}

