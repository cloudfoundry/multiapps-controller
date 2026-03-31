package org.cloudfoundry.multiapps.controller.process.steps;

import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.process.util.TimeoutType;
import org.cloudfoundry.multiapps.controller.process.util.TimeoutValueResolver;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

/**
 * Early-stage step that extracts ALL timeout parameters from the descriptor ONCE and stores them as process variables.
 *
 * This eliminates the need to pass large descriptor objects through BPMN call activities just to extract timeout values. All sub-processes
 * inherit the pre-calculated timeouts from the parent process without needing the descriptor.
 *
 * Benefits: - Zero descriptor objects in BPMN variable passing - Single extraction point for all 12 timeout types - Cleaner BPMN diagrams
 * (no descriptor mappings in call activities) - Better memory usage and performance
 *
 * Should be called early in the main deployment process (after ProcessMtaArchiveStep but before any call activities that need timeouts).
 *
 * @since 2.0
 */
@Named("globalTimeoutSettingStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class GlobalTimeoutSettingStep extends SyncFlowableStep {

    private final TimeoutValueResolver timeoutValueResolver = new TimeoutValueResolver();

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        getStepLogger().debug("Extracting all timeout parameters from descriptor");

        DeploymentDescriptor descriptor = timeoutValueResolver.getDeploymentDescriptor(context, getStepLogger());
        if (descriptor == null) {
            getStepLogger().debug("No descriptor found; sub-processes will use default timeouts");
            return StepPhase.DONE;
        }

        // Extract and set ALL timeout types at once
        // This ensures all sub-processes have timeout values available without needing descriptors
        int successCount = 0;
        for (TimeoutType timeoutType : TimeoutType.values()) {
            if (setTimeoutIfResolved(context, timeoutType)) {
                successCount++;
            }
        }

        getStepLogger().info("Successfully extracted {0} timeout parameters from descriptor", successCount);
        return StepPhase.DONE;
    }

    /**
     * Resolves and sets a single timeout value in the process context.
     *
     * @param context the process context
     * @param timeoutType the timeout type to resolve
     * @return true if timeout was successfully set, false if not available
     */
    private boolean setTimeoutIfResolved(ProcessContext context, TimeoutType timeoutType) {
        try {
            TimeoutValueResolver.TimeoutResolution resolution =
                timeoutValueResolver.resolveTimeout(context, timeoutType, getStepLogger());

            if (resolution != null && resolution.timeout() != null) {
                context.setVariable(timeoutType.getProcessVariable(), resolution.timeout());
                getStepLogger().debug("Timeout {0} = {1}s (from {2})",
                                      timeoutType.getProcessVariable()
                                                 .getName(),
                                      resolution.timeout()
                                                .toSeconds(),
                                      resolution.parameterName());
                return true;
            }
        } catch (Exception e) {
            getStepLogger().warn("Failed to resolve timeout for {0}: {1}", timeoutType, e.getMessage());
        }
        return false;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return "Error while extracting global timeouts from deployment descriptor";
    }
}

