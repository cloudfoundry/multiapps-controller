package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.time.Duration;
import java.util.List;
import java.util.function.LongSupplier;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.client.facade.CloudControllerClient;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudTask;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientFactory;
import org.cloudfoundry.multiapps.controller.core.model.ApplicationColor;
import org.cloudfoundry.multiapps.controller.core.model.HookPhaseProcessType;
import org.cloudfoundry.multiapps.controller.core.security.token.TokenService;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.TimeoutType;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.Hook;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@Named("executeTaskStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ExecuteTaskStep extends TimeoutAsyncFlowableStep {

    protected LongSupplier currentTimeSupplier = System::currentTimeMillis;

    @Inject
    private CloudControllerClientFactory clientFactory;
    @Inject
    private TokenService tokenService;

    @Override
    protected StepPhase executeAsyncStep(ProcessContext context) {
        CloudApplicationExtended app = context.getVariable(Variables.APP_TO_PROCESS);
        CloudTask taskToExecute = StepsUtil.getTask(context);
        CloudControllerClient client = context.getControllerClient();

        String appName = resolveTargetAppName(context, app);

        getStepLogger().info(Messages.EXECUTING_TASK_ON_APP, taskToExecute.getName(), appName);
        CloudTask startedTask = client.runTask(appName, taskToExecute);
        context.setVariable(Variables.STARTED_TASK, startedTask);
        context.setVariable(Variables.START_TIME, currentTimeSupplier.getAsLong());
        return StepPhase.POLL;
    }

    private String resolveTargetAppName(ProcessContext context, CloudApplicationExtended app) {
        Hook hook = context.getVariable(Variables.HOOK_FOR_EXECUTION);
        if (hook == null || hook.getPhaseConfigs().isEmpty()) {
            return app.getName();
        }

        String currentPhase = buildCurrentPhaseString(context, hook);
        String targetApp = hook.getPhaseConfigs()
                               .stream()
                               .filter(config -> currentPhase.equals(config.get("phase")))
                               .map(config -> config.get("target-app"))
                               .findFirst()
                               .orElse(null);

        if (targetApp == null) {
            return app.getName();
        }

        return resolveAppNameForTarget(context, app, targetApp);
    }

    private String buildCurrentPhaseString(ProcessContext context, Hook hook) {
        String hookExecutionPhase = context.getVariable(Variables.HOOK_EXECUTION_PHASE);
        return hook.getPhases()
                   .stream()
                   .filter(p -> p.equals(hookExecutionPhase))
                   .findFirst()
                   .orElseGet(() -> hook.getPhases()
                                        .stream()
                                        .filter(p -> p.contains(".application."))
                                        .findFirst()
                                        .orElse(""));
    }

    private String resolveAppNameForTarget(ProcessContext context, CloudApplicationExtended app, String targetApp) {
        ApplicationColor idleColor = context.getVariable(Variables.IDLE_MTA_COLOR);
        ApplicationColor liveColor = context.getVariable(Variables.LIVE_MTA_COLOR);

        if (idleColor == null || liveColor == null) {
            return app.getName();
        }

        if (HookPhaseProcessType.HookProcessPhase.IDLE.getType().equals(targetApp)) {
            return swapColorSuffix(app.getName(), liveColor, idleColor);
        }
        if (HookPhaseProcessType.HookProcessPhase.LIVE.getType().equals(targetApp)) {
            return swapColorSuffix(app.getName(), idleColor, liveColor);
        }
        return app.getName();
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

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        CloudApplicationExtended app = context.getVariable(Variables.APP_TO_PROCESS);
        CloudTask taskToExecute = StepsUtil.getTask(context);
        return MessageFormat.format(Messages.ERROR_EXECUTING_TASK_0_ON_APP_1, taskToExecute.getName(), app.getName());
    }

    @Override
    protected List<AsyncExecution> getAsyncStepExecutions(ProcessContext context) {
        return List.of(new PollExecuteTaskStatusExecution(clientFactory, tokenService));
    }

    @Override
    public Duration getTimeout(ProcessContext context) {
        return calculateTimeout(context, TimeoutType.TASK);
    }

}
