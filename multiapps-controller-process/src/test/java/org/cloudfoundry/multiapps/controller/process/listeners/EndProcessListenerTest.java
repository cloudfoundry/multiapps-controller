package org.cloudfoundry.multiapps.controller.process.listeners;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.api.model.ProcessType;
import org.cloudfoundry.multiapps.controller.process.dynatrace.DynatraceProcessEvent;
import org.cloudfoundry.multiapps.controller.process.dynatrace.DynatracePublisher;
import org.cloudfoundry.multiapps.controller.process.util.MockDelegateExecution;
import org.cloudfoundry.multiapps.controller.process.util.OperationInFinalStateHandler;
import org.cloudfoundry.multiapps.controller.process.util.ProcessTypeParser;
import org.cloudfoundry.multiapps.controller.process.variables.VariableHandling;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class EndProcessListenerTest {

    private final OperationInFinalStateHandler eventHandler = Mockito.mock(OperationInFinalStateHandler.class);
    private final DynatracePublisher dynatracePublisher = Mockito.mock(DynatracePublisher.class);
    private final ProcessTypeParser processTypeParser = Mockito.mock(ProcessTypeParser.class);
    private final DelegateExecution execution = MockDelegateExecution.createSpyInstance();

    private final static String SPACE_ID = "9ba1dfc7-9c2c-40d5-8bf9-fd04fa7a1722";
    private final static String MTA_ID = "my-mta";
    private final static ProcessType PROCESS_TYPE = ProcessType.DEPLOY;

    @Test
    void testNotifyInternal() {
        EndProcessListener endProcessListener = new EndProcessListener(eventHandler, dynatracePublisher, processTypeParser);
        // set the process as root process
        VariableHandling.set(execution, Variables.CORRELATION_ID, execution.getProcessInstanceId());
        VariableHandling.set(execution, Variables.SPACE_GUID, SPACE_ID);
        VariableHandling.set(execution, Variables.MTA_ID, MTA_ID);
        Mockito.when(processTypeParser.getProcessType(execution, false))
               .thenReturn(PROCESS_TYPE);
        endProcessListener.notifyInternal(execution);
        Mockito.verify(eventHandler)
               .handle(execution, ProcessType.DEPLOY, Operation.State.FINISHED);
        ArgumentCaptor<DynatraceProcessEvent> argumentCaptor = ArgumentCaptor.forClass(DynatraceProcessEvent.class);
        Mockito.verify(dynatracePublisher)
               .publishProcessEvent(argumentCaptor.capture(), Mockito.any());
        DynatraceProcessEvent actualDynatraceEvent = argumentCaptor.getValue();
        assertEquals(MTA_ID, actualDynatraceEvent.getMtaId());
        assertEquals(SPACE_ID, actualDynatraceEvent.getSpaceId());
        assertEquals(PROCESS_TYPE, actualDynatraceEvent.getProcessType());
        assertEquals(DynatraceProcessEvent.EventType.FINISHED, actualDynatraceEvent.getEventType());
    }

}
