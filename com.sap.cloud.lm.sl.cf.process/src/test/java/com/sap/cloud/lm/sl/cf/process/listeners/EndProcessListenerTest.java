package com.sap.cloud.lm.sl.cf.process.listeners;

import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.sap.cloud.lm.sl.cf.process.mock.MockDelegateExecution;
import com.sap.cloud.lm.sl.cf.process.util.OperationInFinalStateHandler;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;

public class EndProcessListenerTest {

    private final OperationInFinalStateHandler eventHandler = Mockito.mock(OperationInFinalStateHandler.class);
    private final DelegateExecution context = MockDelegateExecution.createSpyInstance();

    @Test
    public void testNotifyInternal() {
        EndProcessListener endProcessListener = new EndProcessListener(eventHandler);
        endProcessListener.notifyInternal(context);
        Mockito.verify(eventHandler)
               .handle(context, Operation.State.FINISHED);
    }

}
