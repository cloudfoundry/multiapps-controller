package com.sap.cloud.lm.sl.cf.process.util;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.sap.cloud.lm.sl.cf.core.model.HistoricOperationEvent.EventType;
import com.sap.cloud.lm.sl.cf.core.model.ImmutableHistoricOperationEvent;
import com.sap.cloud.lm.sl.cf.core.persistence.query.HistoricOperationEventQuery;
import com.sap.cloud.lm.sl.cf.core.persistence.service.HistoricOperationEventService;
import com.sap.cloud.lm.sl.cf.process.flowable.FlowableFacade;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation.State;

public class ProcessHelperTest {

    private static final String PROCESS_ID = "79af8e1e-4d96-11ea-b77f-2e728ce88125";

    @Mock
    private FlowableFacade flowableFacade;

    @Mock
    private HistoricOperationEventService historicOperationEventService;

    @Mock
    private HistoricOperationEventQuery historicOperationEventQuery;

    private final ProcessHelper processHelper;

    public ProcessHelperTest() {
        MockitoAnnotations.initMocks(this);
        processHelper = new ProcessHelper(flowableFacade, historicOperationEventService);
    }

    @BeforeEach
    public void setUp() {
        Mockito.when(historicOperationEventService.createQuery())
               .thenReturn(historicOperationEventQuery);
        Mockito.when(historicOperationEventQuery.processId(PROCESS_ID))
               .thenReturn(historicOperationEventQuery);
    }

    @Test
    public void testIsProcessAtReceiveTask() {
        Mockito.when(flowableFacade.isProcessInstanceAtReceiveTask(PROCESS_ID))
               .thenReturn(true);
        Assertions.assertEquals(State.ACTION_REQUIRED, processHelper.computeProcessState(PROCESS_ID));
    }

    @Test
    public void testIsProcessInErrorState() {
        Mockito.when(historicOperationEventQuery.list())
               .thenReturn(Arrays.asList(ImmutableHistoricOperationEvent.builder()
                                                                        .type(EventType.FAILED_BY_CONTENT_ERROR)
                                                                        .processId(PROCESS_ID)
                                                                        .build()));
        Assertions.assertEquals(State.ERROR, processHelper.computeProcessState(PROCESS_ID));
    }

    @Test
    public void testIsProcessAbortedWhenThereIsAbortedProcess() {
        Mockito.when(historicOperationEventQuery.list())
               .thenReturn(Arrays.asList(ImmutableHistoricOperationEvent.builder()
                                                                        .type(EventType.ABORTED)
                                                                        .processId(PROCESS_ID)
                                                                        .build()));
        Assertions.assertEquals(State.ABORTED, processHelper.computeProcessState(PROCESS_ID));
    }

    @Test
    public void testIsProcessAbortedWhenThereIsNotAbortedProcess() {
        mockHistoricEventsWithTypes(EventType.FINISHED);
        Assertions.assertEquals(State.FINISHED, processHelper.computeProcessState(PROCESS_ID));
    }

    private void mockHistoricEventsWithTypes(EventType type) {
        Mockito.when(historicOperationEventQuery.list())
               .thenReturn(Arrays.asList(ImmutableHistoricOperationEvent.builder()
                                                                        .type(type)
                                                                        .processId(PROCESS_ID)
                                                                        .build()));
    }


}
