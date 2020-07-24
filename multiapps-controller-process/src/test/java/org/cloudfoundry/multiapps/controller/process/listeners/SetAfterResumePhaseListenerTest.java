package org.cloudfoundry.multiapps.controller.process.listeners;

import org.cloudfoundry.multiapps.controller.core.model.Phase;
import org.cloudfoundry.multiapps.controller.process.mock.MockDelegateExecution;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SetAfterResumePhaseListenerTest {

    private final ExecutionListener setResumePhaseListener = new SetAfterResumePhaseListener();
    private final DelegateExecution delegateExecution = MockDelegateExecution.createSpyInstance();

    @Test
    void testNotify() {
        setResumePhaseListener.notify(delegateExecution);
        Assertions.assertEquals(Phase.AFTER_RESUME.toString(), delegateExecution.getVariable(Variables.PHASE.getName()));
    }
}
