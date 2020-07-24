package com.sap.cloud.lm.sl.cf.process.listeners;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.sap.cloud.lm.sl.cf.core.model.Phase;
import com.sap.cloud.lm.sl.cf.process.mock.MockDelegateExecution;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;

class SetUndeployPhaseListenerTest {

    private final DelegateExecution delegateExecution = MockDelegateExecution.createSpyInstance();
    private final ExecutionListener setUndeployPhaseListener = new SetUndeployPhaseListener();

    @Test
    void testNotify() {
        setUndeployPhaseListener.notify(delegateExecution);
        Assertions.assertEquals(Phase.UNDEPLOY.toString(), delegateExecution.getVariable(Variables.PHASE.getName()));
    }
}
