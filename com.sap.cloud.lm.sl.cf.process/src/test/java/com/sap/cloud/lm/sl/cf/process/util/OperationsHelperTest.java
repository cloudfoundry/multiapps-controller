package com.sap.cloud.lm.sl.cf.process.util;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.flowable.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.sap.cloud.lm.sl.cf.core.model.HistoricOperationEvent;
import com.sap.cloud.lm.sl.cf.core.persistence.service.OperationService;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.metadata.ProcessTypeToOperationMetadataMapper;
import com.sap.cloud.lm.sl.cf.web.api.model.ErrorType;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.cf.web.api.model.ProcessType;

public class OperationsHelperTest {

    private static final String PROCESS_ID = "84af8e1e-4d96-11ea-b77f-2e728ce88178";

    @Mock
    private OperationService operationService;
    @Mock
    private ProcessTypeToOperationMetadataMapper metadataMapper;
    @Mock
    private ProcessHelper processHelper;

    private final OperationsHelper operationsHelper;

    public OperationsHelperTest() {
        MockitoAnnotations.initMocks(this);
        operationsHelper = new OperationsHelper(operationService, metadataMapper, processHelper);
    }

    @Test
    public void testGetProcessDefinitionKey() {
        Operation mockedOperation = createMockedOperation(PROCESS_ID, ProcessType.DEPLOY, Operation.State.RUNNING);
        Mockito.when(metadataMapper.getDiagramId(ProcessType.DEPLOY))
               .thenReturn(Constants.DEPLOY_SERVICE_ID);
        String processDefinitionKey = operationsHelper.getProcessDefinitionKey(mockedOperation);
        Assertions.assertEquals(Constants.DEPLOY_SERVICE_ID, processDefinitionKey);
    }

    @Test
    public void testAddErrorTypeWhenOperationIsNotInErrorState() {
        Operation mockedOperation = createMockedOperation(PROCESS_ID, ProcessType.DEPLOY, Operation.State.RUNNING);
        Operation operation = operationsHelper.addErrorType(mockedOperation);
        Assertions.assertNull(operation.getErrorType());
        Assertions.assertEquals(mockedOperation, operation);
    }

    @Test
    public void testAddErrorTypeWhenOperationIsInErrorStateNoHistoricOperationEventsAvailable() {
        Operation mockedOperation = createMockedOperation(PROCESS_ID, ProcessType.DEPLOY, Operation.State.ERROR);
        Mockito.when(processHelper.getHistoricOperationEventByProcessId(anyString()))
               .thenReturn(Collections.emptyList());
        Operation operation = operationsHelper.addErrorType(mockedOperation);
        Assertions.assertNull(operation.getErrorType());
    }

    @Test
    public void testAddErrorTypeWhenOperationIsInErrorStateContentError() {
        Operation mockedOperation = createMockedOperation(PROCESS_ID, ProcessType.DEPLOY, Operation.State.ERROR);
        HistoricOperationEvent mockedHistoricOperationEvent = createMockedHistoricOperationEvent(HistoricOperationEvent.EventType.FAILED_BY_CONTENT_ERROR);
        List<HistoricOperationEvent> historicOperationEvents = Collections.singletonList(mockedHistoricOperationEvent);
        Mockito.when(processHelper.getHistoricOperationEventByProcessId(mockedOperation.getProcessId()))
               .thenReturn(historicOperationEvents);
        Operation operation = operationsHelper.addErrorType(mockedOperation);
        Assertions.assertEquals(ErrorType.CONTENT, operation.getErrorType());
    }

    @Test
    public void testAddErrorTypeWhenOperationIsInErrorStateInfrastructureError() {
        Operation mockedOperation = createMockedOperation(PROCESS_ID, ProcessType.DEPLOY, Operation.State.ERROR);
        HistoricOperationEvent mockedHistoricOperationEvent = createMockedHistoricOperationEvent(HistoricOperationEvent.EventType.FAILED_BY_INFRASTRUCTURE_ERROR);
        List<HistoricOperationEvent> historicOperationEvents = Collections.singletonList(mockedHistoricOperationEvent);
        Mockito.when(processHelper.getHistoricOperationEventByProcessId(mockedOperation.getProcessId()))
               .thenReturn(historicOperationEvents);
        Operation operation = operationsHelper.addErrorType(mockedOperation);
        Assertions.assertEquals(ErrorType.INFRASTRUCTURE, operation.getErrorType());
    }

    @Test
    public void testAddStateWhenOperationHasState() {
        Operation mockedOperation = createMockedOperation(PROCESS_ID, ProcessType.DEPLOY, Operation.State.RUNNING);
        Operation operation = operationsHelper.addState(mockedOperation);
        Assertions.assertEquals(Operation.State.RUNNING, operation.getState());
    }

    @Test
    public void testAddStateWithAcquiredLockAndAbortedOperation() {
        Operation mockedOperation = createMockedOperation(PROCESS_ID, ProcessType.DEPLOY, null);
        Mockito.when(processHelper.isAborted(PROCESS_ID))
               .thenReturn(true);
        Mockito.when(mockedOperation.hasAcquiredLock())
               .thenReturn(true);
        Operation operation = operationsHelper.addState(mockedOperation);
        Assertions.assertEquals(Operation.State.ABORTED, operation.getState());
        Assertions.assertFalse(operation.hasAcquiredLock());
        Mockito.verify(operationService)
               .update(eq(PROCESS_ID), any());
    }

