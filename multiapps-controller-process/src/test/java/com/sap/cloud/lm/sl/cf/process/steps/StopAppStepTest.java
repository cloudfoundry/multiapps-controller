package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.cloudfoundry.client.lib.domain.CloudApplication.State;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ImmutableCloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.processor.EnvMtaMetadataParser;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.processor.MtaMetadataParser;
import com.sap.cloud.lm.sl.cf.core.model.HookPhase;
import com.sap.cloud.lm.sl.cf.process.steps.ScaleAppStepTest.SimpleApplication;
import com.sap.cloud.lm.sl.cf.process.util.HooksExecutor;
import com.sap.cloud.lm.sl.cf.process.util.HooksPhaseBuilder;
import com.sap.cloud.lm.sl.cf.process.util.HooksPhaseGetter;
import com.sap.cloud.lm.sl.cf.process.util.ProcessTypeParser;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.cf.web.api.model.ProcessType;

class StopAppStepTest extends SyncFlowableStepTest<StopAppStep> {

    private SimpleApplicationWithState application;
    private SimpleApplicationWithState existingApplication;

    @Mock
    private MtaMetadataParser mtaMetadataParser;
    @Mock
    private EnvMtaMetadataParser envMtaMetadataParser;
    @Mock
    private HooksPhaseGetter hooksPhaseGetter;
    @Mock
    private HooksExecutor hooksExecutor;
    @Mock
    private HooksPhaseBuilder hooksPhaseBuilder;
    @Mock
    private ProcessTypeParser processTypeParser;

    private boolean shouldBeStopped;

    static Stream<Arguments> testExecute() {
        return Stream.of(Arguments.of(new SimpleApplicationWithState("test-app-1", 0, State.STARTED),
                                      new SimpleApplicationWithState("test-app-1", 0, State.STARTED)),
                         Arguments.of(new SimpleApplicationWithState("test-app-1", 0, State.STARTED),
                                      new SimpleApplicationWithState("test-app-1", 0, State.STOPPED)),
                         Arguments.of(new SimpleApplicationWithState("test-app-1", 0, State.STOPPED),
                                      new SimpleApplicationWithState("test-app-1", 0, State.STOPPED)),
                         Arguments.of(new SimpleApplicationWithState("test-app-1", 0, State.STOPPED),
                                      new SimpleApplicationWithState("test-app-1", 0, State.STARTED)));
    }

    @MethodSource
    @ParameterizedTest
    void testExecute(SimpleApplicationWithState application, SimpleApplicationWithState existingApplication) {
        this.application = application;
        this.existingApplication = existingApplication;
        prepareContext();
        determineActionForApplication();

        Mockito.when(processTypeParser.getProcessType(Mockito.any()))
               .thenReturn(ProcessType.DEPLOY);
        step.execute(execution);

        assertStepFinishedSuccessfully();

        validateStoppedApplications();
    }

    private void determineActionForApplication() {
        shouldBeStopped = existingApplication.state != State.STOPPED;
    }

    private void prepareContext() {
        context.setVariable(Variables.MODULES_INDEX, 0);
        StepsTestUtil.mockApplicationsToDeploy(toCloudApplication(), execution);
        context.setVariable(Variables.EXISTING_APP, (existingApplication != null) ? existingApplication.toCloudApplication() : null);
    }

    List<CloudApplicationExtended> toCloudApplication() {
        return Collections.singletonList(application.toCloudApplication());
    }

    private void validateStoppedApplications() {
        String appName = application.name;
        if (shouldBeStopped) {
            Mockito.verify(client)
                   .stopApplication(appName);
        } else {
            Mockito.verify(client, Mockito.times(0))
                   .stopApplication(appName);
        }
    }

