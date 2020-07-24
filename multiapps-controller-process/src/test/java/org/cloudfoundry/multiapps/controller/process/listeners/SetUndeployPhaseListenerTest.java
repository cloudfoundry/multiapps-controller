package org.cloudfoundry.multiapps.controller.process.listeners;

import org.cloudfoundry.multiapps.controller.core.model.Phase;
import org.cloudfoundry.multiapps.controller.process.mock.MockDelegateExecution;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SetUndeployPhaseListenerTest {

    private final DelegateExecution delegateExecution = MockDelegateExecution.createSpyInstance();
    private final ExecutionListener setUndeployPhaseListener = new SetUndeployPhaseListener();

    @Test
    void testNotify() {
        setUndeployPhaseListener.notify(delegateExecution);
        Assertions.assertEquals(Phase.UNDEPLOY.toString(), delegateExecution.getVariable(Variables.PHASE.getName()));
    }
}
