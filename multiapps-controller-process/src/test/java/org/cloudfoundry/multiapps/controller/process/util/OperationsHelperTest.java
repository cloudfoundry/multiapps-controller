package org.cloudfoundry.multiapps.controller.process.util;

import static org.mockito.ArgumentMatchers.anyString;

import java.util.Collections;
import java.util.List;

import org.cloudfoundry.multiapps.controller.api.model.ErrorType;
import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.api.model.ProcessType;
import org.cloudfoundry.multiapps.controller.persistence.model.HistoricOperationEvent;
import org.cloudfoundry.multiapps.controller.persistence.services.OperationService;
import org.cloudfoundry.multiapps.controller.process.Constants;
import org.cloudfoundry.multiapps.controller.process.metadata.ProcessTypeToOperationMetadataMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
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
    void testReleaseLockIfNeededForFinalOperation() {
        Operation mockedOperation = createMockedOperation(PROCESS_ID, ProcessType.DEPLOY, Operation.State.ABORTED);
        Mockito.when(mockedOperation.hasAcquiredLock())
               .thenReturn(true);
        Operation operation = operationsHelper.releaseLockIfNeeded(mockedOperation);
        Assertions.assertEquals(Operation.State.ABORTED, operation.getState());
        Assertions.assertFalse(operation.hasAcquiredLock());
        Mockito.verify(operationService)
               .update(operation, operation);
    }

    @Test
    void testReleaseLockIfNeededForNonFinalOperation() {
        Operation mockedOperation = createMockedOperation(PROCESS_ID, ProcessType.DEPLOY, Operation.State.RUNNING);
        Mockito.when(mockedOperation.hasAcquiredLock())
               .thenReturn(true);
        Operation operation = operationsHelper.releaseLockIfNeeded(mockedOperation);
        Assertions.assertTrue(operation.hasAcquiredLock());
        Mockito.verify(operationService, Mockito.never())
               .update(operation, operation);
    }

    @Test
    void testReleaseLocksIfNeededForFinalOperations() {
        Operation mockedFinalOperation = createMockedOperation(PROCESS_ID, ProcessType.DEPLOY, Operation.State.ABORTED);
        Operation mockedNonFinalOperation = createMockedOperation(PROCESS_ID, ProcessType.DEPLOY, Operation.State.RUNNING);
        Mockito.when(mockedFinalOperation.hasAcquiredLock())
               .thenReturn(true);
        Mockito.when(mockedNonFinalOperation.hasAcquiredLock())
               .thenReturn(true);
        List<Operation> resultOperations = operationsHelper.releaseLocksIfNeeded(List.of(mockedFinalOperation, mockedNonFinalOperation));
        Operation updatedOperation = resultOperations.get(0);
        Operation notUpdatedOperation = resultOperations.get(1);
        Assertions.assertEquals(Operation.State.ABORTED, updatedOperation.getState());
        Assertions.assertFalse(updatedOperation.hasAcquiredLock());
        Mockito.verify(operationService, Mockito.times(1))
               .update(updatedOperation, updatedOperation);
        Assertions.assertEquals(mockedNonFinalOperation, notUpdatedOperation);
        Mockito.verify(operationService, Mockito.never())
               .update(notUpdatedOperation, notUpdatedOperation);
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
