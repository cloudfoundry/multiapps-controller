package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil.Expectation;
import com.sap.cloud.lm.sl.mta.handlers.v2.ConfigurationParser;
import com.sap.cloud.lm.sl.mta.model.v2.Platform;

@RunWith(Parameterized.class)
public class DetectTargetStepTest extends SyncFlowableStepTest<DetectTargetStep> {

    private Expectation expectation;
    private StepInput input;
    public DetectTargetStepTest(StepInput input, Expectation expectation) {
        this.expectation = expectation;
        this.input = input;
    }

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) Platform and its platform types exist (version 2):
            {
                new StepInput("platform-v2.json", "initial", "initial", 2),
                new Expectation(Expectation.Type.RESOURCE, "parsed-platform-v2.json"),
            },
// @formatter:on
        });
    }

    @Test
    public void testExecute() throws Exception {
        step.execute(context);

        assertStepFinishedSuccessfully();

        TestUtil.test(() -> StepsUtil.getPlatform(context), expectation, getClass());
    }

    @Before
    public void setUp() throws Exception {
        context.setVariable(Constants.VAR_SPACE, input.space);
        context.setVariable(Constants.VAR_ORG, input.org);
        context.setVariable(Constants.VAR_MTA_MAJOR_SCHEMA_VERSION, input.majorSchemaVersion);
        Platform platform = loadPlatform();
        Mockito.when(configuration.getPlatform(Mockito.any(), Mockito.anyInt()))
               .thenReturn(platform);
    }

    private Platform loadPlatform() {
        ConfigurationParser configurationParser = new HandlerFactory(input.majorSchemaVersion).getConfigurationParser();
        return StepsTestUtil.loadPlatform(configurationParser, input.platformLocation, getClass());
    }

    @Override
    protected DetectTargetStep createStep() {
        return new DetectTargetStep();
    }

    private static class StepInput {

        public String platformLocation;
        public String org;
        public String space;
        public int majorSchemaVersion;

        public StepInput(String platformLocation, String org, String space, int majorSchemaVersion) {
            this.platformLocation = platformLocation;
            this.org = org;
            this.space = space;
            this.majorSchemaVersion = majorSchemaVersion;
        }

    }

}