    @Test
    void testGetHooksBeforeStep() {
        Mockito.when(hooksPhaseBuilder.buildHookPhases(Arrays.asList(HookPhase.APPLICATION_BEFORE_STOP_LIVE, HookPhase.BEFORE_STOP),
                                                       context))
               .thenReturn(Arrays.asList(HookPhase.BLUE_GREEN_APPLICATION_BEFORE_STOP_LIVE, HookPhase.APPLICATION_BEFORE_STOP_LIVE));
        List<HookPhase> expectedPhases = Arrays.asList(HookPhase.BLUE_GREEN_APPLICATION_BEFORE_STOP_LIVE,
                                                       HookPhase.APPLICATION_BEFORE_STOP_LIVE);
        List<HookPhase> hookPhasesBeforeStep = step.getHookPhasesBeforeStep(context);
        assertEquals(expectedPhases, hookPhasesBeforeStep);
    }

    @Test
    void testGetHooksBeforeStepWithBlueGreenProcess() {
        Mockito.when(processTypeParser.getProcessType(context.getExecution()))
               .thenReturn(ProcessType.BLUE_GREEN_DEPLOY);
        Mockito.when(hooksPhaseBuilder.buildHookPhases(Arrays.asList(HookPhase.APPLICATION_BEFORE_STOP_IDLE, HookPhase.BEFORE_STOP),
                                                       context))
               .thenReturn(Arrays.asList(HookPhase.BLUE_GREEN_APPLICATION_BEFORE_STOP_LIVE, HookPhase.APPLICATION_BEFORE_STOP_IDLE));
        List<HookPhase> expectedPhases = Arrays.asList(HookPhase.BLUE_GREEN_APPLICATION_BEFORE_STOP_LIVE,
                                                       HookPhase.APPLICATION_BEFORE_STOP_IDLE);
        List<HookPhase> hookPhasesBeforeStep = step.getHookPhasesBeforeStep(context);
        assertEquals(expectedPhases, hookPhasesBeforeStep);
    }

    @Test
    void testGetHooksAfterStep() {
        Mockito.when(hooksPhaseBuilder.buildHookPhases(Arrays.asList(HookPhase.APPLICATION_AFTER_STOP_LIVE, HookPhase.AFTER_STOP), context))
               .thenReturn(Arrays.asList(HookPhase.BLUE_GREEN_APPLICATION_AFTER_STOP_LIVE, HookPhase.APPLICATION_AFTER_STOP_LIVE));
        List<HookPhase> expectedPhases = Arrays.asList(HookPhase.BLUE_GREEN_APPLICATION_AFTER_STOP_LIVE,
                                                       HookPhase.APPLICATION_AFTER_STOP_LIVE);
        List<HookPhase> hookPhasesBeforeStep = step.getHookPhasesAfterStep(context);
        assertEquals(expectedPhases, hookPhasesBeforeStep);
    }

    @Test
    void testGetHooksAfterStepWithBlueGreenProcess() {
        Mockito.when(processTypeParser.getProcessType(context.getExecution()))
               .thenReturn(ProcessType.BLUE_GREEN_DEPLOY);
        Mockito.when(hooksPhaseBuilder.buildHookPhases(Arrays.asList(HookPhase.APPLICATION_AFTER_STOP_IDLE, HookPhase.AFTER_STOP), context))
               .thenReturn(Arrays.asList(HookPhase.BLUE_GREEN_APPLICATION_AFTER_STOP_LIVE, HookPhase.APPLICATION_AFTER_STOP_IDLE));
        List<HookPhase> expectedPhases = Arrays.asList(HookPhase.BLUE_GREEN_APPLICATION_AFTER_STOP_LIVE,
                                                       HookPhase.APPLICATION_AFTER_STOP_IDLE);
        List<HookPhase> hookPhasesBeforeStep = step.getHookPhasesAfterStep(context);
        assertEquals(expectedPhases, hookPhasesBeforeStep);
    }

    @Override
    protected StopAppStep createStep() {
        return new StopAppStep();
    }

    private static class SimpleApplicationWithState extends SimpleApplication {
        final State state;

        public SimpleApplicationWithState(String name, int instances, State state) {
            super(name, instances);
            this.state = state;
        }

        @Override
        CloudApplicationExtended toCloudApplication() {
            return ImmutableCloudApplicationExtended.builder()
                                                    .name(name)
                                                    .state(state)
                                                    .build();
        }
    }

}
