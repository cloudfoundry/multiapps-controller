package org.cloudfoundry.multiapps.controller.process.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;

import java.util.Collections;
import java.util.List;

import org.cloudfoundry.multiapps.controller.api.model.ErrorType;
import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.api.model.Operation.State;
import org.cloudfoundry.multiapps.controller.api.model.ProcessType;
import org.cloudfoundry.multiapps.controller.persistence.model.HistoricOperationEvent;
import org.cloudfoundry.multiapps.controller.persistence.services.OperationService;
import org.cloudfoundry.multiapps.controller.process.Constants;
import org.cloudfoundry.multiapps.controller.process.metadata.ProcessTypeToOperationMetadataMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class OperationsHelperTest {

    private static final String PROCESS_ID = "84af8e1e-4d96-11ea-b77f-2e728ce88178";

    @Mock
    private OperationService operationService;
    @Mock
    private ProcessTypeToOperationMetadataMapper metadataMapper;
    @Mock
    private ProcessHelper processHelper;

    private final OperationsHelper operationsHelper;

    OperationsHelperTest() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        operationsHelper = new OperationsHelper(operationService, metadataMapper, processHelper);
    }

    @Test
    void testGetProcessDefinitionKey() {
        Operation mockedOperation = createMockedOperation(PROCESS_ID, ProcessType.DEPLOY, Operation.State.RUNNING);
        Mockito.when(metadataMapper.getDiagramId(ProcessType.DEPLOY))
               .thenReturn(Constants.DEPLOY_SERVICE_ID);
        String processDefinitionKey = operationsHelper.getProcessDefinitionKey(mockedOperation);
        Assertions.assertEquals(Constants.DEPLOY_SERVICE_ID, processDefinitionKey);
    }

    @Test
    void testAddErrorTypeWhenOperationIsNotInErrorState() {
        Operation mockedOperation = createMockedOperation(PROCESS_ID, ProcessType.DEPLOY, Operation.State.RUNNING);
        Operation operation = operationsHelper.addErrorType(mockedOperation);
        Assertions.assertNull(operation.getErrorType());
        Assertions.assertEquals(mockedOperation, operation);
    }

    @Test
    void testAddErrorTypeWhenOperationIsInErrorStateNoHistoricOperationEventsAvailable() {
        Operation mockedOperation = createMockedOperation(PROCESS_ID, ProcessType.DEPLOY, Operation.State.ERROR);
        Mockito.when(processHelper.getHistoricOperationEventByProcessId(anyString()))
               .thenReturn(Collections.emptyList());
        Operation operation = operationsHelper.addErrorType(mockedOperation);
        Assertions.assertNull(operation.getErrorType());
    }

    @Test
    void testAddErrorTypeWhenOperationIsInErrorStateContentError() {
        Operation mockedOperation = createMockedOperation(PROCESS_ID, ProcessType.DEPLOY, Operation.State.ERROR);
        HistoricOperationEvent mockedHistoricOperationEvent = createMockedHistoricOperationEvent(HistoricOperationEvent.EventType.FAILED_BY_CONTENT_ERROR);
        List<HistoricOperationEvent> historicOperationEvents = List.of(mockedHistoricOperationEvent);
        Mockito.when(processHelper.getHistoricOperationEventByProcessId(mockedOperation.getProcessId()))
               .thenReturn(historicOperationEvents);
        Operation operation = operationsHelper.addErrorType(mockedOperation);
        Assertions.assertEquals(ErrorType.CONTENT, operation.getErrorType());
    }

    @Test
    void testAddErrorTypeWhenOperationIsInErrorStateInfrastructureError() {
        Operation mockedOperation = createMockedOperation(PROCESS_ID, ProcessType.DEPLOY, Operation.State.ERROR);
        HistoricOperationEvent mockedHistoricOperationEvent = createMockedHistoricOperationEvent(HistoricOperationEvent.EventType.FAILED_BY_INFRASTRUCTURE_ERROR);
        List<HistoricOperationEvent> historicOperationEvents = List.of(mockedHistoricOperationEvent);
        Mockito.when(processHelper.getHistoricOperationEventByProcessId(mockedOperation.getProcessId()))
               .thenReturn(historicOperationEvents);
        Operation operation = operationsHelper.addErrorType(mockedOperation);
        Assertions.assertEquals(ErrorType.INFRASTRUCTURE, operation.getErrorType());
    }

    @Test
    void testAddStateWhenOperationHasState() {
        Operation mockedOperation = createMockedOperation(PROCESS_ID, ProcessType.DEPLOY, Operation.State.RUNNING);
        Operation operation = operationsHelper.addState(mockedOperation);
        Assertions.assertEquals(Operation.State.RUNNING, operation.getState());
    }

    @Test
    void testAddStateWithAcquiredLockAndAbortedOperation() {
        Operation mockedOperation = createMockedOperation(PROCESS_ID, ProcessType.DEPLOY, null);
        Mockito.when(processHelper.computeProcessState(PROCESS_ID))
               .thenReturn(State.ABORTED);
        Mockito.when(mockedOperation.hasAcquiredLock())
               .thenReturn(true);
        Operation operation = operationsHelper.addState(mockedOperation);
        Assertions.assertEquals(Operation.State.ABORTED, operation.getState());
        Assertions.assertFalse(operation.hasAcquiredLock());
        ArgumentCaptor<Operation> argumentCaptor = ArgumentCaptor.forClass(Operation.class);
        Mockito.verify(operationService)
               .update(argumentCaptor.capture(), any());
        assertEquals(PROCESS_ID, argumentCaptor.getValue()
                                               .getProcessId());
    }

    @Test
    void testAddStateWithAcquiredLockAndFinishedOperation() {
        Operation mockedOperation = createMockedOperation(PROCESS_ID, ProcessType.DEPLOY, null);
        Mockito.when(mockedOperation.hasAcquiredLock())
               .thenReturn(true);
        Mockito.when(processHelper.computeProcessState(PROCESS_ID))
               .thenReturn(State.FINISHED);
        Operation operation = operationsHelper.addState(mockedOperation);
        Assertions.assertEquals(Operation.State.FINISHED, operation.getState());
        Assertions.assertFalse(operation.hasAcquiredLock());
        ArgumentCaptor<Operation> argumentCaptor = ArgumentCaptor.forClass(Operation.class);
        Mockito.verify(operationService)
               .update(argumentCaptor.capture(), any());
        assertEquals(PROCESS_ID, argumentCaptor.getValue()
                                               .getProcessId());
    }

    @Test
    void testAddStateWithoutAcquiredLockAndRunningOperation() {
        Operation mockedOperation = createMockedOperation(PROCESS_ID, ProcessType.DEPLOY, null);
        Mockito.when(processHelper.computeProcessState(PROCESS_ID))
               .thenReturn(State.RUNNING);
        Operation operation = operationsHelper.addState(mockedOperation);
        Assertions.assertEquals(Operation.State.RUNNING, operation.getState());
        Assertions.assertFalse(operation.hasAcquiredLock());
        Mockito.verify(operationService, never())
               .update(any(), any());
    }

    @Test
    void testComputeOperationStateWhenProcessIsInNonFinalStateAndProcessIsAborted() {
        Mockito.when(processHelper.computeProcessState(PROCESS_ID))
               .thenReturn(State.ABORTED);
        Operation.State state = operationsHelper.computeProcessState(PROCESS_ID);
        Assertions.assertEquals(Operation.State.ABORTED, state);
    }

    @Test
    void testComputeOperationStateWhenProcessIsInNonFinalStateAndProcessIsAtReceiveTask() {
        Mockito.when(processHelper.computeProcessState(PROCESS_ID))
               .thenReturn(State.ACTION_REQUIRED);
        Operation.State state = operationsHelper.computeProcessState(PROCESS_ID);
        Assertions.assertEquals(Operation.State.ACTION_REQUIRED, state);
    }

    @Test
    void testComputeOperationStateWhenProcessIsInNonFinalStateAndProcessIsInErrorState() {
        Mockito.when(processHelper.computeProcessState(PROCESS_ID))
               .thenReturn(State.ERROR);
        Operation.State state = operationsHelper.computeProcessState(PROCESS_ID);
        Assertions.assertEquals(Operation.State.ERROR, state);
    }

    @Test
    void testComputeOperationStateWhenProcessIsInNonFinalStateAndProcessIsRunning() {
        Mockito.when(processHelper.computeProcessState(PROCESS_ID))
               .thenReturn(State.RUNNING);
        Operation.State state = operationsHelper.computeProcessState(PROCESS_ID);
        Assertions.assertEquals(Operation.State.RUNNING, state);
    }

    @Test
    void testComputeOperationStateWhenProcessIsInFinalStateAndProcessIsAborted() {
        Mockito.when(processHelper.computeProcessState(PROCESS_ID))
               .thenReturn(State.ABORTED);
        Operation.State state = operationsHelper.computeProcessState(PROCESS_ID);
        Assertions.assertEquals(Operation.State.ABORTED, state);
    }

    @Test
    void testComputeOperationStateWhenProcessIsInFinalStateAndProcessIsNotAborted() {
        Mockito.when(processHelper.computeProcessState(PROCESS_ID))
               .thenReturn(State.FINISHED);
        Operation.State state = operationsHelper.computeProcessState(PROCESS_ID);
        Assertions.assertEquals(Operation.State.FINISHED, state);
    }

    @Test
    void testFindOperationsWithStatusRunning() {
        List<Operation> operations = List.of(createMockedOperation("12af8e1e-4d96-11ea-b77f-2e728ce88178", ProcessType.DEPLOY,
                                                                   Operation.State.RUNNING),
                                             createMockedOperation("13af8e1e-4d96-11ea-b77f-2e728ce88178", ProcessType.DEPLOY,
                                                                   Operation.State.ABORTED));
        List<Operation.State> statusList = List.of(Operation.State.RUNNING);
        List<Operation> foundOperations = operationsHelper.findOperations(operations, statusList);
        Assertions.assertEquals(1, foundOperations.size());
        Assertions.assertEquals("12af8e1e-4d96-11ea-b77f-2e728ce88178", operations.get(0)
                                                                                  .getProcessId());
    }

    @Test
    void testFundOperationsWithoutStatus() {
        List<Operation> operations = List.of(createMockedOperation("12af8e1e-4d96-11ea-b77f-2e728ce88178", ProcessType.DEPLOY,
                                                                   Operation.State.RUNNING),
                                             createMockedOperation("13af8e1e-4d96-11ea-b77f-2e728ce88178", ProcessType.DEPLOY,
                                                                   Operation.State.ABORTED));
        List<Operation> foundOperations = operationsHelper.findOperations(operations, Collections.emptyList());
        Assertions.assertEquals(2, foundOperations.size());

    }

    private Operation createMockedOperation(String processId, ProcessType processType, Operation.State state) {
        Operation operation = Mockito.mock(Operation.class);
        Mockito.when(operation.getProcessType())
               .thenReturn(processType);
        Mockito.when(operation.getState())
               .thenReturn(state);
        Mockito.when(operation.getProcessId())
               .thenReturn(processId);
        return operation;
    }

    private HistoricOperationEvent createMockedHistoricOperationEvent(HistoricOperationEvent.EventType eventType) {
        HistoricOperationEvent historicOperationEvent = Mockito.mock(HistoricOperationEvent.class);
        Mockito.when(historicOperationEvent.getType())
               .thenReturn(eventType);
        return historicOperationEvent;
    }

}
