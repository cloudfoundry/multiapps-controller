package com.sap.cloud.lm.sl.cf.process.listeners;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.sap.cloud.lm.sl.cf.core.model.Phase;
import com.sap.cloud.lm.sl.cf.process.mock.MockDelegateExecution;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;

class SetAfterResumePhaseListenerTest {

    private final ExecutionListener setResumePhaseListener = new SetAfterResumePhaseListener();
    private final DelegateExecution delegateExecution = MockDelegateExecution.createSpyInstance();

    @Test
    void testNotify() {
        setResumePhaseListener.notify(delegateExecution);
        Assertions.assertEquals(Phase.AFTER_RESUME.toString(), delegateExecution.getVariable(Variables.PHASE.getName()));
    }
}
