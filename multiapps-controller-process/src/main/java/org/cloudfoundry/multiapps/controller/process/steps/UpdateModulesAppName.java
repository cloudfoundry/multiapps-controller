package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.List;
import java.util.Map;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.core.model.BlueGreenApplicationNameSuffix;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@Named("updateModulesAppNames")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UpdateModulesAppName extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        DeploymentDescriptor unresolvedDeploymentDescriptor = context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR_WITH_SYSTEM_PARAMETERS);
        List<Module> modules = unresolvedDeploymentDescriptor.getModules();
        modules.forEach(this::updateModuleAppName);
        context.setVariable(Variables.DEPLOYMENT_DESCRIPTOR_WITH_SYSTEM_PARAMETERS, unresolvedDeploymentDescriptor);
        return StepPhase.DONE;
    }

    private void updateModuleAppName(Module module) {
        Map<String, Object> parameters = module.getParameters();
        String appNameWithSuffix = (String) parameters.get(SupportedParameters.APP_NAME);
        String appNameWithoutSuffix = BlueGreenApplicationNameSuffix.removeSuffix(appNameWithSuffix);
        parameters.put(SupportedParameters.APP_NAME, appNameWithoutSuffix);
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_UPDATING_MODULE_PARAMETERS;
    }

}
