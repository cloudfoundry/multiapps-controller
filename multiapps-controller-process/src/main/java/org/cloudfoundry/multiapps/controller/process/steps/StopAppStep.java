package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.api.model.ProcessType;
import org.cloudfoundry.multiapps.controller.core.model.HookPhase;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.ApplicationWaitAfterStopHandler;
import org.cloudfoundry.multiapps.controller.process.util.DeploymentTypeDeterminer;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication.State;

@Named("stopAppStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class StopAppStep extends SyncFlowableStepWithHooks implements BeforeStepHookPhaseProvider, AfterStepHookPhaseProvider {

    @Inject
    private DeploymentTypeDeterminer deploymentTypeDeterminer;

    @Inject
    private ApplicationWaitAfterStopHandler waitAfterStopHandler;

    @Override
    public StepPhase executeStepInternal(ProcessContext context) {

        CloudApplication app = context.getVariable(Variables.APP_TO_PROCESS);

        CloudApplication existingApp = context.getVariable(Variables.EXISTING_APP);
        if (existingApp != null && !existingApp.getState()
                                               .equals(State.STOPPED)) {
            getStepLogger().info(Messages.STOPPING_APP, app.getName());

            CloudControllerClient client = context.getControllerClient();

            client.stopApplication(app.getName());

            getStepLogger().debug(Messages.APP_STOPPED, app.getName());
            waitAfterStopHandler.configureDelayAfterAppStop(context, app.getName());
        } else {
            getStepLogger().debug("Application \"{0}\" already stopped", app.getName());
        }
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return MessageFormat.format(Messages.ERROR_STOPPING_APP, context.getVariable(Variables.APP_TO_PROCESS)
                                                                        .getName());
    }

    @Override
    public List<HookPhase> getHookPhasesBeforeStep(ProcessContext context) {
        List<HookPhase> hookPhases = getHookPhases(HookPhase.APPLICATION_BEFORE_STOP_IDLE, HookPhase.APPLICATION_BEFORE_STOP_LIVE, context);
        hookPhases.add(HookPhase.BEFORE_STOP);
        return hooksPhaseBuilder.buildHookPhases(hookPhases, context);
    }

    @Override
    public List<HookPhase> getHookPhasesAfterStep(ProcessContext context) {
        List<HookPhase> hookPhases = getHookPhases(HookPhase.APPLICATION_AFTER_STOP_IDLE, HookPhase.APPLICATION_AFTER_STOP_LIVE, context);
        hookPhases.add(HookPhase.AFTER_STOP);
        return hooksPhaseBuilder.buildHookPhases(hookPhases, context);
    }

    private List<HookPhase> getHookPhases(HookPhase beforeStepHookPhase, HookPhase afterStepHookPhase, ProcessContext context) {
        List<HookPhase> hookPhases = new ArrayList<>();
        ProcessType processType = deploymentTypeDeterminer.determineDeploymentType(context);
        if (ProcessType.BLUE_GREEN_DEPLOY.equals(processType)) {
            hookPhases.add(beforeStepHookPhase);
        } else {
            hookPhases.add(afterStepHookPhase);
        }
        return hookPhases;
    }

}
