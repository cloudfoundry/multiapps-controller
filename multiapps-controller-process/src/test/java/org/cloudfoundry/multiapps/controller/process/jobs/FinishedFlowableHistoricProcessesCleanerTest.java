package org.cloudfoundry.multiapps.controller.process.jobs;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.query.OperationQuery;
import org.cloudfoundry.multiapps.controller.persistence.services.OperationService;
import org.flowable.engine.HistoryService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class FinishedFlowableHistoricProcessesCleanerTest {

    private static final String OPERATION_GUID = UUID.randomUUID()
                                                     .toString();

    @Mock
    private ApplicationConfiguration applicationConfiguration;
    @Mock
    private OperationService operationService;
    @Mock
    private HistoryService historyService;
    private FinishedFlowableHistoricProcessesCleaner cleaner;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        cleaner = new FinishedFlowableHistoricProcessesCleaner(applicationConfiguration, operationService, historyService);
    }

    @Test
    void testCleanerExecutionOnNonSelectedInstance() throws ExecutionException, InterruptedException {
        when(applicationConfiguration.getApplicationInstanceIndex()).thenReturn(0);
        cleaner.cleanUp();
        verify(operationService, never()).createQuery();
        verify(historyService, never()).deleteHistoricProcessInstance(any());
    }

    @Test
    void testCleanerExecution() throws ExecutionException, InterruptedException {
        when(applicationConfiguration.getApplicationInstanceIndex()).thenReturn(1);
        var operationQuery = createMockedOperationQuery();
        when(operationService.createQuery()).thenReturn(operationQuery);
        mockHistoricService();
        cleaner.cleanUp();
        verify(operationService, times(2)).createQuery();
        verify(historyService).deleteHistoricProcessInstance(OPERATION_GUID);
    }

    private OperationQuery createMockedOperationQuery() {
        OperationQuery operationQueryWithResults = getMockedOperationQuery();
        when(operationQueryWithResults.offsetOnSelect(0)).thenReturn(operationQueryWithResults);
        OperationQuery operationQueryWithoutResults = getMockedOperationQuery();
        when(operationQueryWithResults.offsetOnSelect(100)).thenReturn(operationQueryWithoutResults);
        var mockedOperations = getMockedOperations();
        when(operationQueryWithResults.list()).thenReturn(mockedOperations);
        return operationQueryWithResults;
    }

    private OperationQuery getMockedOperationQuery() {
        var operationQueryWithResults = mock(OperationQuery.class);
        when(operationQueryWithResults.limitOnSelect(anyInt())).thenReturn(operationQueryWithResults);
        when(operationQueryWithResults.endedAfter(any())).thenReturn(operationQueryWithResults);
        when(operationQueryWithResults.endedBefore(any())).thenReturn(operationQueryWithResults);
        when(operationQueryWithResults.state(Operation.State.FINISHED)).thenReturn(operationQueryWithResults);
        return operationQueryWithResults;
    }

    private List<Operation> getMockedOperations() {
        var operation = mock(Operation.class);
        when(operation.getProcessId()).thenReturn(OPERATION_GUID);
        return List.of(operation);
    }

    private void mockHistoricService() {
        var historicProcessInstanceQuery = mock(HistoricProcessInstanceQuery.class);
        when(historyService.createHistoricProcessInstanceQuery()).thenReturn(historicProcessInstanceQuery);
        when(historicProcessInstanceQuery.finished()).thenReturn(historicProcessInstanceQuery);
        when(historicProcessInstanceQuery.processInstanceIds(Set.of(OPERATION_GUID))).thenReturn(historicProcessInstanceQuery);
        var historicProcessInstances = getMockedHistoricProcessInstances();
        when(historicProcessInstanceQuery.list()).thenReturn(historicProcessInstances);
    }

    private List<HistoricProcessInstance> getMockedHistoricProcessInstances() {
        var historicProcessInstance = mock(HistoricProcessInstance.class);
        when(historicProcessInstance.getId()).thenReturn(OPERATION_GUID);
        return List.of(historicProcessInstance);
    }

}
