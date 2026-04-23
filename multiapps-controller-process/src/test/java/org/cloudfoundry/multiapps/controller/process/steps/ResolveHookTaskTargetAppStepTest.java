package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.List;
import java.util.Map;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.model.ApplicationColor;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMta;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableDeployedMta;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.ImmutableMtaMetadata;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.Hook;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResolveHookTaskTargetAppStepTest extends SyncFlowableStepTest<ResolveHookTaskTargetAppStep> {

    private static final String BEFORE_STOP_LIVE = "blue-green.application.before-stop.live";
    private static final String BEFORE_START_IDLE = "blue-green.application.before-start.idle";

    private static final String PHASE_KEY = "phase";
    private static final String TARGET_APP_KEY = "target-app";
    private static final String TARGET_IDLE = "idle";
    private static final String TARGET_LIVE = "live";

    private static final String HOOK_NAME = "test-hook";
    private static final String APP_BASE_NAME = "my-app";
    private static final String APP_NAME_GREEN = "my-app-green";
    private static final String APP_NAME_BLUE = "my-app-blue";
    private static final String APP_NAME_LIVE = "my-app-live";
    private static final String APP_NAME_IDLE = "my-app-idle";

    private static final DeployedMta EXISTING_MTA = ImmutableDeployedMta.builder()
                                                                       .metadata(ImmutableMtaMetadata.builder()
                                                                                                     .id("test-mta")
                                                                                                     .build())
                                                                       .build();

    @Override
    protected ResolveHookTaskTargetAppStep createStep() {
        return new ResolveHookTaskTargetAppStep();
    }

    @Test
    void appNameUnchangedWhenNoHookSet() {
        context.setVariable(Variables.APP_TO_PROCESS, buildApp(APP_BASE_NAME));
        context.setVariable(Variables.HOOK_FOR_EXECUTION, null);

        step.execute(execution);

        assertStepFinishedSuccessfully();
        assertEquals(APP_BASE_NAME, context.getVariable(Variables.APP_TO_PROCESS)
                                           .getName());
    }

    @Test
    void appNameUnchangedWhenHookHasNoPhasesConfig() {
        context.setVariable(Variables.APP_TO_PROCESS, buildApp(APP_BASE_NAME));
        context.setVariable(Variables.HOOK_FOR_EXECUTION, buildHook(List.of(BEFORE_STOP_LIVE), Map.of()));

        step.execute(execution);

        assertStepFinishedSuccessfully();
        assertEquals(APP_BASE_NAME, context.getVariable(Variables.APP_TO_PROCESS)
                                           .getName());
    }

    @Test
    void appNameUnchangedWhenNoPhasesConfigMatchesCurrentPhase() {
        context.setVariable(Variables.APP_TO_PROCESS, buildApp(APP_NAME_LIVE));
        context.setVariable(Variables.HOOK_EXECUTION_PHASES, List.of(BEFORE_STOP_LIVE));
        context.setVariable(Variables.HOOK_FOR_EXECUTION, buildHookWithPhasesConfig(
            List.of(BEFORE_STOP_LIVE),
            List.of(Map.of(PHASE_KEY, BEFORE_START_IDLE, TARGET_APP_KEY, TARGET_IDLE))
        ));

        step.execute(execution);

        assertStepFinishedSuccessfully();
        assertEquals(APP_NAME_LIVE, context.getVariable(Variables.APP_TO_PROCESS)
                                           .getName());
    }

    @Test
    void bgDeploy_targetIdle_resolvesToIdleColorApp() {
        context.setVariable(Variables.APP_TO_PROCESS, buildApp(APP_NAME_GREEN));
        context.setVariable(Variables.IDLE_MTA_COLOR, ApplicationColor.BLUE);
        context.setVariable(Variables.LIVE_MTA_COLOR, ApplicationColor.GREEN);
        context.setVariable(Variables.HOOK_EXECUTION_PHASES, List.of(BEFORE_STOP_LIVE));
        context.setVariable(Variables.HOOK_FOR_EXECUTION, buildHookWithPhasesConfig(
            List.of(BEFORE_STOP_LIVE),
            List.of(Map.of(PHASE_KEY, BEFORE_STOP_LIVE, TARGET_APP_KEY, TARGET_IDLE))
        ));

        step.execute(execution);

        assertStepFinishedSuccessfully();
        assertEquals(APP_NAME_BLUE, context.getVariable(Variables.APP_TO_PROCESS)
                                           .getName());
    }

    @Test
    void bgDeploy_targetLive_resolvesToLiveColorApp() {
        context.setVariable(Variables.APP_TO_PROCESS, buildApp(APP_NAME_BLUE));
        context.setVariable(Variables.IDLE_MTA_COLOR, ApplicationColor.BLUE);
        context.setVariable(Variables.LIVE_MTA_COLOR, ApplicationColor.GREEN);
        context.setVariable(Variables.DEPLOYED_MTA, EXISTING_MTA);
        context.setVariable(Variables.HOOK_EXECUTION_PHASES, List.of(BEFORE_START_IDLE));
        context.setVariable(Variables.HOOK_FOR_EXECUTION, buildHookWithPhasesConfig(
            List.of(BEFORE_START_IDLE),
            List.of(Map.of(PHASE_KEY, BEFORE_START_IDLE, TARGET_APP_KEY, TARGET_LIVE))
        ));

        step.execute(execution);

        assertStepFinishedSuccessfully();
        assertEquals(APP_NAME_GREEN, context.getVariable(Variables.APP_TO_PROCESS)
                                            .getName());
    }

    @Test
    void bgDeploy_targetIdle_appHasNoSuffix_appendsIdleColorSuffix() {
        context.setVariable(Variables.APP_TO_PROCESS, buildApp(APP_BASE_NAME));
        context.setVariable(Variables.IDLE_MTA_COLOR, ApplicationColor.BLUE);
        context.setVariable(Variables.LIVE_MTA_COLOR, ApplicationColor.GREEN);
        context.setVariable(Variables.HOOK_EXECUTION_PHASES, List.of(BEFORE_STOP_LIVE));
        context.setVariable(Variables.HOOK_FOR_EXECUTION, buildHookWithPhasesConfig(
            List.of(BEFORE_STOP_LIVE),
            List.of(Map.of(PHASE_KEY, BEFORE_STOP_LIVE, TARGET_APP_KEY, TARGET_IDLE))
        ));

        step.execute(execution);

        assertStepFinishedSuccessfully();
        assertEquals(APP_NAME_BLUE, context.getVariable(Variables.APP_TO_PROCESS)
                                           .getName());
    }

    @Test
    void strategyBgDeploy_targetIdle_appHasLiveSuffix_resolvesToIdleApp() {
        context.setVariable(Variables.APP_TO_PROCESS, buildApp(APP_NAME_LIVE));
        context.setVariable(Variables.HOOK_EXECUTION_PHASES, List.of(BEFORE_STOP_LIVE));
        context.setVariable(Variables.HOOK_FOR_EXECUTION, buildHookWithPhasesConfig(
            List.of(BEFORE_STOP_LIVE),
            List.of(Map.of(PHASE_KEY, BEFORE_STOP_LIVE, TARGET_APP_KEY, TARGET_IDLE))
        ));

        step.execute(execution);

        assertStepFinishedSuccessfully();
        assertEquals(APP_NAME_IDLE, context.getVariable(Variables.APP_TO_PROCESS)
                                           .getName());
    }

    @Test
    void strategyBgDeploy_targetIdle_appHasNoSuffix_appendsIdleSuffix() {
        context.setVariable(Variables.APP_TO_PROCESS, buildApp(APP_BASE_NAME));
        context.setVariable(Variables.HOOK_EXECUTION_PHASES, List.of(BEFORE_STOP_LIVE));
        context.setVariable(Variables.HOOK_FOR_EXECUTION, buildHookWithPhasesConfig(
            List.of(BEFORE_STOP_LIVE),
            List.of(Map.of(PHASE_KEY, BEFORE_STOP_LIVE, TARGET_APP_KEY, TARGET_IDLE))
        ));

        step.execute(execution);

        assertStepFinishedSuccessfully();
        assertEquals(APP_NAME_IDLE, context.getVariable(Variables.APP_TO_PROCESS)
                                           .getName());
    }

    @Test
    void strategyBgDeploy_targetIdle_appAlreadyHasIdleSuffix_unchanged() {
        context.setVariable(Variables.APP_TO_PROCESS, buildApp(APP_NAME_IDLE));
        context.setVariable(Variables.HOOK_EXECUTION_PHASES, List.of(BEFORE_STOP_LIVE));
        context.setVariable(Variables.HOOK_FOR_EXECUTION, buildHookWithPhasesConfig(
            List.of(BEFORE_STOP_LIVE),
            List.of(Map.of(PHASE_KEY, BEFORE_STOP_LIVE, TARGET_APP_KEY, TARGET_IDLE))
        ));

        step.execute(execution);

        assertStepFinishedSuccessfully();
        assertEquals(APP_NAME_IDLE, context.getVariable(Variables.APP_TO_PROCESS)
                                           .getName());
    }

    @Test
    void strategyBgDeploy_targetLive_appHasIdleSuffix_resolvesToLiveApp() {
        context.setVariable(Variables.APP_TO_PROCESS, buildApp(APP_NAME_IDLE));
        context.setVariable(Variables.DEPLOYED_MTA, EXISTING_MTA);
        context.setVariable(Variables.HOOK_EXECUTION_PHASES, List.of(BEFORE_START_IDLE));
        context.setVariable(Variables.HOOK_FOR_EXECUTION, buildHookWithPhasesConfig(
            List.of(BEFORE_START_IDLE),
            List.of(Map.of(PHASE_KEY, BEFORE_START_IDLE, TARGET_APP_KEY, TARGET_LIVE))
        ));

        step.execute(execution);

        assertStepFinishedSuccessfully();
        assertEquals(APP_NAME_LIVE, context.getVariable(Variables.APP_TO_PROCESS)
                                           .getName());
    }

    @Test
    void strategyBgDeploy_targetLive_appAlreadyHasLiveSuffix_unchanged() {
        context.setVariable(Variables.APP_TO_PROCESS, buildApp(APP_NAME_LIVE));
        context.setVariable(Variables.DEPLOYED_MTA, EXISTING_MTA);
        context.setVariable(Variables.HOOK_EXECUTION_PHASES, List.of(BEFORE_START_IDLE));
        context.setVariable(Variables.HOOK_FOR_EXECUTION, buildHookWithPhasesConfig(
            List.of(BEFORE_START_IDLE),
            List.of(Map.of(PHASE_KEY, BEFORE_START_IDLE, TARGET_APP_KEY, TARGET_LIVE))
        ));

        step.execute(execution);

        assertStepFinishedSuccessfully();
        assertEquals(APP_NAME_LIVE, context.getVariable(Variables.APP_TO_PROCESS)
                                           .getName());
    }

    @Test
    void strategyBgDeploy_targetLive_appHasNoSuffix_appendsLiveSuffix() {
        context.setVariable(Variables.APP_TO_PROCESS, buildApp(APP_BASE_NAME));
        context.setVariable(Variables.DEPLOYED_MTA, EXISTING_MTA);
        context.setVariable(Variables.HOOK_EXECUTION_PHASES, List.of(BEFORE_START_IDLE));
        context.setVariable(Variables.HOOK_FOR_EXECUTION, buildHookWithPhasesConfig(
            List.of(BEFORE_START_IDLE),
            List.of(Map.of(PHASE_KEY, BEFORE_START_IDLE, TARGET_APP_KEY, TARGET_LIVE))
        ));

        step.execute(execution);

        assertStepFinishedSuccessfully();
        assertEquals(APP_NAME_LIVE, context.getVariable(Variables.APP_TO_PROCESS)
                                           .getName());
    }

    @Test
    void initialDeploy_targetLive_skipsTask() {
        context.setVariable(Variables.APP_TO_PROCESS, buildApp(APP_NAME_IDLE));
        context.setVariable(Variables.HOOK_EXECUTION_PHASES, List.of(BEFORE_START_IDLE));
        context.setVariable(Variables.HOOK_FOR_EXECUTION, buildHookWithPhasesConfig(
            List.of(BEFORE_START_IDLE),
            List.of(Map.of(PHASE_KEY, BEFORE_START_IDLE, TARGET_APP_KEY, TARGET_LIVE))
        ));

        step.execute(execution);

        assertStepFinishedSuccessfully();
        assertTrue(context.getVariable(Variables.TASKS_TO_EXECUTE).isEmpty());
    }

    private CloudApplicationExtended buildApp(String name) {
        return ImmutableCloudApplicationExtended.builder()
                                                .name(name)
                                                .build();
    }

    private Hook buildHook(List<String> phases, Map<String, Object> parameters) {
        return Hook.createV3()
                   .setName(HOOK_NAME)
                   .setType("task")
                   .setPhases(phases)
                   .setParameters(parameters);
    }

    private Hook buildHookWithPhasesConfig(List<String> phases,
                                           List<Map<String, String>> phasesConfig) {
        return buildHook(phases, Map.of(SupportedParameters.PHASES_CONFIG, phasesConfig));
    }

}
