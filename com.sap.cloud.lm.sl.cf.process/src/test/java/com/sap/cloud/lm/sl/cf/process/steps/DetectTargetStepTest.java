package com.sap.cloud.lm.sl.cf.process.steps;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil.Expectation;
import com.sap.cloud.lm.sl.mta.model.Platform;

public class DetectTargetStepTest extends SyncFlowableStepTest<DetectTargetStep> {

    @Before
    public void setUp() throws Exception {
        context.setVariable(Constants.VAR_SPACE, "initial");
        context.setVariable(Constants.VAR_ORG, "initial");
        Platform platform = StepsTestUtil.loadPlatform("platform.json", getClass());
        Mockito.when(configuration.getPlatform())
            .thenReturn(platform);
    }

    @Test
    public void testExecute() throws Exception {
        step.execute(context);

        assertStepFinishedSuccessfully();

        Expectation expectation = new Expectation(Expectation.Type.RESOURCE, "parsed-platform.json");
        TestUtil.test(() -> StepsUtil.getPlatform(context), expectation, getClass());
    }

    @Override
    protected DetectTargetStep createStep() {
        return new DetectTargetStep();
    }

}
