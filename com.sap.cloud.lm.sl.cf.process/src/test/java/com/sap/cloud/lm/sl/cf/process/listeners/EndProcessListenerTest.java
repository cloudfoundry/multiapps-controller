package com.sap.cloud.lm.sl.cf.process.listeners;

import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.mock.MockDelegateExecution;
import com.sap.cloud.lm.sl.cf.process.util.OperationInFinalStateHandler;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;

public class EndProcessListenerTest {

    private final OperationInFinalStateHandler eventHandler = Mockito.mock(OperationInFinalStateHandler.class);
    private final DelegateExecution execution = MockDelegateExecution.createSpyInstance();

    @Test
    public void testNotifyInternal() {
        EndProcessListener endProcessListener = new EndProcessListener(eventHandler);
        // set the process as root process
        execution.setVariable(Constants.VAR_CORRELATION_ID, execution.getProcessInstanceId());
        endProcessListener.notifyInternal(execution);
        Mockito.verify(eventHandler)
               .handle(execution, Operation.State.FINISHED);
    }

}
