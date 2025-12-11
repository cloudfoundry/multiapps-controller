package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.client.facade.CloudControllerClient;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudApplication;
import org.cloudfoundry.multiapps.controller.core.helpers.ModuleToDeployHelper;
import org.cloudfoundry.multiapps.controller.core.model.BlueGreenApplicationNameSuffix;
import org.cloudfoundry.multiapps.controller.core.model.HookPhase;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.ApplicationWaitAfterStopHandler;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@Named("stopDependentModuleStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class StopDependentModuleStep extends SyncFlowableStepWithHooks implements BeforeStepHookPhaseProvider, AfterStepHookPhaseProvider {

    @Inject
    private ModuleToDeployHelper moduleToDeployHelper;

    @Inject
    private ApplicationWaitAfterStopHandler waitAfterStopHandler;

    @Override
    protected StepPhase executeStepInternal(ProcessContext context) {
        CloudControllerClient client = context.getControllerClient();
        String idleName = getCurrentModuleToStop(context).getName() + BlueGreenApplicationNameSuffix.IDLE.asSuffix();
        CloudApplication app = client.getApplication(idleName);
        if (app != null && !app.getState()
                               .equals(CloudApplication.State.STOPPED)) {
            client.stopApplication(idleName);
            getStepLogger().error(Messages.APP_STOPPED, idleName);
        }
        waitAfterStopHandler.configureDelayAfterAppStop(context, idleName);
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return MessageFormat.format("Failed to stop dependent modules", getCurrentModuleToStop(context).getName());
    }

    static Module getCurrentModuleToStop(ProcessContext context) {
        List<Module> modules = context.getVariable(Variables.DEPENDENT_MODULES_TO_STOP);
        int index = context.getVariable(Variables.APPS_TO_STOP_INDEX);
        return modules.get(index);
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
