package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.BlueGreenVariablesSetter;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@Named("prepareModulesDeploymentStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class PrepareModulesDeploymentStep extends SyncFlowableStep {

    private BlueGreenVariablesSetter blueGreenVariablesSetter;

    @Inject
    public PrepareModulesDeploymentStep(BlueGreenVariablesSetter blueGreenVariablesSetter) {
        this.blueGreenVariablesSetter = blueGreenVariablesSetter;
    }

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        getStepLogger().debug(Messages.PREPARING_MODULES_DEPLOYMENT);

        // Get the list of cloud modules from the context:
        List<Module> modulesToDeploy = getModulesToDeploy(context);

        // Initialize the iteration over the applications list:
        context.setVariable(Variables.MODULES_COUNT, modulesToDeploy.size());
        context.setVariable(Variables.MODULES_INDEX, 0);
        context.setVariable(Variables.INDEX_VARIABLE_NAME, Variables.MODULES_INDEX.getName());

        context.setVariable(Variables.REBUILD_APP_ENV, true);
        context.setVariable(Variables.SHOULD_UPLOAD_APPLICATION_CONTENT, true);
        context.setVariable(Variables.EXECUTE_ONE_OFF_TASKS, true);

        context.setVariable(Variables.MODULES_TO_DEPLOY, modulesToDeploy);

        context.setVariable(Variables.DELETE_IDLE_URIS, false);
        blueGreenVariablesSetter.set(context);

        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_PREPARING_MODULES_DEPLOYMENT;
    }

    protected List<Module> getModulesToDeploy(ProcessContext context) {
        return context.getVariable(Variables.ALL_MODULES_TO_DEPLOY);
    }

}
