package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Map;

import org.cloudfoundry.multiapps.controller.core.model.BlueGreenApplicationNameSuffix;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UpdateModulesAppNameTest extends SyncFlowableStepTest<UpdateModulesAppName> {

    @BeforeEach
    void setUp() {
        prepareContext();
    }

    @Test
    void testUpdatingAppNames() {
        step.execute(execution);
        DeploymentDescriptor deploymentDescriptor = context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR_WITH_SYSTEM_PARAMETERS);
        validateModulesDoNotHaveUnresolvedAppNameParameter(deploymentDescriptor.getModules());
        assertStepFinishedSuccessfully();
    }

    private void prepareContext() {
        DeploymentDescriptor deploymentDescriptor = getDeploymentDescriptor();
        context.setVariable(Variables.DEPLOYMENT_DESCRIPTOR_WITH_SYSTEM_PARAMETERS, deploymentDescriptor);
    }

    private DeploymentDescriptor getDeploymentDescriptor() {
        DeploymentDescriptor deploymentDescriptor = DeploymentDescriptor.createV3();
        List<Module> modules = getModules();
        deploymentDescriptor.setModules(modules);
        return deploymentDescriptor;
    }

    private List<Module> getModules() {
        Module firstModule = Module.createV3();
        firstModule.setParameters(Map.of(SupportedParameters.APP_NAME, "first-idle"));
        Module secondModule = Module.createV3();
        secondModule.setParameters(Map.of(SupportedParameters.APP_NAME, "second-idle"));
        return List.of(firstModule, secondModule);
    }

    private void validateModulesDoNotHaveUnresolvedAppNameParameter(List<Module> modules) {
        boolean moduleNamesEndsWithSuffix = modules.stream()
                                                   .map(Module::getParameters)
                                                   .anyMatch(this::doesAppNameEndWithIdleSuffix);
        assertFalse(moduleNamesEndsWithSuffix, "One or more modules names end with idle suffix");
    }

    private boolean doesAppNameEndWithIdleSuffix(Map<String, Object> parameters) {
        String appName = (String) parameters.get(SupportedParameters.APP_NAME);
        return appName.endsWith(BlueGreenApplicationNameSuffix.IDLE.asSuffix());
    }

    @Override
    protected UpdateModulesAppName createStep() {
        return new UpdateModulesAppName();
    }
}
