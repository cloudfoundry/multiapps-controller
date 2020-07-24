package com.sap.cloud.lm.sl.cf.process.listeners;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.sap.cloud.lm.sl.cf.core.model.SubprocessPhase;
import com.sap.cloud.lm.sl.cf.process.mock.MockDelegateExecution;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;

class SetBeforeApplicationStartPhaseListenerTest {

    private final DelegateExecution delegateExecution = MockDelegateExecution.createSpyInstance();
    private final ExecutionListener setBeforeApplicationStartPhaseListener = new SetBeforeApplicationStartPhaseListener();

    @Test
    void testNotify() {
        setBeforeApplicationStartPhaseListener.notify(delegateExecution);
        Assertions.assertEquals(SubprocessPhase.BEFORE_APPLICATION_START.toString(),
                                delegateExecution.getVariable(Variables.SUBPROCESS_PHASE.getName()));
    }
}
