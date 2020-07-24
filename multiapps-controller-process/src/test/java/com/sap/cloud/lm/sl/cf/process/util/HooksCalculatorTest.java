package com.sap.cloud.lm.sl.cf.process.util;

import java.util.Collections;
import java.util.List;

import org.cloudfoundry.multiapps.mta.model.Hook;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.sap.cloud.lm.sl.cf.core.cf.CloudControllerClientProvider;
import com.sap.cloud.lm.sl.cf.core.model.HookPhase;
import com.sap.cloud.lm.sl.cf.core.model.Phase;
import com.sap.cloud.lm.sl.cf.core.model.SubprocessPhase;
import com.sap.cloud.lm.sl.cf.process.mock.MockDelegateExecution;
import com.sap.cloud.lm.sl.cf.process.steps.ProcessContext;
import com.sap.cloud.lm.sl.cf.process.steps.StepPhase;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.cf.web.api.model.ProcessType;

class HooksCalculatorTest {

    private final ProcessContext context = createContext();
    @Mock
    private ProcessTypeParser processTypeParser;

    HooksCalculatorTest() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void testExecuteHooksForPhaseBeforeStart() {
        HooksCalculator hooksHelper = createHooksCalculator(HookPhase.DEPLOY_APPLICATION_BEFORE_START, HookPhase.NONE);
        Module moduleToDeploy = createModule("module-to-deploy");
        Hook beforeStartIdleHook = createHook("before-start", Collections.singletonList("deploy.application.before-start"));
        moduleToDeploy.setHooks(Collections.singletonList(beforeStartIdleHook));
        Mockito.when(processTypeParser.getProcessType(context.getExecution()))
               .thenReturn(ProcessType.DEPLOY);
        List<Hook> hooksForCurrentPhase = hooksHelper.calculateHooksForExecution(moduleToDeploy, StepPhase.EXECUTE);
        Assertions.assertEquals(1, hooksForCurrentPhase.size());
        List<String> expectedHookPhases = Collections.singletonList("deploy.application.before-start");
        Assertions.assertEquals(expectedHookPhases, hooksForCurrentPhase.get(0)
                                                                        .getPhases());
    }

    @Test
    void testExecuteHooksForPhaseBeforeStartIdle() {
        HooksCalculator hooksHelper = createHooksCalculator(HookPhase.BLUE_GREEN_APPLICATION_BEFORE_START_IDLE, HookPhase.NONE);
        Module moduleToDeploy = createModule("module-to-deploy");
        Hook beforeStartIdleHook = createHook("before-start-idle", Collections.singletonList("blue-green.application.before-start.idle"));
        moduleToDeploy.setHooks(Collections.singletonList(beforeStartIdleHook));
        Mockito.when(processTypeParser.getProcessType(context.getExecution()))
               .thenReturn(ProcessType.BLUE_GREEN_DEPLOY);
        context.setVariable(Variables.SUBPROCESS_PHASE, SubprocessPhase.BEFORE_APPLICATION_START);
        List<Hook> hooksForCurrentPhase = hooksHelper.calculateHooksForExecution(moduleToDeploy, StepPhase.EXECUTE);
        Assertions.assertEquals(1, hooksForCurrentPhase.size());
        List<String> expectedHookPhases = Collections.singletonList("blue-green.application.before-start.idle");
        Assertions.assertEquals(expectedHookPhases, hooksForCurrentPhase.get(0)
                                                                        .getPhases());
    }

    @Test
    void testExecuteHooksForPhaseBeforeStartLive() {
        HooksCalculator hooksHelper = createHooksCalculator(HookPhase.BLUE_GREEN_APPLICATION_BEFORE_START_LIVE, HookPhase.NONE);
        Module moduleToDeploy = createModule("module-to-deploy");
        Hook beforeStartLiveHook = createHook("before-start-live", Collections.singletonList("blue-green.application.before-start.live"));
        moduleToDeploy.setHooks(Collections.singletonList(beforeStartLiveHook));
        context.setVariable(Variables.SUBPROCESS_PHASE, SubprocessPhase.BEFORE_APPLICATION_START);
        context.setVariable(Variables.PHASE, Phase.AFTER_RESUME);
        List<Hook> hooksForCurrentPhase = hooksHelper.calculateHooksForExecution(moduleToDeploy, StepPhase.EXECUTE);
        Assertions.assertEquals(1, hooksForCurrentPhase.size());
        List<String> expectedHookPhases = Collections.singletonList("blue-green.application.before-start.live");
        Assertions.assertEquals(expectedHookPhases, hooksForCurrentPhase.get(0)
                                                                        .getPhases());
    }

