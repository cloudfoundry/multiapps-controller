package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.model.ApplicationColor;
import org.cloudfoundry.multiapps.controller.core.model.BlueGreenApplicationNameSuffix;
import org.cloudfoundry.multiapps.controller.core.model.HookPhaseProcessType;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.Hook;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@Named("resolveHookTaskTargetAppStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ResolveHookTaskTargetAppStep extends SyncFlowableStep {

    private static final String PHASE_KEY = "phase";
    private static final String TARGET_APP_KEY = "target-app";

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        CloudApplicationExtended app = context.getVariable(Variables.APP_TO_PROCESS);

        String resolvedAppName = resolveTargetAppName(context, app);
        if (resolvedAppName == null) {
            context.setVariable(Variables.TASKS_TO_EXECUTE, List.of());
            return StepPhase.DONE;
        }
        if (!resolvedAppName.equals(app.getName())) {
            context.setVariable(Variables.APP_TO_PROCESS, buildAppWithName(app, resolvedAppName));
        }

        return StepPhase.DONE;
    }

    private String resolveTargetAppName(ProcessContext context, CloudApplicationExtended app) {
        Hook hook = context.getVariable(Variables.HOOK_FOR_EXECUTION);
        if (hook == null) {
            return app.getName();
        }

        List<Map<String, String>> phasesConfig = getPhasesConfig(hook);
        if (phasesConfig.isEmpty()) {
            return app.getName();
        }

        String currentPhase = buildCurrentPhaseString(context, hook);
        String targetApp = phasesConfig.stream()
                                       .filter(config -> currentPhase.equals(config.get(PHASE_KEY)))
                                       .map(config -> config.get(TARGET_APP_KEY))
                                       .findFirst()
                                       .orElse(null);

        if (targetApp == null) {
            return app.getName();
        }

        return resolveAppNameForTarget(context, app, targetApp);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> getPhasesConfig(Hook hook) {
        Object phasesConfigValue = hook.getParameters()
                                       .get(SupportedParameters.PHASES_CONFIG);
        if (phasesConfigValue instanceof List) {
            return (List<Map<String, String>>) phasesConfigValue;
        }
        return List.of();
    }

    private String buildCurrentPhaseString(ProcessContext context, Hook hook) {
        List<String> hookExecutionPhases = context.getVariable(Variables.HOOK_EXECUTION_PHASES);
        return hook.getPhases()
                   .stream()
                   .filter(hookExecutionPhases::contains)
                   .findFirst()
                   .orElse("");
    }

    private String resolveAppNameForTarget(ProcessContext context, CloudApplicationExtended app, String targetApp) {
        if (HookPhaseProcessType.HookProcessPhase.LIVE.getType()
                                                      .equals(targetApp)) {
            if (isInitialDeploy(context)) {
                getStepLogger().warn(Messages.SKIPPING_HOOK_TASK_NO_LIVE_APP, app.getName());
                return null;
            }
            ApplicationColor liveColor = context.getVariable(Variables.LIVE_MTA_COLOR);
            String suffix = liveColor != null ? liveColor.asSuffix() : BlueGreenApplicationNameSuffix.LIVE.asSuffix();
            return BlueGreenApplicationNameSuffix.removeSuffix(app.getName()) + suffix;
        }
        if (HookPhaseProcessType.HookProcessPhase.IDLE.getType()
                                                      .equals(targetApp)) {
            String baseName = BlueGreenApplicationNameSuffix.removeSuffix(app.getName());
            ApplicationColor idleColor = context.getVariable(Variables.IDLE_MTA_COLOR);
            if (idleColor != null) {
                return baseName + idleColor.asSuffix();
            }
            if (isAfterRenamePhase(context)) {
                return baseName;
            }
            return baseName + BlueGreenApplicationNameSuffix.IDLE.asSuffix();
        }
        return app.getName();
    }

    private boolean isAfterRenamePhase(ProcessContext context) {
        return context.getVariable(Variables.PHASE) != null;
    }

    private boolean isInitialDeploy(ProcessContext context) {
        return context.getVariable(Variables.DEPLOYED_MTA) == null;
    }

    private CloudApplicationExtended buildAppWithName(CloudApplicationExtended app, String resolvedAppName) {
        return org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended.copyOf(app)
                                                                                                        .withName(resolvedAppName);
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return MessageFormat.format(Messages.ERROR_EXECUTING_HOOK,
                                    context.getVariable(Variables.HOOK_FOR_EXECUTION)
                                           .getName());
    }

}
