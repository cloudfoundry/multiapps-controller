package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.cloudfoundry.client.lib.StartingInfo;
import org.cloudfoundry.client.lib.domain.CloudApplication.State;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ImmutableCloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.processor.EnvMtaMetadataParser;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.processor.MtaMetadataParser;
import com.sap.cloud.lm.sl.cf.core.model.HookPhase;
import com.sap.cloud.lm.sl.cf.core.model.Phase;
import com.sap.cloud.lm.sl.cf.process.util.HooksExecutor;
import com.sap.cloud.lm.sl.cf.process.util.HooksPhaseGetter;
import com.sap.cloud.lm.sl.cf.process.util.ProcessTypeParser;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.cf.web.api.model.ProcessType;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

class RestartAppStepTest extends SyncFlowableStepTest<RestartAppStep> {

    private static final String APP_NAME = "foo";
    @Mock
    private MtaMetadataParser mtaMetadataParser;
    @Mock
    private EnvMtaMetadataParser envMtaMetadataParser;
    @Mock
    private HooksExecutor hooksExecutor;
    @Mock
    private ProcessTypeParser processTypeParser;
    @Mock
    private HooksPhaseGetter hooksPhaseGetter;

    @BeforeEach
    public void setUp() {
        context.setVariable(Variables.MODULES_INDEX, 0);
    }

    @Test
    void testExecuteWhenAppIsStopped() {
        Mockito.when(processTypeParser.getProcessType(context.getExecution()))
               .thenReturn(ProcessType.DEPLOY);
        CloudApplicationExtended app = createApplication(APP_NAME, State.STOPPED);
        StartingInfo startingInfo = new StartingInfo("dummyStagingFile");
        prepareContextAndClient(app, startingInfo);

        step.execute(execution);
        assertStepFinishedSuccessfully();

        Mockito.verify(client, Mockito.never())
               .stopApplication(APP_NAME);
        Mockito.verify(client, Mockito.times(1))
               .startApplication(APP_NAME);

        assertEquals(JsonUtil.toJson(startingInfo), JsonUtil.toJson(context.getVariable(Variables.STARTING_INFO)));
    }

    @Override
    protected void assertStepFinishedSuccessfully() {
        assertEquals(StepPhase.POLL.toString(), getExecutionStatus());
    }

    @Test
    void testExecuteWhenAppIsStarted() {
        Mockito.when(processTypeParser.getProcessType(context.getExecution()))
               .thenReturn(ProcessType.DEPLOY);
        CloudApplicationExtended app = createApplication(APP_NAME, State.STARTED);
        StartingInfo startingInfo = new StartingInfo("dummyStagingFile");
        prepareContextAndClient(app, startingInfo);

        step.execute(execution);
        assertStepFinishedSuccessfully();

        Mockito.verify(client)
               .stopApplication(APP_NAME);
        Mockito.verify(client)
               .startApplication(APP_NAME);

        assertEquals(JsonUtil.toJson(startingInfo), JsonUtil.toJson(context.getVariable(Variables.STARTING_INFO)));
    }

    @Test
    void testGetHookPhaseBeforeBlueGreenProcess() {
        Mockito.when(processTypeParser.getProcessType(context.getExecution()))
               .thenReturn(ProcessType.BLUE_GREEN_DEPLOY);
        List<HookPhase> expectedHooks = Arrays.asList(HookPhase.APPLICATION_BEFORE_START_IDLE, HookPhase.APPLICATION_BEFORE_START);
        List<HookPhase> hookPhasesBeforeStep = step.getHookPhasesBeforeStep(context);
        assertEquals(expectedHooks, hookPhasesBeforeStep);
    }

    @Test
    void testGetHookPhaseBeforeBlueGreenProcessResumePhase() {
        Mockito.when(processTypeParser.getProcessType(context.getExecution()))
               .thenReturn(ProcessType.BLUE_GREEN_DEPLOY);
        context.setVariable(Variables.PHASE, Phase.AFTER_RESUME);
        List<HookPhase> expectedHooks = Arrays.asList(HookPhase.APPLICATION_BEFORE_START_LIVE, HookPhase.APPLICATION_BEFORE_START);
        List<HookPhase> hookPhasesBeforeStep = step.getHookPhasesBeforeStep(context);
        assertEquals(expectedHooks, hookPhasesBeforeStep);
    }

    @Test
    void testGetHookPhaseBeforeDeployProcess() {
        Mockito.when(processTypeParser.getProcessType(context.getExecution()))
               .thenReturn(ProcessType.DEPLOY);
        List<HookPhase> expectedHooks = Arrays.asList(HookPhase.APPLICATION_BEFORE_START_LIVE, HookPhase.APPLICATION_BEFORE_START);
        List<HookPhase> hookPhasesBeforeStep = step.getHookPhasesBeforeStep(context);
        assertEquals(expectedHooks, hookPhasesBeforeStep);
    }

    private CloudApplicationExtended createApplication(String name, State state) {
        return ImmutableCloudApplicationExtended.builder()
                                                .name(name)
                                                .state(state)
                                                .build();
    }

    private void prepareContextAndClient(CloudApplicationExtended app, StartingInfo startingInfo) {
        Mockito.when(client.getApplication(APP_NAME))
               .thenReturn(app);
        Mockito.when(client.startApplication(APP_NAME))
               .thenReturn(startingInfo);
        context.setVariable(Variables.APP_TO_PROCESS, app);
    }

    @Override
    protected RestartAppStep createStep() {
        return new RestartAppStep();
    }

}
