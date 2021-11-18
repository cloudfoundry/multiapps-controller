package org.cloudfoundry.multiapps.controller.process.util;

import java.util.List;

import org.cloudfoundry.multiapps.controller.api.model.Operation.State;
import org.cloudfoundry.multiapps.controller.persistence.OrderDirection;
import org.cloudfoundry.multiapps.controller.persistence.model.HistoricOperationEvent;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableHistoricOperationEvent;
import org.cloudfoundry.multiapps.controller.persistence.query.HistoricOperationEventQuery;
import org.cloudfoundry.multiapps.controller.persistence.services.HistoricOperationEventService;
import org.cloudfoundry.multiapps.controller.process.flowable.FlowableFacade;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class ProcessHelperTest {

    private static final String PROCESS_ID = "79af8e1e-4d96-11ea-b77f-2e728ce88125";

    @Mock
    private FlowableFacade flowableFacade;

    @Mock
    private HistoricOperationEventService historicOperationEventService;

    @Mock
    private HistoricOperationEventQuery historicOperationEventQuery;

    private ProcessHelper processHelper;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        processHelper = new ProcessHelper(flowableFacade, historicOperationEventService);
        Mockito.when(historicOperationEventService.createQuery())
               .thenReturn(historicOperationEventQuery);
        Mockito.when(historicOperationEventQuery.processId(PROCESS_ID))
               .thenReturn(historicOperationEventQuery);
        Mockito.when(historicOperationEventQuery.orderByTimestamp(Mockito.any()))
               .thenReturn(historicOperationEventQuery);
    }

    @Test
    void testIsProcessAtReceiveTask() {
        Mockito.when(flowableFacade.isProcessInstanceAtReceiveTask(PROCESS_ID))
               .thenReturn(true);
        Assertions.assertEquals(State.ACTION_REQUIRED, processHelper.computeProcessState(PROCESS_ID));
    }

    @Test
    void testIsProcessInErrorState() {
        Mockito.when(flowableFacade.hasDeadLetterJobs(PROCESS_ID))
               .thenReturn(true);
        Assertions.assertEquals(State.ERROR, processHelper.computeProcessState(PROCESS_ID));
    }

    @Test
    void testIsProcessAbortedWhenThereIsAbortedProcess() {
        Mockito.when(historicOperationEventQuery.list())
               .thenReturn(List.of(ImmutableHistoricOperationEvent.builder()
                                                                  .type(HistoricOperationEvent.EventType.ABORTED)
                                                                  .processId(PROCESS_ID)
                                                                  .build()));
        Assertions.assertEquals(State.ABORTED, processHelper.computeProcessState(PROCESS_ID));
    }

    @Test
    void testIsProcessAbortedWhenThereIsNotAbortedProcess() {
        mockHistoricEventsWithTypes(HistoricOperationEvent.EventType.FINISHED);
        Assertions.assertEquals(State.FINISHED, processHelper.computeProcessState(PROCESS_ID));
    }

    @Test
    void isGetHistoricOperationEventOrdered() {
        mockHistoricEventsWithTypes(HistoricOperationEvent.EventType.FINISHED);
        processHelper.getHistoricOperationEventByProcessId(PROCESS_ID);
        Mockito.verify(historicOperationEventQuery)
               .orderByTimestamp(OrderDirection.ASCENDING);
    }

    private void mockHistoricEventsWithTypes(HistoricOperationEvent.EventType type) {
        Mockito.when(historicOperationEventQuery.list())
               .thenReturn(List.of(ImmutableHistoricOperationEvent.builder()
                                                                  .type(type)
                                                                  .processId(PROCESS_ID)
                                                                  .build()));
    }

}
