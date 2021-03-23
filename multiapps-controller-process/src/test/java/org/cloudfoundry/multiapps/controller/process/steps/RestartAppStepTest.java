package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

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

import com.sap.cloudfoundry.client.facade.domain.CloudApplication.State;

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
        Mockito.when(processTypeParser.getProcessTypeFromProcessVariable(context.getExecution()))
               .thenReturn(ProcessType.DEPLOY);
        CloudApplicationExtended app = createApplication(APP_NAME, State.STOPPED);
        prepareContextAndClient(app);
        step.execute(execution);
        assertStepFinishedSuccessfully();

        Mockito.verify(client, Mockito.never())
               .stopApplication(APP_NAME);
        Mockito.verify(client, Mockito.times(1))
               .startApplication(APP_NAME);
    }

    @Override
    protected void assertStepFinishedSuccessfully() {
        assertEquals(StepPhase.POLL.toString(), getExecutionStatus());
    }

    @Test
    void testExecuteWhenAppIsStarted() {
        Mockito.when(processTypeParser.getProcessTypeFromProcessVariable(context.getExecution()))
               .thenReturn(ProcessType.DEPLOY);
        CloudApplicationExtended app = createApplication(APP_NAME, State.STARTED);
        prepareContextAndClient(app);
        step.execute(execution);
        assertStepFinishedSuccessfully();

        Mockito.verify(client)
               .stopApplication(APP_NAME);
        Mockito.verify(client)
               .startApplication(APP_NAME);
    }

    @Test
    void testGetHookPhasesBeforeStep() {
        Mockito.when(hooksPhaseBuilder.buildHookPhases(List.of(HookPhase.BEFORE_START), context))
               .thenReturn(List.of(HookPhase.BLUE_GREEN_APPLICATION_BEFORE_START_LIVE));
        List<HookPhase> expectedHooks = List.of(HookPhase.BLUE_GREEN_APPLICATION_BEFORE_START_LIVE);
        List<HookPhase> hookPhasesBeforeStep = step.getHookPhasesBeforeStep(context);
        assertEquals(expectedHooks, hookPhasesBeforeStep);
    }

    private CloudApplicationExtended createApplication(String name, State state) {
        return ImmutableCloudApplicationExtended.builder()
                                                .name(name)
                                                .state(state)
                                                .build();
    }

    private void prepareContextAndClient(CloudApplicationExtended app) {
        Mockito.when(client.getApplication(APP_NAME))
               .thenReturn(app);
        context.setVariable(Variables.APP_TO_PROCESS, app);
    }

    @Override
    protected RestartAppStep createStep() {
        return new RestartAppStep();
    }

}