    @Test
    public void testAddStateWithAcquiredLockAndFinishedOperation() {
        Operation mockedOperation = createMockedOperation(PROCESS_ID, ProcessType.DEPLOY, null);
        Mockito.when(mockedOperation.hasAcquiredLock())
               .thenReturn(true);
        Operation operation = operationsHelper.addState(mockedOperation);
        Assertions.assertEquals(Operation.State.FINISHED, operation.getState());
        Assertions.assertFalse(operation.hasAcquiredLock());
        Mockito.verify(operationService)
               .update(eq(PROCESS_ID), any());
    }

    @Test
    public void testAddStateWithoutAcquiredLockAndRunningOperation() {
        Operation mockedOperation = createMockedOperation(PROCESS_ID, ProcessType.DEPLOY, null);
        mockProcessHelper();
        Operation operation = operationsHelper.addState(mockedOperation);
        Assertions.assertEquals(Operation.State.RUNNING, operation.getState());
        Assertions.assertFalse(operation.hasAcquiredLock());
        Mockito.verify(operationService, never())
               .update(eq(PROCESS_ID), any());
    }

    @Test
    public void testComputeOperationStateWhenProcessIsInNonFinalStateAndProcessIsAborted() {
        Mockito.when(processHelper.isAborted(PROCESS_ID))
               .thenReturn(true);
        mockProcessHelper();
        Operation.State state = operationsHelper.computeProcessState(PROCESS_ID);
        Assertions.assertEquals(Operation.State.ABORTED, state);
    }

    @Test
    public void testComputeOperationStateWhenProcessIsInNonFinalStateAndProcessIsAtReceiveTask() {
        Mockito.when(processHelper.isAtReceiveTask(PROCESS_ID))
               .thenReturn(true);
        mockProcessHelper();
        Operation.State state = operationsHelper.computeProcessState(PROCESS_ID);
        Assertions.assertEquals(Operation.State.ACTION_REQUIRED, state);
    }

    @Test
    public void testComputeOperationStateWhenProcessIsInNonFinalStateAndProcessIsInErrorState() {
        Mockito.when(processHelper.isInErrorState(PROCESS_ID))
               .thenReturn(true);
        mockProcessHelper();
        Operation.State state = operationsHelper.computeProcessState(PROCESS_ID);
        Assertions.assertEquals(Operation.State.ERROR, state);
    }

    @Test
    public void testComputeOperationStateWhenProcessIsInNonFinalStateAndProcessIsRunning() {
        mockProcessHelper();
        Operation.State state = operationsHelper.computeProcessState(PROCESS_ID);
        Assertions.assertEquals(Operation.State.RUNNING, state);
    }

    @Test
    public void testComputeOperationStateWhenProcessIsInFinalStateAndProcessIsAborted() {
        Mockito.when(processHelper.isAborted(PROCESS_ID))
               .thenReturn(true);
        Operation.State state = operationsHelper.computeProcessState(PROCESS_ID);
        Assertions.assertEquals(Operation.State.ABORTED, state);
    }

    @Test
    public void testComputeOperationStateWhenProcessIsInFinalStateAndProcessIsNotAborted() {
        Operation.State state = operationsHelper.computeProcessState(PROCESS_ID);
        Assertions.assertEquals(Operation.State.FINISHED, state);
    }

    @Test
    public void testFindOperationsWithStatusRunning() {
        List<Operation> operations = Arrays.asList(createMockedOperation("12af8e1e-4d96-11ea-b77f-2e728ce88178", ProcessType.DEPLOY,
                                                                         Operation.State.RUNNING),
                                                   createMockedOperation("13af8e1e-4d96-11ea-b77f-2e728ce88178", ProcessType.DEPLOY,
                                                                         Operation.State.ABORTED));
        List<Operation.State> statusList = Collections.singletonList(Operation.State.RUNNING);
        List<Operation> foundOperations = operationsHelper.findOperations(operations, statusList);
        Assertions.assertEquals(1, foundOperations.size());
        Assertions.assertEquals("12af8e1e-4d96-11ea-b77f-2e728ce88178", operations.get(0)
                                                                                  .getProcessId());
    }

    @Test
    public void testFundOperationsWithoutStatus() {
        List<Operation> operations = Arrays.asList(createMockedOperation("12af8e1e-4d96-11ea-b77f-2e728ce88178", ProcessType.DEPLOY,
                                                                         Operation.State.RUNNING),
                                                   createMockedOperation("13af8e1e-4d96-11ea-b77f-2e728ce88178", ProcessType.DEPLOY,
                                                                         Operation.State.ABORTED));
        List<Operation> foundOperations = operationsHelper.findOperations(operations, Collections.emptyList());
        Assertions.assertEquals(2, foundOperations.size());

    }

    private void mockProcessHelper() {
        Optional<ProcessInstance> mockedProcessInstance = Optional.of(createMockedProcessInstance(PROCESS_ID));
        Mockito.when(processHelper.findProcessInstanceById(PROCESS_ID))
               .thenReturn(mockedProcessInstance);
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

    private ProcessInstance createMockedProcessInstance(String processId) {
        ProcessInstance processInstance = Mockito.mock(ProcessInstance.class);
        Mockito.when(processInstance.getId())
               .thenReturn(processId);
        return processInstance;
    }
}
