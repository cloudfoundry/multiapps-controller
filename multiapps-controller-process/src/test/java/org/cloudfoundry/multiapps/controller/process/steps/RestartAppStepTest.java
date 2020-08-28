package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.List;

import org.cloudfoundry.client.lib.StartingInfo;
import org.cloudfoundry.client.lib.domain.CloudApplication.State;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.api.model.ProcessType;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.processor.MtaMetadataParser;
import org.cloudfoundry.multiapps.controller.core.model.HookPhase;
import org.cloudfoundry.multiapps.controller.process.util.HooksExecutor;
import org.cloudfoundry.multiapps.controller.process.util.HooksPhaseBuilder;
import org.cloudfoundry.multiapps.controller.process.util.HooksPhaseGetter;
import org.cloudfoundry.multiapps.controller.process.util.ProcessTypeParser;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

class RestartAppStepTest extends SyncFlowableStepTest<RestartAppStep> {

    private static final String APP_NAME = "foo";
    @Mock
    private MtaMetadataParser mtaMetadataParser;
    @Mock
    private HooksExecutor hooksExecutor;
    @Mock
    private ProcessTypeParser processTypeParser;
    @Mock
    private HooksPhaseGetter hooksPhaseGetter;
    @Mock
    private HooksPhaseBuilder hooksPhaseBuilder;

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
    void testGetHookPhasesBeforeStep() {
        Mockito.when(hooksPhaseBuilder.buildHookPhases(Collections.singletonList(HookPhase.BEFORE_START), context))
               .thenReturn(Collections.singletonList(HookPhase.BLUE_GREEN_APPLICATION_BEFORE_START_LIVE));
        List<HookPhase> expectedHooks = Collections.singletonList(HookPhase.BLUE_GREEN_APPLICATION_BEFORE_START_LIVE);
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
