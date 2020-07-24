package org.cloudfoundry.multiapps.controller.process.listeners;

import org.cloudfoundry.multiapps.controller.core.model.SubprocessPhase;
import org.cloudfoundry.multiapps.controller.process.mock.MockDelegateExecution;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
