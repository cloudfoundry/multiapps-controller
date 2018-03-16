package com.sap.cloud.lm.sl.cf.process.steps;

import static com.sap.cloud.lm.sl.cf.process.steps.StepsTestUtil.loadPlatforms;
import static com.sap.cloud.lm.sl.cf.process.steps.StepsTestUtil.loadTargets;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.mta.handlers.v1_0.ConfigurationParser;

@RunWith(Parameterized.class)
public class DetectTargetStepTest extends SyncActivitiStepTest<DetectTargetStep> {

    private static class StepInput {

        public String platformsLocation;
        public String targetName;
        public String platformTypesLocation;
        public int majorSchemaVersion;
        public int minorSchemaVersion;
        public String org;
        public String space;

        public StepInput(String platformName, String platformsLocation, String platformTypesLocation, String org, String space,
            int majorSchemaVersion, int minorSchemaVersion) {
            this.platformsLocation = platformsLocation;
            this.targetName = platformName;
            this.platformTypesLocation = platformTypesLocation;
            this.majorSchemaVersion = majorSchemaVersion;
            this.minorSchemaVersion = minorSchemaVersion;
            this.org = org;
            this.space = space;
        }

    }

    private static interface StepOutput {

    }

    private static class FailedStepOutput implements StepOutput {

        public String expectedExceptionMessage;

        public FailedStepOutput(String expectedExceptionMessage) {
            this.expectedExceptionMessage = expectedExceptionMessage;
        }

    }

    private static class SuccessfulStepOutput implements StepOutput {

        public String platformName;
        public String platformLocation;
        public String platformTypeLocation;

        public SuccessfulStepOutput(String platformName, String platformLocation, String platformTypeLocation) {
            this.platformName = platformName;
            this.platformLocation = platformLocation;
            this.platformTypeLocation = platformTypeLocation;
        }

    }

    private StepOutput output;
    private StepInput input;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    public DetectTargetStepTest(StepInput input, StepOutput output) {
        this.output = output;
        this.input = input;
    }

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) Platform and its platform types exist (version 1):
            {
                new StepInput("ZE_PLATFORM", "platforms-01.json", "platform-types-01.json", "initial", "initial", 1, 0), new SuccessfulStepOutput("ZE_PLATFORM", "R:platform-01.json.json", "R:platform-type-01.json.json"),
            },
            // (1) Platform and its platform types exist (version 2):
            {
                new StepInput("ZE_PLATFORM", "platforms-02.json", "platform-types-02.json", "initial", "initial", 2, 0), new SuccessfulStepOutput("ZE_PLATFORM", "R:platform-02.json.json", "R:platform-type-02.json.json"),
            },
            // (2) No platform types are configured:
            {
                new StepInput("ZE_PLATFORM", "platforms-01.json", "platform-types-03.json", "initial", "initial", 1, 0), new FailedStepOutput("No platforms configured"),
            },
            // (3) Platform name parameter is missing (a default one should be constructed):
            {
                new StepInput(null, "platforms-01.json", "platform-types-01.json", "initial", "initial", 1, 0), new SuccessfulStepOutput("initial initial", "R:platform-03.json.json", "R:platform-type-01.json.json"),
            },
            // (4) Platform name parameter is missing (a default one should be constructed):
            {
                new StepInput(""  , "platforms-01.json", "platform-types-01.json", "initial", "initial", 1, 0), new SuccessfulStepOutput("initial initial", "R:platform-03.json.json", "R:platform-type-01.json.json"),
            },
            // (5) Platform organization does not match the organization from the process's context:
            {
                new StepInput("ZE_PLATFORM", "platforms-01.json", "platform-types-01.json", "initi@l", "initial", 1, 0), new FailedStepOutput("Deploy target organization \"initial\" does not match the organization \"initi@l\" specified in the URL"),
            },
            // (6) Platform space does not match the space from the process's context:
            {
                new StepInput("ZE_PLATFORM", "platforms-01.json", "platform-types-01.json", "initial", "initi@l", 1, 0), new FailedStepOutput("Deploy target space \"initial\" does not match the space \"initi@l\" specified in the URL"),
            },
// @formatter:on
        });
    }

    @Test
    public void testExecute() throws Exception {
        step.execute(context);

        assertStepFinishedSuccessfully();

        assertEquals(((SuccessfulStepOutput) output).platformName, context.getVariable(Constants.PARAM_TARGET_NAME));

        TestUtil.test(() -> {

            return StepsUtil.getTarget(context);

        }, ((SuccessfulStepOutput) output).platformLocation, getClass());

        TestUtil.test(() -> {

            return StepsUtil.getPlatform(context);

        }, ((SuccessfulStepOutput) output).platformTypeLocation, getClass());
    }

    @Before
    public void setUp() throws Exception {
        loadParameters();
        prepareContext();
    }

    private void loadParameters() {
        if (output instanceof FailedStepOutput) {
            expectedException.expectMessage(((FailedStepOutput) output).expectedExceptionMessage);
        }
    }

    private void prepareContext() {
        ConfigurationParser configurationParser = new HandlerFactory(input.majorSchemaVersion).getConfigurationParser();

        context.setVariable(Constants.VAR_SPACE, input.space);
        context.setVariable(Constants.VAR_ORG, input.org);

        context.setVariable(Constants.VAR_MTA_MAJOR_SCHEMA_VERSION, input.majorSchemaVersion);
        context.setVariable(Constants.VAR_MTA_MINOR_SCHEMA_VERSION, input.minorSchemaVersion);
        context.setVariable(Constants.PARAM_TARGET_NAME, input.targetName);

        Mockito.when(configuration.getPlatforms(Mockito.any(), Mockito.anyInt()))
            .thenReturn(loadPlatforms(configurationParser, input.platformTypesLocation, getClass()));
        step.targetsSupplier = (factory) -> loadTargets(configurationParser, input.platformsLocation, getClass());
    }

    @Override
    protected DetectTargetStep createStep() {
        return new DetectTargetStep();
    }

}
