package org.cloudfoundry.multiapps.controller.process.steps;

import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.processor.EnvMtaMetadataParser;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.processor.MtaMetadataParser;
import org.cloudfoundry.multiapps.controller.core.model.HookPhase;
import org.cloudfoundry.multiapps.controller.process.util.HooksPhaseBuilder;
import org.cloudfoundry.multiapps.controller.process.util.HooksPhaseGetter;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

class StopApplicationUndeploymentStepTest extends UndeployAppStepTest {

    @Mock
    private MtaMetadataParser mtaMetadataParser;
    @Mock
    private EnvMtaMetadataParser envMtaMetadataParser;
    @Mock
    private HooksPhaseGetter hooksPhaseGetter;
    @Mock
    private HooksPhaseBuilder hooksPhaseBuilder;

    @Test
    void testGetHookPhaseBefore() {
        Mockito.when(hooksPhaseBuilder.buildHookPhases(Arrays.asList(HookPhase.BEFORE_STOP, HookPhase.APPLICATION_BEFORE_STOP_LIVE),
                                                       context))
               .thenReturn(Arrays.asList(HookPhase.BLUE_GREEN_APPLICATION_BEFORE_STOP_LIVE, HookPhase.APPLICATION_BEFORE_STOP_LIVE));
        List<HookPhase> expectedHooks = Arrays.asList(HookPhase.BLUE_GREEN_APPLICATION_BEFORE_STOP_LIVE,
                                                      HookPhase.APPLICATION_BEFORE_STOP_LIVE);
        List<HookPhase> hookPhasesBeforeStep = ((StopApplicationUndeploymentStep) step).getHookPhasesBeforeStep(context);
        Assert.assertEquals(expectedHooks, hookPhasesBeforeStep);
    }

    @Test
    void testGetHookPhaseAfter() {
        Mockito.when(hooksPhaseBuilder.buildHookPhases(Arrays.asList(HookPhase.AFTER_STOP, HookPhase.APPLICATION_AFTER_STOP_LIVE), context))
               .thenReturn(Arrays.asList(HookPhase.BLUE_GREEN_APPLICATION_AFTER_STOP_LIVE, HookPhase.APPLICATION_AFTER_STOP_LIVE));
        List<HookPhase> expectedHooks = Arrays.asList(HookPhase.BLUE_GREEN_APPLICATION_AFTER_STOP_LIVE,
                                                      HookPhase.APPLICATION_AFTER_STOP_LIVE);
        List<HookPhase> hookPhasesBeforeStep = ((StopApplicationUndeploymentStep) step).getHookPhasesAfterStep(context);
        Assert.assertEquals(expectedHooks, hookPhasesBeforeStep);
    }

    @Override
    protected void performValidation(CloudApplication cloudApplication) {
        verify(client).stopApplication(cloudApplication.getName());
    }

    @Override
    protected UndeployAppStep createStep() {
        return new StopApplicationUndeploymentStep();
    }

    @Override
    protected void performAfterUndeploymentValidation() {
    }

}
