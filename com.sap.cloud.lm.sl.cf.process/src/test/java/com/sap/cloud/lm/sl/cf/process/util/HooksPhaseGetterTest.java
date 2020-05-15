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
import com.sap.cloud.lm.sl.cf.process.steps.StopAppStep;
import com.sap.cloud.lm.sl.cf.process.steps.SyncFlowableStep;

class HooksPhaseGetterTest {

    private final HooksPhaseGetter hooksPhaseGetter = new HooksPhaseGetter();
    private final ProcessContext context = createContext();

    @Test
    void testGetHookPhasesBeforeStopNonHooksClass() {
        SyncFlowableStep syncFlowableStep = Mockito.mock(SyncFlowableStep.class);
        List<HookPhase> hookPhasesBeforeStop = hooksPhaseGetter.getHookPhasesBeforeStop(syncFlowableStep, context);
        Assertions.assertEquals(1, hookPhasesBeforeStop.size());
        Assertions.assertEquals(HookPhase.NONE, hookPhasesBeforeStop.get(0));
    }

    @Test
    void testGetHookPhasesAfterStopNonHooksClass() {
        SyncFlowableStep syncFlowableStep = Mockito.mock(SyncFlowableStep.class);
        List<HookPhase> hookPhasesAfterStop = hooksPhaseGetter.getHookPhasesAfterStop(syncFlowableStep, context);
        Assertions.assertEquals(1, hookPhasesAfterStop.size());
        Assertions.assertEquals(HookPhase.NONE, hookPhasesAfterStop.get(0));
    }

    @Test
    void testGetHookPhasesBeforeStop() {
        List<HookPhase> hookPhases = Collections.singletonList(HookPhase.APPLICATION_BEFORE_STOP_IDLE);
        StopAppStep beforeStepHookPhaseProvider = Mockito.mock(StopAppStep.class);
        Mockito.when(beforeStepHookPhaseProvider.getHookPhasesBeforeStep(context))
               .thenReturn(hookPhases);
        List<HookPhase> hookPhasesBeforeStop = hooksPhaseGetter.getHookPhasesBeforeStop(beforeStepHookPhaseProvider, context);
        Assertions.assertEquals(1, hookPhasesBeforeStop.size());
        Assertions.assertEquals(hookPhases, hookPhasesBeforeStop);
    }

    @Test
    void testGetHookPhasesAfterStop() {
        List<HookPhase> hookPhases = Collections.singletonList(HookPhase.APPLICATION_AFTER_STOP_IDLE);
        StopAppStep afterStepHookPhaseProvider = Mockito.mock(StopAppStep.class);
        Mockito.when(afterStepHookPhaseProvider.getHookPhasesAfterStep(context))
               .thenReturn(hookPhases);
        List<HookPhase> hookPhasesBeforeStop = hooksPhaseGetter.getHookPhasesAfterStop(afterStepHookPhaseProvider, context);
        Assertions.assertEquals(1, hookPhasesBeforeStop.size());
        Assertions.assertEquals(hookPhases, hookPhasesBeforeStop);
    }

    private ProcessContext createContext() {
        DelegateExecution delegateExecution = MockDelegateExecution.createSpyInstance();
        StepLogger stepLogger = Mockito.mock(StepLogger.class);
        CloudControllerClientProvider cloudControllerClientProvider = Mockito.mock(CloudControllerClientProvider.class);
        return new ProcessContext(delegateExecution, stepLogger, cloudControllerClientProvider);
    }
}