    @Test
    void testExecuteHooksForPhaseBeforeStop() {
        HooksCalculator hooksHelper = createHooksCalculator(HookPhase.DEPLOY_APPLICATION_BEFORE_STOP, HookPhase.NONE);
        Module moduleToDeploy = createModule("module-to-deploy");
        Hook beforeStartIdleHook = createHook("before-start", Collections.singletonList("deploy.application.before-stop"));
        moduleToDeploy.setHooks(Collections.singletonList(beforeStartIdleHook));
        Mockito.when(processTypeParser.getProcessType(context.getExecution()))
               .thenReturn(ProcessType.DEPLOY);
        List<Hook> hooksForCurrentPhase = hooksHelper.calculateHooksForExecution(moduleToDeploy, StepPhase.EXECUTE);
        Assertions.assertEquals(1, hooksForCurrentPhase.size());
        List<String> expectedHookPhases = Collections.singletonList("deploy.application.before-stop");
        Assertions.assertEquals(expectedHookPhases, hooksForCurrentPhase.get(0)
                                                                        .getPhases());
    }

    @Test
    void testExecuteHooksForPhaseBeforeStopIdle() {
        HooksCalculator hooksHelper = createHooksCalculator(HookPhase.BLUE_GREEN_APPLICATION_BEFORE_STOP_IDLE, HookPhase.NONE);
        Module moduleToDeploy = createModule("module-to-deploy");
        Hook beforeStopIdleHook = createHook("before-stop-idle", Collections.singletonList("blue-green.application.before-stop.idle"));
        moduleToDeploy.setHooks(Collections.singletonList(beforeStopIdleHook));
        Mockito.when(processTypeParser.getProcessType(context.getExecution()))
               .thenReturn(ProcessType.BLUE_GREEN_DEPLOY);
        context.setVariable(Variables.SUBPROCESS_PHASE, SubprocessPhase.BEFORE_APPLICATION_START);
        List<Hook> hooksForCurrentPhase = hooksHelper.calculateHooksForExecution(moduleToDeploy, StepPhase.EXECUTE);
        Assertions.assertEquals(1, hooksForCurrentPhase.size());
        List<String> expectedHookPhases = Collections.singletonList("blue-green.application.before-stop.idle");
        Assertions.assertEquals(expectedHookPhases, hooksForCurrentPhase.get(0)
                                                                        .getPhases());
    }

    @Test
    void testExecuteHooksForPhaseBeforeStopLive() {
        HooksCalculator hooksHelper = createHooksCalculator(HookPhase.BLUE_GREEN_APPLICATION_BEFORE_STOP_LIVE, HookPhase.NONE);
        Module moduleToDeploy = createModule("module-to-deploy");
        Hook beforeStopLiveHook = createHook("before-stop-live", Collections.singletonList("blue-green.application.before-stop.live"));
        moduleToDeploy.setHooks(Collections.singletonList(beforeStopLiveHook));
        Mockito.when(processTypeParser.getProcessType(context.getExecution()))
               .thenReturn(ProcessType.BLUE_GREEN_DEPLOY);
        List<Hook> hooksForCurrentPhase = hooksHelper.calculateHooksForExecution(moduleToDeploy, StepPhase.EXECUTE);
        Assertions.assertEquals(1, hooksForCurrentPhase.size());
        List<String> expectedHookPhases = Collections.singletonList("blue-green.application.before-stop.live");
        Assertions.assertEquals(expectedHookPhases, hooksForCurrentPhase.get(0)
                                                                        .getPhases());
    }

    @Test
    void testExecuteHooksForPhaseAfterStop() {
        HooksCalculator hooksHelper = createHooksCalculator(HookPhase.NONE, HookPhase.DEPLOY_APPLICATION_AFTER_STOP);
        Module moduleToDeploy = createModule("module-to-deploy");
        Hook afterStopHook = createHook("after-stop", Collections.singletonList("deploy.application.after-stop"));
        moduleToDeploy.setHooks(Collections.singletonList(afterStopHook));
        Mockito.when(processTypeParser.getProcessType(context.getExecution()))
               .thenReturn(ProcessType.DEPLOY);
        List<Hook> hooksForCurrentPhase = hooksHelper.calculateHooksForExecution(moduleToDeploy, StepPhase.DONE);
        Assertions.assertEquals(1, hooksForCurrentPhase.size());
        List<String> expectedHookPhases = Collections.singletonList("deploy.application.after-stop");
        Assertions.assertEquals(expectedHookPhases, hooksForCurrentPhase.get(0)
                                                                        .getPhases());
    }

