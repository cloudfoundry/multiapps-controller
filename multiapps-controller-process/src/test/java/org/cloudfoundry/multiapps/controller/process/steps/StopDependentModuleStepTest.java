package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.List;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudApplication;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudMetadata;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.processor.MtaMetadataParser;
import org.cloudfoundry.multiapps.controller.core.model.BlueGreenApplicationNameSuffix;
import org.cloudfoundry.multiapps.controller.process.util.ApplicationWaitAfterStopHandler;
import org.cloudfoundry.multiapps.controller.process.util.HooksPhaseGetter;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StopDependentModuleStepTest extends SyncFlowableStepTest<StopDependentModuleStep> {

    private static String appName = "test-name";

    private static String idleAppName = appName + BlueGreenApplicationNameSuffix.IDLE.asSuffix();

    @Mock
    private ApplicationWaitAfterStopHandler waitAfterStopHandler;

    @Mock
    private MtaMetadataParser mtaMetadataParser;

    @Mock
    private HooksPhaseGetter hooksPhaseGetter;

    @Test
    void testStopDependentModuleStep() {
        org.cloudfoundry.multiapps.mta.model.Module module = org.cloudfoundry.multiapps.mta.model.Module.createV3()
                                                                                                        .setName(appName);
        setUpMocks(module, CloudApplication.State.STARTED);
        step.execute(execution);
        assertStepFinishedSuccessfully();
        verify(client, times(1)).stopApplication(idleAppName);
    }

    @Test
    void testStopDependentModuleStepStopped() {
        org.cloudfoundry.multiapps.mta.model.Module module = org.cloudfoundry.multiapps.mta.model.Module.createV3()
                                                                                                        .setName(appName);
        setUpMocks(module, CloudApplication.State.STOPPED);
        step.execute(execution);
        assertStepFinishedSuccessfully();
        verify(client, times(0)).stopApplication(idleAppName);
    }

    private void setUpMocks(Module module, CloudApplication.State state) {
        DeploymentDescriptor completeDeploymentDescriptor = DeploymentDescriptor.createV3();
        completeDeploymentDescriptor.setModules(List.of(module));
        ImmutableCloudApplicationExtended applicationExtended = ImmutableCloudApplicationExtended.builder()
                                                                                                 .name(appName)
                                                                                                 .metadata(ImmutableCloudMetadata.of(
                                                                                                     UUID.randomUUID()
                                                                                                 ))
                                                                                                 .state(state)
                                                                                                 .build();
        when(client.getApplication(idleAppName)).thenReturn(applicationExtended);
        context.setVariable(Variables.MODULE_TO_DEPLOY, module);
        context.setVariable(Variables.DEPENDENT_MODULES_TO_STOP, List.of(module));
        context.setVariable(Variables.APPS_TO_STOP_INDEX, 0);
    }

    @Override
    protected StopDependentModuleStep createStep() {
        return new StopDependentModuleStep(waitAfterStopHandler);
    }
}