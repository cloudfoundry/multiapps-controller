package org.cloudfoundry.multiapps.controller.process.steps;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.common.ContentException;
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
        getStepLogger().debug("Extracting all timeout parameters from descriptor");

        DeploymentDescriptor descriptor = timeoutValueResolver.getDeploymentDescriptor(context, getStepLogger());
        if (descriptor == null) {
            getStepLogger().debug("No descriptor found; sub-processes will use default timeouts");
            return StepPhase.DONE;
        }

        int successCount = 0;
        for (TimeoutType timeoutType : TimeoutType.values()) {
            if (setTimeoutIfResolved(context, timeoutType)) {
                successCount++;
            }
        }

        getStepLogger().info("Successfully extracted {0} timeout parameters from descriptor", successCount);
        return StepPhase.DONE;
    }

    private boolean setTimeoutIfResolved(ProcessContext context, TimeoutType timeoutType) {
        try {
            TimeoutValueResolver.TimeoutResolution resolution =
                timeoutValueResolver.resolveTimeout(context, timeoutType, getStepLogger());
            context.setVariable(timeoutType.getProcessVariable(), resolution.timeout());
            getStepLogger().debug("Timeout {0} = {1}s (from {2})",
                                  timeoutType.getProcessVariable()
                                             .getName(),
                                  resolution.timeout()
                                            .toSeconds(),
                                  resolution.parameterName());
            return true;
        } catch (ContentException e) {
            getStepLogger().warn("Failed to resolve timeout for {0}: {1}", timeoutType, e.getMessage());
        }
        return false;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return "Error while extracting global timeouts from deployment descriptor";
    }
}

