package com.sap.cloud.lm.sl.cf.process.util;

import java.util.Collections;
import java.util.List;

import org.flowable.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.sap.cloud.lm.sl.cf.core.model.HistoricOperationEvent;
import com.sap.cloud.lm.sl.cf.core.persistence.query.HistoricOperationEventQuery;
import com.sap.cloud.lm.sl.cf.core.persistence.service.HistoricOperationEventService;
import com.sap.cloud.lm.sl.cf.process.flowable.FlowableFacade;

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
        Assertions.assertTrue(processHelper.isAtReceiveTask(PROCESS_ID));
    }

    @Test
    public void testIsProcessInErrorState() {
        Mockito.when(flowableFacade.hasDeadLetterJobs(PROCESS_ID))
               .thenReturn(true);
        Assertions.assertTrue(processHelper.isInErrorState(PROCESS_ID));
    }

    @Test
    public void testIsProcessAbortedWhenThereIsAbortedProcess() {
        List<HistoricOperationEvent> mockedHistoricOperationEvents = getMockedHistoricOperationEventsWithEventType(HistoricOperationEvent.EventType.ABORTED);
        Mockito.when(historicOperationEventQuery.list())
               .thenReturn(mockedHistoricOperationEvents);
        Assertions.assertTrue(processHelper.isAborted(PROCESS_ID));
    }

    @Test
    public void testIsProcessAbortedWhenThereIsNotAbortedProcess() {
        List<HistoricOperationEvent> mockedHistoricOperationEvents = getMockedHistoricOperationEventsWithEventType(HistoricOperationEvent.EventType.FINISHED);
        Mockito.when(historicOperationEventQuery.list())
               .thenReturn(mockedHistoricOperationEvents);
        Assertions.assertFalse(processHelper.isAborted(PROCESS_ID));
    }

    @Test
    public void testFindProcessInstanceByIdWithValidProcessInstanceId() {
        ProcessInstance processInstance = Mockito.mock(ProcessInstance.class);
        Mockito.when(flowableFacade.getProcessInstance(PROCESS_ID))
               .thenReturn(processInstance);
        Assertions.assertEquals(processInstance, processHelper.findProcessInstanceById(PROCESS_ID)
                                                              .get());
    }

    @Test
    public void testFindProcessInstanceByIdWithInvalidProcessInstanceId() {
        Assertions.assertFalse(processHelper.findProcessInstanceById("invalid")
                                            .isPresent());
    }

    private List<HistoricOperationEvent> getMockedHistoricOperationEventsWithEventType(HistoricOperationEvent.EventType eventType) {
        HistoricOperationEvent historicOperationEvent = Mockito.mock(HistoricOperationEvent.class);
        Mockito.when(historicOperationEvent.getType())
               .thenReturn(eventType);
        return Collections.singletonList(historicOperationEvent);
    }

}
