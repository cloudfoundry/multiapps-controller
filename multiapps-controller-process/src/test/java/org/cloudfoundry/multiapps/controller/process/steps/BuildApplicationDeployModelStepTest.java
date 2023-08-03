package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.cloudfoundry.multiapps.controller.core.helpers.ModuleToDeployHelper;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.process.util.ApplicationEnvironmentCalculator;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class BuildApplicationDeployModelStepTest extends SyncFlowableStepTest<BuildApplicationDeployModelStep> {

    @Mock
    private ModuleToDeployHelper moduleToDeployHelper;
    @Mock
    private ApplicationEnvironmentCalculator applicationEnvironmentCalculator;

    @Test
    void testModuleResolutionAsyncServiceBindings() {
        Module module = Module.createV3()
                              .setName("test-module");
        setUpMocks(module);
        step.execute(execution);
        assertTrue(context.getVariable(Variables.SHOULD_UNBIND_BIND_SERVICES_IN_PARALLEL));
        assertStepFinishedSuccessfully();
    }

    @Test
    void testModuleResolutionSyncServiceBindings() {
        Module module = Module.createV3()
                              .setName("test-module")
                              .setParameters(Map.of(SupportedParameters.ENABLE_PARALLEL_SERVICE_BINDINGS, false));
        setUpMocks(module);
        step.execute(execution);
        assertFalse(context.getVariable(Variables.SHOULD_UNBIND_BIND_SERVICES_IN_PARALLEL));
        assertStepFinishedSuccessfully();
    }

    private void setUpMocks(Module module) {
        DeploymentDescriptor completeDeploymentDescriptor = DeploymentDescriptor.createV3();
        completeDeploymentDescriptor.setModules(List.of(module));
        context.setVariable(Variables.MODULE_TO_DEPLOY, module);
        context.setVariable(Variables.MTA_MAJOR_SCHEMA_VERSION, 3);
        context.setVariable(Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR, completeDeploymentDescriptor);
        when(moduleToDeployHelper.isApplication(any())).thenReturn(true);
    }

    @Override
    protected BuildApplicationDeployModelStep createStep() {
        return new BuildApplicationDeployModelStep();
    }
}
