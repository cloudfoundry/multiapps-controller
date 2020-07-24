package org.cloudfoundry.multiapps.controller.process.listeners;

import org.cloudfoundry.multiapps.controller.core.model.SubprocessPhase;
import org.cloudfoundry.multiapps.controller.process.mock.MockDelegateExecution;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
