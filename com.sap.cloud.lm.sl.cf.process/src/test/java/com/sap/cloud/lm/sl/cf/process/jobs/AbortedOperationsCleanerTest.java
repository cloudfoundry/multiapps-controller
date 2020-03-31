package com.sap.cloud.lm.sl.cf.process.jobs;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.sap.cloud.lm.sl.cf.core.model.HistoricOperationEvent;
import com.sap.cloud.lm.sl.cf.core.model.HistoricOperationEvent.EventType;
import com.sap.cloud.lm.sl.cf.core.model.ImmutableHistoricOperationEvent;
import com.sap.cloud.lm.sl.cf.core.persistence.query.HistoricOperationEventQuery;
import com.sap.cloud.lm.sl.cf.core.persistence.query.OperationQuery;
import com.sap.cloud.lm.sl.cf.core.persistence.service.HistoricOperationEventService;
import com.sap.cloud.lm.sl.cf.core.persistence.service.OperationService;
import com.sap.cloud.lm.sl.cf.process.flowable.FlowableFacade;
import com.sap.cloud.lm.sl.cf.web.api.model.ImmutableOperation;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation.State;

public class AbortedOperationsCleanerTest {

    @Mock
    private HistoricOperationEventService historicOperationEventService;
    @Mock
    private FlowableFacade flowableFacade;
    @Mock
    private OperationService operationService;
    @InjectMocks
    private AbortedOperationsCleaner abortedOperationsCleaner;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testExecuteWithNoAbortedOperations() {
        prepareMocksWithOperations(Collections.emptyList());

        abortedOperationsCleaner.execute(new Date()); // Passed argument is not used.

        Mockito.verify(historicOperationEventService)
               .createQuery();
        Mockito.verifyNoInteractions(flowableFacade, operationService);
    }

    @Test
    public void testExecuteWithOperationsInFinalState() {
        prepareMocksWithOperations(Arrays.asList(createOperation("foo", Operation.State.FINISHED),
                                                 createOperation("bar", Operation.State.FINISHED)));

        abortedOperationsCleaner.execute(new Date()); // Passed argument is not used.

        Mockito.verify(operationService, Mockito.times(2))
               .createQuery();
        Mockito.verifyNoInteractions(flowableFacade);
    }

    @Test
    public void testExecute() {
        prepareMocksWithOperations(Arrays.asList(createOperation("foo", null), createOperation("bar", null)));

        abortedOperationsCleaner.execute(new Date()); // Passed argument is not used.

        Mockito.verify(operationService, Mockito.times(2))
               .createQuery();
        Mockito.verify(flowableFacade)
               .deleteProcessInstance("foo", Operation.State.ABORTED.name());
        Mockito.verify(flowableFacade)
               .deleteProcessInstance("bar", Operation.State.ABORTED.name());
    }

    @Test
    public void testExecuteWithMixedOperations() {
        prepareMocksWithOperations(Arrays.asList(createOperation("foo", null), createOperation("bar", Operation.State.FINISHED)));

        abortedOperationsCleaner.execute(new Date()); // Passed argument is not used.

        Mockito.verify(operationService, Mockito.times(2))
               .createQuery();
        Mockito.verify(flowableFacade)
               .deleteProcessInstance("foo", Operation.State.ABORTED.name());
        Mockito.verify(flowableFacade, Mockito.never())
               .deleteProcessInstance("bar", Operation.State.ABORTED.name());
    }

    private void prepareMocksWithOperations(List<Operation> operations) {
        HistoricOperationEventQuery historicOperationEventQuery = Mockito.mock(HistoricOperationEventQuery.class, Mockito.RETURNS_SELF);
        Mockito.when(historicOperationEventQuery.list())
               .thenReturn(createAbortedEvents(operations));
        Mockito.when(historicOperationEventService.createQuery())
               .thenReturn(historicOperationEventQuery);

        OperationQuery commonOperationQuery = Mockito.mock(OperationQuery.class, Mockito.RETURNS_SELF);
        Mockito.when(operationService.createQuery())
               .thenReturn(commonOperationQuery);
        for (Operation operation : operations) {
            OperationQuery operationQuery = Mockito.mock(OperationQuery.class, Mockito.RETURNS_SELF);
            Mockito.doReturn(operation)
                   .when(operationQuery)
                   .singleResult();
            Mockito.when(commonOperationQuery.processId(operation.getProcessId()))
                   .thenReturn(operationQuery);
        }
    }

    private List<HistoricOperationEvent> createAbortedEvents(List<Operation> operations) {
        return operations.stream()
                         .map(this::createAbortedEvent)
                         .collect(Collectors.toList());
    }

    private HistoricOperationEvent createAbortedEvent(Operation operation) {
        return ImmutableHistoricOperationEvent.builder()
                                              .processId(operation.getProcessId())
                                              .type(EventType.ABORTED)
                                              .build();
    }

    private Operation createOperation(String processId, State state) {
        return ImmutableOperation.builder()
                                 .processId(processId)
                                 .state(state)
                                 .build();
    }

}
