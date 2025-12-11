package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.List;

import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.core.model.SubprocessPhase;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@Named("prepareToStopDependentModulesStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class PrepareToStopDependentModulesStep extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        List<Module> dependentModulesToStop = context.getVariable(Variables.DEPENDENT_MODULES_TO_STOP);
        context.setVariable(Variables.APPS_TO_STOP_COUNT, dependentModulesToStop.size());
        context.setVariable(Variables.APPS_TO_STOP_INDEX, 0);
        context.setVariable(Variables.INDEX_VARIABLE_NAME, Variables.APPS_TO_STOP_INDEX.getName());
        context.setVariable(Variables.SUBPROCESS_PHASE, SubprocessPhase.BEFORE_APPLICATION_STOP);
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return MessageFormat.format(Messages.ERROR_WHEN_CONFIGURING_STOPPING_OF_DEPENDENT_MODULES,
                                    context.getVariable(Variables.APP_TO_PROCESS)
                                           .getName());
    }

}