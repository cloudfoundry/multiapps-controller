package org.cloudfoundry.multiapps.controller.process.util;

import java.util.List;

import org.cloudfoundry.multiapps.controller.api.model.ProcessType;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientProvider;
import org.cloudfoundry.multiapps.controller.core.model.HookPhase;
import org.cloudfoundry.multiapps.controller.core.model.Phase;
import org.cloudfoundry.multiapps.controller.core.model.SubprocessPhase;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.steps.StepPhase;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.Hook;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class HooksCalculatorTest {

    private final ProcessContext context = createContext();
    @Mock
    private ProcessTypeParser processTypeParser;

    HooksCalculatorTest() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
    }

    @Test
    void testExecuteHooksForPhaseBeforeStart() {
        HooksCalculator hooksHelper = createHooksCalculator(HookPhase.DEPLOY_APPLICATION_BEFORE_START, HookPhase.NONE);
        Module moduleToDeploy = createModule("module-to-deploy");
        Hook beforeStartIdleHook = createHook("before-start", List.of("deploy.application.before-start"));
        moduleToDeploy.setHooks(List.of(beforeStartIdleHook));
        Mockito.when(processTypeParser.getProcessTypeFromProcessVariable(context.getExecution()))
               .thenReturn(ProcessType.DEPLOY);
        List<Hook> hooksForCurrentPhase = hooksHelper.calculateHooksForExecution(moduleToDeploy, StepPhase.EXECUTE);
        Assertions.assertEquals(1, hooksForCurrentPhase.size());
        List<String> expectedHookPhases = List.of("deploy.application.before-start");
        Assertions.assertEquals(expectedHookPhases, hooksForCurrentPhase.get(0)
                                                                        .getPhases());
    }

    @Test
    void testExecuteHooksForPhaseBeforeStartIdle() {
        HooksCalculator hooksHelper = createHooksCalculator(HookPhase.BLUE_GREEN_APPLICATION_BEFORE_START_IDLE, HookPhase.NONE);
        Module moduleToDeploy = createModule("module-to-deploy");
        Hook beforeStartIdleHook = createHook("before-start-idle", List.of("blue-green.application.before-start.idle"));
        moduleToDeploy.setHooks(List.of(beforeStartIdleHook));
        Mockito.when(processTypeParser.getProcessTypeFromProcessVariable(context.getExecution()))
               .thenReturn(ProcessType.BLUE_GREEN_DEPLOY);
        context.setVariable(Variables.SUBPROCESS_PHASE, SubprocessPhase.BEFORE_APPLICATION_START);
        List<Hook> hooksForCurrentPhase = hooksHelper.calculateHooksForExecution(moduleToDeploy, StepPhase.EXECUTE);
        Assertions.assertEquals(1, hooksForCurrentPhase.size());
        List<String> expectedHookPhases = List.of("blue-green.application.before-start.idle");
        Assertions.assertEquals(expectedHookPhases, hooksForCurrentPhase.get(0)
                                                                        .getPhases());
    }

    @Test
    void testExecuteHooksForPhaseBeforeStartLive() {
        HooksCalculator hooksHelper = createHooksCalculator(HookPhase.BLUE_GREEN_APPLICATION_BEFORE_START_LIVE, HookPhase.NONE);
        Module moduleToDeploy = createModule("module-to-deploy");
        Hook beforeStartLiveHook = createHook("before-start-live", List.of("blue-green.application.before-start.live"));
        moduleToDeploy.setHooks(List.of(beforeStartLiveHook));
        context.setVariable(Variables.SUBPROCESS_PHASE, SubprocessPhase.BEFORE_APPLICATION_START);
        context.setVariable(Variables.PHASE, Phase.AFTER_RESUME);
        List<Hook> hooksForCurrentPhase = hooksHelper.calculateHooksForExecution(moduleToDeploy, StepPhase.EXECUTE);
        Assertions.assertEquals(1, hooksForCurrentPhase.size());
        List<String> expectedHookPhases = List.of("blue-green.application.before-start.live");
        Assertions.assertEquals(expectedHookPhases, hooksForCurrentPhase.get(0)
                                                                        .getPhases());
    }

    @Test
    void testExecuteHooksForPhaseBeforeStop() {
        HooksCalculator hooksHelper = createHooksCalculator(HookPhase.DEPLOY_APPLICATION_BEFORE_STOP, HookPhase.NONE);
        Module moduleToDeploy = createModule("module-to-deploy");
        Hook beforeStartIdleHook = createHook("before-start", List.of("deploy.application.before-stop"));
        moduleToDeploy.setHooks(List.of(beforeStartIdleHook));
        Mockito.when(processTypeParser.getProcessTypeFromProcessVariable(context.getExecution()))
               .thenReturn(ProcessType.DEPLOY);
        List<Hook> hooksForCurrentPhase = hooksHelper.calculateHooksForExecution(moduleToDeploy, StepPhase.EXECUTE);
        Assertions.assertEquals(1, hooksForCurrentPhase.size());
        List<String> expectedHookPhases = List.of("deploy.application.before-stop");
        Assertions.assertEquals(expectedHookPhases, hooksForCurrentPhase.get(0)
                                                                        .getPhases());
    }

    @Test
    void testExecuteHooksForPhaseBeforeStopIdle() {
        HooksCalculator hooksHelper = createHooksCalculator(HookPhase.BLUE_GREEN_APPLICATION_BEFORE_STOP_IDLE, HookPhase.NONE);
        Module moduleToDeploy = createModule("module-to-deploy");
        Hook beforeStopIdleHook = createHook("before-stop-idle", List.of("blue-green.application.before-stop.idle"));
        moduleToDeploy.setHooks(List.of(beforeStopIdleHook));
        Mockito.when(processTypeParser.getProcessTypeFromProcessVariable(context.getExecution()))
               .thenReturn(ProcessType.BLUE_GREEN_DEPLOY);
        context.setVariable(Variables.SUBPROCESS_PHASE, SubprocessPhase.BEFORE_APPLICATION_START);
        List<Hook> hooksForCurrentPhase = hooksHelper.calculateHooksForExecution(moduleToDeploy, StepPhase.EXECUTE);
        Assertions.assertEquals(1, hooksForCurrentPhase.size());
        List<String> expectedHookPhases = List.of("blue-green.application.before-stop.idle");
        Assertions.assertEquals(expectedHookPhases, hooksForCurrentPhase.get(0)
                                                                        .getPhases());
    }

    @Test
    void testExecuteHooksForPhaseBeforeStopLive() {
        HooksCalculator hooksHelper = createHooksCalculator(HookPhase.BLUE_GREEN_APPLICATION_BEFORE_STOP_LIVE, HookPhase.NONE);
        Module moduleToDeploy = createModule("module-to-deploy");
        Hook beforeStopLiveHook = createHook("before-stop-live", List.of("blue-green.application.before-stop.live"));
        moduleToDeploy.setHooks(List.of(beforeStopLiveHook));
        Mockito.when(processTypeParser.getProcessTypeFromProcessVariable(context.getExecution()))
               .thenReturn(ProcessType.BLUE_GREEN_DEPLOY);
        List<Hook> hooksForCurrentPhase = hooksHelper.calculateHooksForExecution(moduleToDeploy, StepPhase.EXECUTE);
        Assertions.assertEquals(1, hooksForCurrentPhase.size());
        List<String> expectedHookPhases = List.of("blue-green.application.before-stop.live");
        Assertions.assertEquals(expectedHookPhases, hooksForCurrentPhase.get(0)
                                                                        .getPhases());
    }

    @Test
    void testExecuteHooksForPhaseAfterStop() {
        HooksCalculator hooksHelper = createHooksCalculator(HookPhase.NONE, HookPhase.DEPLOY_APPLICATION_AFTER_STOP);
        Module moduleToDeploy = createModule("module-to-deploy");
        Hook afterStopHook = createHook("after-stop", List.of("deploy.application.after-stop"));
        moduleToDeploy.setHooks(List.of(afterStopHook));
        Mockito.when(processTypeParser.getProcessTypeFromProcessVariable(context.getExecution()))
               .thenReturn(ProcessType.DEPLOY);
        List<Hook> hooksForCurrentPhase = hooksHelper.calculateHooksForExecution(moduleToDeploy, StepPhase.DONE);
        Assertions.assertEquals(1, hooksForCurrentPhase.size());
        List<String> expectedHookPhases = List.of("deploy.application.after-stop");
        Assertions.assertEquals(expectedHookPhases, hooksForCurrentPhase.get(0)
                                                                        .getPhases());
    }

    @Test
    void testExecuteHooksForPhaseAfterStopIdle() {
        HooksCalculator hooksHelper = createHooksCalculator(HookPhase.NONE, HookPhase.BLUE_GREEN_APPLICATION_AFTER_STOP_IDLE);
        Module moduleToDeploy = createModule("module-to-deploy");
        Hook afterStopIdleHook = createHook("after-stop-idle", List.of("blue-green.application.after-stop.idle"));
        moduleToDeploy.setHooks(List.of(afterStopIdleHook));
        Mockito.when(processTypeParser.getProcessTypeFromProcessVariable(context.getExecution()))
               .thenReturn(ProcessType.BLUE_GREEN_DEPLOY);
        context.setVariable(Variables.SUBPROCESS_PHASE, SubprocessPhase.BEFORE_APPLICATION_START);
        List<Hook> hooksForCurrentPhase = hooksHelper.calculateHooksForExecution(moduleToDeploy, StepPhase.DONE);
        Assertions.assertEquals(1, hooksForCurrentPhase.size());
        List<String> expectedHookPhases = List.of("blue-green.application.after-stop.idle");
        Assertions.assertEquals(expectedHookPhases, hooksForCurrentPhase.get(0)
                                                                        .getPhases());
    }

    @Test
    void testExecuteHooksForPhaseAfterStopLive() {
        HooksCalculator hooksHelper = createHooksCalculator(HookPhase.NONE, HookPhase.BLUE_GREEN_APPLICATION_AFTER_STOP_LIVE);
        Module moduleToDeploy = createModule("module-to-deploy");
        Hook afterStopLiveHook = createHook("after-stop-live", List.of("blue-green.application.after-stop.live"));
        moduleToDeploy.setHooks(List.of(afterStopLiveHook));
        Mockito.when(processTypeParser.getProcessTypeFromProcessVariable(context.getExecution()))
               .thenReturn(ProcessType.BLUE_GREEN_DEPLOY);
        List<Hook> hooksForCurrentPhase = hooksHelper.calculateHooksForExecution(moduleToDeploy, StepPhase.DONE);
        Assertions.assertEquals(1, hooksForCurrentPhase.size());
        List<String> expectedHookPhases = List.of("blue-green.application.after-stop.live");
        Assertions.assertEquals(expectedHookPhases, hooksForCurrentPhase.get(0)
                                                                        .getPhases());
    }

    @Test
    void testExecuteHooksForPhaseBeforeUnmapRoutes() {
        HooksCalculator hooksHelper = createHooksCalculator(HookPhase.BLUE_GREEN_APPLICATION_BEFORE_UNMAP_ROUTES_LIVE, HookPhase.NONE);
        Module moduleToDeploy = createModule("module-to-deploy");
        Hook afterUnmapRoutesHook = createHook("before-unmap-routes", List.of("blue-green.application.before-unmap-routes.live"));
        moduleToDeploy.setHooks(List.of(afterUnmapRoutesHook));
        Mockito.when(processTypeParser.getProcessTypeFromProcessVariable(context.getExecution()))
               .thenReturn(ProcessType.BLUE_GREEN_DEPLOY);
        List<Hook> hooksForCurrentPhase = hooksHelper.calculateHooksForExecution(moduleToDeploy, StepPhase.EXECUTE);
        Assertions.assertEquals(1, hooksForCurrentPhase.size());
        List<String> expectedHookPhases = List.of("blue-green.application.before-unmap-routes.live");
        Assertions.assertEquals(expectedHookPhases, hooksForCurrentPhase.get(0)
                                                                        .getPhases());
    }

    @Test
    void testExecuteHooksForPhaseNoHooks() {
        HooksCalculator hooksHelper = createHooksCalculator(HookPhase.NONE, HookPhase.NONE);
        Module moduleToDeploy = createModule("module-to-deploy");
        Mockito.when(processTypeParser.getProcessTypeFromProcessVariable(context.getExecution()))
               .thenReturn(ProcessType.DEPLOY);
        List<Hook> hooksForCurrentPhase = hooksHelper.calculateHooksForExecution(moduleToDeploy, StepPhase.POLL);
        Assertions.assertEquals(0, hooksForCurrentPhase.size());
    }

    private HooksCalculator createHooksCalculator(HookPhase hookPhaseBeforeStep, HookPhase hookPhaseAfterStep) {
        return ImmutableHooksCalculator.builder()
                                       .context(context)
                                       .hookPhasesBeforeStep(List.of(hookPhaseBeforeStep))
                                       .hookPhasesAfterStep(List.of(hookPhaseAfterStep))
                                       .build();
    }

    private ProcessContext createContext() {
        DelegateExecution delegateExecution = MockDelegateExecution.createSpyInstance();
        StepLogger stepLogger = Mockito.mock(StepLogger.class);
        CloudControllerClientProvider cloudControllerClientProvider = Mockito.mock(CloudControllerClientProvider.class);
        return new ProcessContext(delegateExecution, stepLogger, cloudControllerClientProvider);
    }

    private Hook createHook(String name, List<String> phases) {
        return Hook.createV3()
                   .setName(name)
                   .setPhases(phases);
    }

    private Module createModule(String moduleName) {
        return Module.createV3()
                     .setName(moduleName);
    }
}
