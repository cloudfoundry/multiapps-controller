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
    private static final String APPLICATION_PHASE_SUBSTRING = ".application.";

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        CloudApplicationExtended app = context.getVariable(Variables.APP_TO_PROCESS);
        Hook hook = context.getVariable(Variables.HOOK_FOR_EXECUTION);

        String resolvedAppName = resolveTargetAppName(context, app, hook);
        if (!resolvedAppName.equals(app.getName())) {
            context.setVariable(Variables.APP_TO_PROCESS, buildAppWithName(app, resolvedAppName));
        }

        return StepPhase.DONE;
    }

    private String resolveTargetAppName(ProcessContext context, CloudApplicationExtended app, Hook hook) {
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
        String hookExecutionPhase = context.getVariable(Variables.HOOK_EXECUTION_PHASE);
        return hook.getPhases()
                   .stream()
                   .filter(p -> p.equals(hookExecutionPhase))
                   .findFirst()
                   .orElseGet(() -> hook.getPhases()
                                        .stream()
                                        .filter(p -> p.contains(APPLICATION_PHASE_SUBSTRING))
                                        .findFirst()
                                        .orElse(""));
    }

    private String resolveAppNameForTarget(ProcessContext context, CloudApplicationExtended app, String targetApp) {
        ApplicationColor idleColor = context.getVariable(Variables.IDLE_MTA_COLOR);
        ApplicationColor liveColor = context.getVariable(Variables.LIVE_MTA_COLOR);

        if (idleColor == null || liveColor == null) {
            return resolveAppNameWithLiveIdleSuffix(app.getName(), targetApp);
        }

        if (HookPhaseProcessType.HookProcessPhase.IDLE.getType().equals(targetApp)) {
            return swapColorSuffix(app.getName(), liveColor, idleColor);
        }
        if (HookPhaseProcessType.HookProcessPhase.LIVE.getType().equals(targetApp)) {
            return swapColorSuffix(app.getName(), idleColor, liveColor);
        }
        return app.getName();
    }

    private String resolveAppNameWithLiveIdleSuffix(String appName, String targetApp) {
        String liveSuffix = BlueGreenApplicationNameSuffix.LIVE.asSuffix();
        String idleSuffix = BlueGreenApplicationNameSuffix.IDLE.asSuffix();

        if (HookPhaseProcessType.HookProcessPhase.IDLE.getType().equals(targetApp)) {
            if (appName.endsWith(liveSuffix)) {
                return appName.substring(0, appName.length() - liveSuffix.length());
            }
            if (!appName.endsWith(idleSuffix)) {
                return appName + idleSuffix;
            }
        }
        if (HookPhaseProcessType.HookProcessPhase.LIVE.getType().equals(targetApp)) {
            if (appName.endsWith(idleSuffix)) {
                return appName.substring(0, appName.length() - idleSuffix.length());
            }
            if (!appName.endsWith(liveSuffix)) {
                return appName + liveSuffix;
            }
        }
        return appName;
    }

    private String swapColorSuffix(String appName, ApplicationColor fromColor, ApplicationColor toColor) {
        String fromSuffix = fromColor.asSuffix();
        String toSuffix = toColor.asSuffix();
        if (appName.endsWith(fromSuffix)) {
            return appName.substring(0, appName.length() - fromSuffix.length()) + toSuffix;
        }
        if (!appName.endsWith(toSuffix)) {
            return appName + toSuffix;
        }
        return appName;
    }

    private CloudApplicationExtended buildAppWithName(CloudApplicationExtended app, String resolvedAppName) {
        return org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended.copyOf(app)
                                                                                                        .withName(resolvedAppName);
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return MessageFormat.format(Messages.ERROR_EXECUTING_HOOK,
                                    context.getVariable(Variables.HOOK_FOR_EXECUTION).getName());
    }

}
