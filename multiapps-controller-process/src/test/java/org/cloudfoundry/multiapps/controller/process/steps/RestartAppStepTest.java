package org.cloudfoundry.multiapps.controller.process.steps;

import java.time.Duration;
import java.util.List;

import org.cloudfoundry.multiapps.controller.api.model.ProcessType;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudApplication;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.processor.MtaMetadataParser;
import org.cloudfoundry.multiapps.controller.core.model.HookPhase;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.process.util.HooksExecutor;
import org.cloudfoundry.multiapps.controller.process.util.HooksPhaseBuilder;
import org.cloudfoundry.multiapps.controller.process.util.HooksPhaseGetter;
import org.cloudfoundry.multiapps.controller.process.util.ProcessTypeParser;
import org.cloudfoundry.multiapps.controller.process.util.TimeoutType;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.Hook;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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
        when(processTypeParser.getProcessType(context.getExecution())).thenReturn(ProcessType.DEPLOY);
        CloudApplicationExtended app = createApplication(APP_NAME, CloudApplication.State.STOPPED);
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
        when(processTypeParser.getProcessType(context.getExecution())).thenReturn(ProcessType.DEPLOY);
        CloudApplicationExtended app = createApplication(APP_NAME, CloudApplication.State.STARTED);
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
        when(hooksPhaseBuilder.buildHookPhases(List.of(HookPhase.BEFORE_START), context)).thenReturn(
            List.of(HookPhase.BLUE_GREEN_APPLICATION_BEFORE_START_LIVE));
        List<HookPhase> expectedHooks = List.of(HookPhase.BLUE_GREEN_APPLICATION_BEFORE_START_LIVE);
        List<HookPhase> hookPhasesBeforeStep = step.getHookPhasesBeforeStep(context);
        assertEquals(expectedHooks, hookPhasesBeforeStep);
    }

    private CloudApplicationExtended createApplication(String name, CloudApplication.State state) {
        return ImmutableCloudApplicationExtended.builder()
                                                .name(name)
                                                .state(state)
                                                .build();
    }

    private void prepareContextAndClient(CloudApplicationExtended app) {
        when(client.getApplication(APP_NAME)).thenReturn(app);
        context.setVariable(Variables.APP_TO_PROCESS, app);
        context.setVariable(Variables.DEPLOYMENT_DESCRIPTOR, descriptor);
    }

    @ParameterizedTest
    @MethodSource("testValidatePriority")
    void testGetTimeout(Integer timeoutProcessVariable, Integer timeoutModuleLevel, Integer timeoutGlobalLevel, int expectedTimeout) {
        step.initializeStepLogger(execution);
        setUpContext(timeoutProcessVariable, timeoutModuleLevel, timeoutGlobalLevel, Variables.APPS_START_TIMEOUT_PROCESS_VARIABLE,
                     SupportedParameters.START_TIMEOUT, SupportedParameters.APPS_START_TIMEOUT);

        Duration actualTimeout = step.getTimeout(context);
        assertEquals(Duration.ofSeconds(expectedTimeout), actualTimeout);
    }

    @Test
    void testGetTimeoutWhenHooksAreBeingExecuted() {
        step.initializeStepLogger(execution);
        Module moduleToDeployWithHooks = context.getVariable(Variables.MODULE_TO_DEPLOY);
        List<Hook> hooksForExecution = buildHooksForExecution();
        moduleToDeployWithHooks.setHooks(hooksForExecution);
        context.setVariable(Variables.MODULE_TO_DEPLOY, moduleToDeployWithHooks);
        context.setVariable(TimeoutType.START.getProcessVariable(), TimeoutType.START.getProcessVariable()
                                                                                     .getDefaultValue());
        context.setVariable(TimeoutType.TASK.getProcessVariable(), TimeoutType.TASK.getProcessVariable()
                                                                                   .getDefaultValue());
        when(hooksPhaseGetter.getHookPhasesBeforeStop(any(), any())).thenReturn(
            List.of(HookPhase.BLUE_GREEN_APPLICATION_BEFORE_START_LIVE));
        Duration timeout = step.getTimeout(context);
        assertEquals(Variables.APPS_TASK_EXECUTION_TIMEOUT_PROCESS_VARIABLE.getDefaultValue(), timeout);
    }

    private List<Hook> buildHooksForExecution() {
        Hook hook = Hook.createV3()
                        .setName("custom-hook")
                        .setPhases(List.of(HookPhase.BLUE_GREEN_APPLICATION_BEFORE_START_LIVE.getValue()));
        return List.of(hook);
    }

    @Override
    protected RestartAppStep createStep() {
        return new RestartAppStep();
    }

}