    @Test
    void testExecuteHooksForPhaseAfterStopIdle() {
        HooksCalculator hooksHelper = createHooksCalculator(HookPhase.NONE, HookPhase.BLUE_GREEN_APPLICATION_AFTER_STOP_IDLE);
        Module moduleToDeploy = createModule("module-to-deploy");
        Hook afterStopIdleHook = createHook("after-stop-idle", Collections.singletonList("blue-green.application.after-stop.idle"));
        moduleToDeploy.setHooks(Collections.singletonList(afterStopIdleHook));
        Mockito.when(processTypeParser.getProcessType(context.getExecution()))
               .thenReturn(ProcessType.BLUE_GREEN_DEPLOY);
        context.setVariable(Variables.SUBPROCESS_PHASE, SubprocessPhase.BEFORE_APPLICATION_START);
        List<Hook> hooksForCurrentPhase = hooksHelper.calculateHooksForExecution(moduleToDeploy, StepPhase.DONE);
        Assertions.assertEquals(1, hooksForCurrentPhase.size());
        List<String> expectedHookPhases = Collections.singletonList("blue-green.application.after-stop.idle");
        Assertions.assertEquals(expectedHookPhases, hooksForCurrentPhase.get(0)
                                                                        .getPhases());
    }

    @Test
    void testExecuteHooksForPhaseAfterStopLive() {
        HooksCalculator hooksHelper = createHooksCalculator(HookPhase.NONE, HookPhase.BLUE_GREEN_APPLICATION_AFTER_STOP_LIVE);
        Module moduleToDeploy = createModule("module-to-deploy");
        Hook afterStopLiveHook = createHook("after-stop-live", Collections.singletonList("blue-green.application.after-stop.live"));
        moduleToDeploy.setHooks(Collections.singletonList(afterStopLiveHook));
        Mockito.when(processTypeParser.getProcessType(context.getExecution()))
               .thenReturn(ProcessType.BLUE_GREEN_DEPLOY);
        List<Hook> hooksForCurrentPhase = hooksHelper.calculateHooksForExecution(moduleToDeploy, StepPhase.DONE);
        Assertions.assertEquals(1, hooksForCurrentPhase.size());
        List<String> expectedHookPhases = Collections.singletonList("blue-green.application.after-stop.live");
        Assertions.assertEquals(expectedHookPhases, hooksForCurrentPhase.get(0)
                                                                        .getPhases());
    }

    @Test
    void testExecuteHooksForPhaseBeforeUnmapRoutes() {
        HooksCalculator hooksHelper = createHooksCalculator(HookPhase.BLUE_GREEN_APPLICATION_BEFORE_UNMAP_ROUTES_LIVE, HookPhase.NONE);
        Module moduleToDeploy = createModule("module-to-deploy");
        Hook afterUnmapRoutesHook = createHook("before-unmap-routes",
                                               Collections.singletonList("blue-green.application.before-unmap-routes.live"));
        moduleToDeploy.setHooks(Collections.singletonList(afterUnmapRoutesHook));
        Mockito.when(processTypeParser.getProcessType(context.getExecution()))
               .thenReturn(ProcessType.BLUE_GREEN_DEPLOY);
        List<Hook> hooksForCurrentPhase = hooksHelper.calculateHooksForExecution(moduleToDeploy, StepPhase.EXECUTE);
        Assertions.assertEquals(1, hooksForCurrentPhase.size());
        List<String> expectedHookPhases = Collections.singletonList("blue-green.application.before-unmap-routes.live");
        Assertions.assertEquals(expectedHookPhases, hooksForCurrentPhase.get(0)
                                                                        .getPhases());
    }

    @Test
    void testExecuteHooksForPhaseNoHooks() {
        HooksCalculator hooksHelper = createHooksCalculator(HookPhase.NONE, HookPhase.NONE);
        Module moduleToDeploy = createModule("module-to-deploy");
        Mockito.when(processTypeParser.getProcessType(context.getExecution()))
               .thenReturn(ProcessType.DEPLOY);
        List<Hook> hooksForCurrentPhase = hooksHelper.calculateHooksForExecution(moduleToDeploy, StepPhase.POLL);
        Assertions.assertEquals(0, hooksForCurrentPhase.size());
    }

    private HooksCalculator createHooksCalculator(HookPhase hookPhaseBeforeStep, HookPhase hookPhaseAfterStep) {
        return ImmutableHooksCalculator.builder()
                                       .context(context)
                                       .hookPhasesBeforeStep(Collections.singletonList(hookPhaseBeforeStep))
                                       .hookPhasesAfterStep(Collections.singletonList(hookPhaseAfterStep))
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
