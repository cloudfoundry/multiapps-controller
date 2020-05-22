package com.sap.cloud.lm.sl.cf.process.util;

import java.util.Collections;
import java.util.List;

import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.core.cf.CloudControllerClientProvider;
import com.sap.cloud.lm.sl.cf.core.model.HookPhase;
import com.sap.cloud.lm.sl.cf.process.mock.MockDelegateExecution;
import com.sap.cloud.lm.sl.cf.process.steps.ProcessContext;
import com.sap.cloud.lm.sl.cf.process.steps.StepPhase;
import com.sap.cloud.lm.sl.mta.model.Hook;
import com.sap.cloud.lm.sl.mta.model.Module;

class HooksCalculatorTest {

    private final ProcessContext context = createContext();

    @Test
    void testExecuteHooksForPhaseBeforeStartIdle() {
        HooksCalculator hooksHelper = createHooksCalculator(HookPhase.APPLICATION_BEFORE_START_IDLE, HookPhase.NONE);
        Module moduleToDeploy = createModule("module-to-deploy");
        Hook beforeStartIdleHook = createHook("before-start-idle", Collections.singletonList("application.before-start.idle"));
        moduleToDeploy.setHooks(Collections.singletonList(beforeStartIdleHook));
        List<Hook> hooksForCurrentPhase = hooksHelper.calculateHooksForExecution(moduleToDeploy, StepPhase.EXECUTE);
        Assertions.assertEquals(1, hooksForCurrentPhase.size());
        Assertions.assertEquals(1, hooksForCurrentPhase.get(0)
                .getPhases()
                .size());
        Assertions.assertEquals(HookPhase.APPLICATION_BEFORE_START_IDLE.getValue(), hooksForCurrentPhase.get(0)
                .getPhases()
                .get(0));
    }

    @Test
    void testExecuteHooksForPhaseBeforeStartLive() {
        HooksCalculator hooksHelper = createHooksCalculator(HookPhase.APPLICATION_BEFORE_START_LIVE, HookPhase.NONE);
        Module moduleToDeploy = createModule("module-to-deploy");
        Hook beforeStartLiveHook = createHook("before-start-live", Collections.singletonList("application.before-start.live"));
        moduleToDeploy.setHooks(Collections.singletonList(beforeStartLiveHook));
        List<Hook> hooksForCurrentPhase = hooksHelper.calculateHooksForExecution(moduleToDeploy, StepPhase.EXECUTE);
        Assertions.assertEquals(1, hooksForCurrentPhase.size());
        Assertions.assertEquals(1, hooksForCurrentPhase.get(0)
                                                       .getPhases()
                                                       .size());
        Assertions.assertEquals(HookPhase.APPLICATION_BEFORE_START_LIVE.getValue(), hooksForCurrentPhase.get(0)
                                                                                                        .getPhases()
                                                                                                        .get(0));
    }

    @Test
    void testExecuteHooksForPhaseBeforeStopIdle() {
        HooksCalculator hooksHelper = createHooksCalculator(HookPhase.APPLICATION_BEFORE_STOP_IDLE, HookPhase.NONE);
        Module moduleToDeploy = createModule("module-to-deploy");
        Hook beforeStopIdleHook = createHook("before-start", Collections.singletonList("application.before-stop.idle"));
        moduleToDeploy.setHooks(Collections.singletonList(beforeStopIdleHook));
        List<Hook> hooksForCurrentPhase = hooksHelper.calculateHooksForExecution(moduleToDeploy, StepPhase.EXECUTE);
        Assertions.assertEquals(1, hooksForCurrentPhase.size());
        Assertions.assertEquals(1, hooksForCurrentPhase.get(0)
                                                       .getPhases()
                                                       .size());
        Assertions.assertEquals(HookPhase.APPLICATION_BEFORE_STOP_IDLE.getValue(), hooksForCurrentPhase.get(0)
                                                                                                       .getPhases()
                                                                                                       .get(0));
    }

