package org.cloudfoundry.multiapps.controller.process.listeners;

import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.process.mock.MockDelegateExecution;
import org.cloudfoundry.multiapps.controller.process.util.OperationInFinalStateHandler;
import org.cloudfoundry.multiapps.controller.process.variables.VariableHandling;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class EndProcessListenerTest {

    private final OperationInFinalStateHandler eventHandler = Mockito.mock(OperationInFinalStateHandler.class);
    private final DelegateExecution execution = MockDelegateExecution.createSpyInstance();

    @Test
    public void testNotifyInternal() {
        EndProcessListener endProcessListener = new EndProcessListener(eventHandler);
        // set the process as root process
        VariableHandling.set(execution, Variables.CORRELATION_ID, execution.getProcessInstanceId());
        endProcessListener.notifyInternal(execution);
        Mockito.verify(eventHandler)
               .handle(execution, Operation.State.FINISHED);
    }

}
