package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.cloudfoundry.multiapps.controller.core.model.HookPhase;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.ApplicationWaitAfterStopHandler;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;

@Named("stopApplicationUndeploymentStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class StopApplicationUndeploymentStep extends UndeployAppStep implements BeforeStepHookPhaseProvider, AfterStepHookPhaseProvider {

    @Inject
    private ApplicationWaitAfterStopHandler waitAfterStopHandler;

    @Override
    public StepPhase undeployApplication(CloudControllerClient client, CloudApplication cloudApplicationToUndeploy,
                                         ProcessContext context) {
        getStepLogger().info(Messages.STOPPING_APP, cloudApplicationToUndeploy.getName());
        client.stopApplication(cloudApplicationToUndeploy.getName());
        getStepLogger().debug(Messages.APP_STOPPED, cloudApplicationToUndeploy.getName());
        waitAfterStopHandler.configureDelayAfterAppStop(context, cloudApplicationToUndeploy.getName());
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return MessageFormat.format(Messages.ERROR_STOPPING_APP, context.getVariable(Variables.APP_TO_PROCESS)
                                                                        .getName());
    }

    @Override
    public List<HookPhase> getHookPhasesBeforeStep(ProcessContext context) {
        return hooksPhaseBuilder.buildHookPhases(Arrays.asList(HookPhase.BEFORE_STOP, HookPhase.APPLICATION_BEFORE_STOP_LIVE), context);
    }

    @Override
    public List<HookPhase> getHookPhasesAfterStep(ProcessContext context) {
        return hooksPhaseBuilder.buildHookPhases(Arrays.asList(HookPhase.AFTER_STOP, HookPhase.APPLICATION_AFTER_STOP_LIVE), context);
    }
}