    @Test
    void testExecuteHooksForPhaseBeforeStopLive() {
        HooksCalculator hooksHelper = createHooksCalculator(HookPhase.APPLICATION_BEFORE_STOP_LIVE, HookPhase.NONE);
        Module moduleToDeploy = createModule("module-to-deploy");
        Hook beforeStopLiveHook = createHook("before-start", Collections.singletonList("application.before-stop.live"));
        moduleToDeploy.setHooks(Collections.singletonList(beforeStopLiveHook));
        List<Hook> hooksForCurrentPhase = hooksHelper.calculateHooksForExecution(moduleToDeploy, StepPhase.EXECUTE);
        Assertions.assertEquals(1, hooksForCurrentPhase.size());
        Assertions.assertEquals(1, hooksForCurrentPhase.get(0)
                                                       .getPhases()
                                                       .size());
        Assertions.assertEquals(HookPhase.APPLICATION_BEFORE_STOP_LIVE.getValue(), hooksForCurrentPhase.get(0)
                                                                                                       .getPhases()
                                                                                                       .get(0));
    }

    @Test
    void testExecuteHooksForPhaseAfterStopLive() {
        HooksCalculator hooksHelper = createHooksCalculator(HookPhase.NONE, HookPhase.APPLICATION_AFTER_STOP_LIVE);
        Module moduleToDeploy = createModule("module-to-deploy");
        Hook afterStopLiveHook = createHook("before-start", Collections.singletonList("application.after-stop.live"));
        moduleToDeploy.setHooks(Collections.singletonList(afterStopLiveHook));
        List<Hook> hooksForCurrentPhase = hooksHelper.calculateHooksForExecution(moduleToDeploy, StepPhase.DONE);
        Assertions.assertEquals(1, hooksForCurrentPhase.size());
        Assertions.assertEquals(1, hooksForCurrentPhase.get(0)
                                                       .getPhases()
                                                       .size());
        Assertions.assertEquals(HookPhase.APPLICATION_AFTER_STOP_LIVE.getValue(), hooksForCurrentPhase.get(0)
                                                                                                      .getPhases()
                                                                                                      .get(0));
    }

    @Test
    void testExecuteHooksForPhaseAfterStopIdle() {
        HooksCalculator hooksHelper = createHooksCalculator(HookPhase.NONE, HookPhase.APPLICATION_AFTER_STOP_IDLE);
        Module moduleToDeploy = createModule("module-to-deploy");
        Hook afterStopIdleHook = createHook("before-start", Collections.singletonList("application.after-stop.idle"));
        moduleToDeploy.setHooks(Collections.singletonList(afterStopIdleHook));
        List<Hook> hooksForCurrentPhase = hooksHelper.calculateHooksForExecution(moduleToDeploy, StepPhase.DONE);
        Assertions.assertEquals(1, hooksForCurrentPhase.size());
        Assertions.assertEquals(1, hooksForCurrentPhase.get(0)
                                                       .getPhases()
                                                       .size());
        Assertions.assertEquals(HookPhase.APPLICATION_AFTER_STOP_IDLE.getValue(), hooksForCurrentPhase.get(0)
                                                                                                      .getPhases()
                                                                                                      .get(0));
    }

    @Test
    void testExecuteHooksForPhaseBeforeUnmapRoutes() {
        HooksCalculator hooksHelper = createHooksCalculator(HookPhase.APPLICATION_BEFORE_UNMAP_ROUTES, HookPhase.NONE);
        Module moduleToDeploy = createModule("module-to-deploy");
        Hook afterUnmapRoutesHook = createHook("before-start", Collections.singletonList("application.before-unmap-routes"));
        moduleToDeploy.setHooks(Collections.singletonList(afterUnmapRoutesHook));
        List<Hook> hooksForCurrentPhase = hooksHelper.calculateHooksForExecution(moduleToDeploy, StepPhase.EXECUTE);
        Assertions.assertEquals(1, hooksForCurrentPhase.size());
        Assertions.assertEquals(1, hooksForCurrentPhase.get(0)
                                                       .getPhases()
                                                       .size());
        Assertions.assertEquals(HookPhase.APPLICATION_BEFORE_UNMAP_ROUTES.getValue(), hooksForCurrentPhase.get(0)
                                                                                                          .getPhases()
                                                                                                          .get(0));
    }

    @Test
    void testExecuteHooksForPhaseNoHooks() {
        HooksCalculator hooksHelper = createHooksCalculator(HookPhase.APPLICATION_BEFORE_STOP_LIVE, HookPhase.NONE);
        Module moduleToDeploy = createModule("module-to-deploy");
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
