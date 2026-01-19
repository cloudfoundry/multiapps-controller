package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.List;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudMetadata;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableStaging;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Staging;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.cf.util.ModulesCloudModelBuilderContentCalculator;
import org.cloudfoundry.multiapps.controller.core.cf.v2.ApplicationCloudModelBuilder;
import org.cloudfoundry.multiapps.controller.core.cf.v2.ServiceKeysCloudModelBuilder;
import org.cloudfoundry.multiapps.controller.core.helpers.ModuleToDeployHelper;
import org.cloudfoundry.multiapps.controller.process.util.ApplicationEnvironmentCalculator;
import org.cloudfoundry.multiapps.controller.process.util.DeprecatedBuildpackChecker;
import org.cloudfoundry.multiapps.controller.process.util.ProcessTypeParser;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class PrepareToStopDependentModuleStepTest extends SyncFlowableStepTest<PrepareToStopDependentModuleStep> {

    @Mock
    private ModuleToDeployHelper moduleToDeployHelper;

    @Mock
    protected ApplicationCloudModelBuilder applicationCloudModelBuilder;
    @Mock
    protected ModulesCloudModelBuilderContentCalculator modulesCloudModelBuilderContentCalculator;
    @Mock
    protected ServiceKeysCloudModelBuilder serviceKeysCloudModelBuilder;

    @Mock
    private ProcessTypeParser processTypeParser;

    @Mock
    private DeprecatedBuildpackChecker deprecatedBuildpackChecker;

    @Mock
    private ApplicationEnvironmentCalculator applicationEnvironmentCalculator;

    @Override
    protected PrepareToStopDependentModuleStep createStep() {
        return new PrepareToStopDependentModuleStepTest.PrepareToStopDependentModuleStepMock(moduleToDeployHelper,
                                                                                             applicationEnvironmentCalculator);
    }

    @Test
    void testPrepareToStopDependentModuleStep() {
        Module module = Module.createV3()
                              .setName("test-module");
        setUpMocks(module);
        step.execute(execution);
        assertEquals(context.getVariable(Variables.APP_TO_PROCESS)
                            .getName(), module.getName());
        assertStepFinishedSuccessfully();
    }

    @Test
    void testPrepareToStopDependentModuleStepIdleURIs() {
        Module module = Module.createV3()
                              .setName("test-module");
        setUpMocks(module);
        context.setVariable(Variables.USE_IDLE_URIS, true);
        step.execute(execution);
        assertEquals(context.getVariable(Variables.APP_TO_PROCESS)
                            .getName(), module.getName());
        assertStepFinishedSuccessfully();
    }
    
    private void setUpMocks(Module module) {
        DeploymentDescriptor completeDeploymentDescriptor = DeploymentDescriptor.createV3();
        completeDeploymentDescriptor.setModules(List.of(module));
        context.setVariable(Variables.DEPENDENT_MODULES_TO_STOP, List.of(module));
        context.setVariable(Variables.APPS_TO_STOP_INDEX, 0);
        context.setVariable(Variables.MTA_MAJOR_SCHEMA_VERSION, 3);
        ImmutableCloudApplicationExtended applicationExtended = ImmutableCloudApplicationExtended.builder()
                                                                                                 .name("test-module")
                                                                                                 .metadata(ImmutableCloudMetadata.of(
                                                                                                     UUID.randomUUID()
                                                                                                 ))
                                                                                                 .staging(createStaging(
                                                                                                     true))
                                                                                                 .build();
        context.setVariable(Variables.APP_TO_PROCESS, applicationExtended);
        context.setVariable(Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR, completeDeploymentDescriptor);
        context.setVariable(Variables.DEPLOYMENT_DESCRIPTOR, completeDeploymentDescriptor);
        when(applicationCloudModelBuilder.build(Mockito.any(), Mockito.any())).thenReturn(applicationExtended);
    }

    private Staging createStaging(boolean isReadinessHealthCheckEnabled) {
        return ImmutableStaging.builder()
                               .isReadinessHealthCheckEnabled(isReadinessHealthCheckEnabled)
                               .readinessHealthCheckType("http")
                               .build();
    }

    private class PrepareToStopDependentModuleStepMock extends PrepareToStopDependentModuleStep {

        public PrepareToStopDependentModuleStepMock(ModuleToDeployHelper moduleToDeployHelper,
                                                    ApplicationEnvironmentCalculator applicationEnvironmentCalculator) {
            super(moduleToDeployHelper, applicationEnvironmentCalculator);
        }

        @Override
        protected ApplicationCloudModelBuilder getApplicationCloudModelBuilder(ProcessContext context) {
            return applicationCloudModelBuilder;
        }

    }
}