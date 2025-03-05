package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.List;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
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

import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named("stopAppStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class StopAppStep extends SyncFlowableStepWithHooks implements BeforeStepHookPhaseProvider, AfterStepHookPhaseProvider {

    @Inject
    private DeploymentTypeDeterminer deploymentTypeDeterminer;
    @Inject
    private ApplicationWaitAfterStopHandler waitAfterStopHandler;

    @Override
    public StepPhase executeStepInternal(ProcessContext context) {
        CloudApplicationExtended app = context.getVariable(Variables.APP_TO_PROCESS);

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
        List<HookPhase> hookPhases = List.of(HookPhase.BEFORE_STOP);
        return hooksPhaseBuilder.buildHookPhases(hookPhases, context);
    }

    @Override
    public List<HookPhase> getHookPhasesAfterStep(ProcessContext context) {
        List<HookPhase> hookPhases = List.of(HookPhase.AFTER_STOP);
        return hooksPhaseBuilder.buildHookPhases(hookPhases, context);
    }

}
