package com.sap.cloud.lm.sl.cf.process.steps;

import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import com.sap.cloud.lm.sl.cf.core.cf.metadata.processor.EnvMtaMetadataParser;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.processor.MtaMetadataParser;
import com.sap.cloud.lm.sl.cf.core.model.HookPhase;
import com.sap.cloud.lm.sl.cf.process.util.HooksExecutor;
import com.sap.cloud.lm.sl.cf.process.util.HooksPhaseGetter;

class StopApplicationUndeploymentStepTest extends UndeployAppStepTest {

    @Mock
    private MtaMetadataParser mtaMetadataParser;
    @Mock
    private EnvMtaMetadataParser envMtaMetadataParser;
    @Mock
    private HooksPhaseGetter hooksPhaseGetter;
    @Mock
    private HooksExecutor hooksExecutor;

    @Test
    void testGetHookPhaseBefore() {
        List<HookPhase> expectedHooks = Collections.singletonList(HookPhase.APPLICATION_BEFORE_STOP_LIVE);
        List<HookPhase> hookPhasesBeforeStep = ((StopApplicationUndeploymentStep) step).getHookPhasesBeforeStep(context);
        Assert.assertEquals(expectedHooks, hookPhasesBeforeStep);
    }

    @Test
    void testGetHookPhaseAfter() {
        List<HookPhase> expectedHooks = Collections.singletonList(HookPhase.APPLICATION_AFTER_STOP_LIVE);
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
