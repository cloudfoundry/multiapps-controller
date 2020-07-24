package com.sap.cloud.lm.sl.cf.process.listeners;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.sap.cloud.lm.sl.cf.core.model.SubprocessPhase;
import com.sap.cloud.lm.sl.cf.process.mock.MockDelegateExecution;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;

class SetBeforeApplicationStopPhaseListenerTest {

    private final DelegateExecution delegateExecution = MockDelegateExecution.createSpyInstance();
    private final ExecutionListener setBeforeApplicationStopPhaseListener = new SetBeforeApplicationStopPhaseListener();

    @Test
    void testNotify() {
        setBeforeApplicationStopPhaseListener.notify(delegateExecution);
        Assertions.assertEquals(SubprocessPhase.BEFORE_APPLICATION_STOP.toString(),
                                delegateExecution.getVariable(Variables.SUBPROCESS_PHASE.getName()));
    }
}
