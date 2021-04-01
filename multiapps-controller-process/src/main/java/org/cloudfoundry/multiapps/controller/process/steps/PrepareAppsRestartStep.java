package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.core.helpers.ModuleToDeployHelper;
import org.cloudfoundry.multiapps.controller.process.util.BlueGreenVariablesSetter;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@Named("prepareAppsRestartStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class PrepareAppsRestartStep extends PrepareModulesDeploymentStep {

    private ModuleToDeployHelper moduleToDeployHelper;

    @Inject
    public PrepareAppsRestartStep(BlueGreenVariablesSetter blueGreenVariablesSetter, ModuleToDeployHelper moduleToDeployHelper) {
        super(blueGreenVariablesSetter);
        this.moduleToDeployHelper = moduleToDeployHelper;
    }

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        super.executeStep(context);

        context.setVariable(Variables.REBUILD_APP_ENV, true);
        context.setVariable(Variables.SHOULD_UPLOAD_APPLICATION_CONTENT, false);
        context.setVariable(Variables.EXECUTE_ONE_OFF_TASKS, false);
        context.setVariable(Variables.SHOULD_SKIP_SERVICE_REBINDING, true);
        context.setVariable(Variables.USE_IDLE_URIS, false);
        context.setVariable(Variables.DELETE_IDLE_URIS, true);
        context.setVariable(Variables.SKIP_UPDATE_CONFIGURATION_ENTRIES, false);
        context.setVariable(Variables.SKIP_MANAGE_SERVICE_BROKER, false);
        context.setVariable(Variables.ITERATED_MODULES_IN_PARALLEL, Collections.emptyList());

        return StepPhase.DONE;
    }

    @Override
    protected List<Module> getModulesToDeploy(ProcessContext context) {
        List<Module> allModulesToDeploy = context.getVariable(Variables.ALL_MODULES_TO_DEPLOY);
        return allModulesToDeploy.stream()
                                 .filter(module -> moduleToDeployHelper.isApplication(module))
                                 .collect(Collectors.toList());
    }

}
