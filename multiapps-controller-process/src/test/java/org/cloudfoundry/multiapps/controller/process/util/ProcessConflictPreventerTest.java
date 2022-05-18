package org.cloudfoundry.multiapps.controller.process.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.api.model.ImmutableOperation;
import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.api.model.ProcessType;
import org.cloudfoundry.multiapps.controller.persistence.query.OperationQuery;
import org.cloudfoundry.multiapps.controller.persistence.services.OperationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class ProcessConflictPreventerTest {

    private final String testMtaId = "test-mta-id";
    private final String testSpaceId = "test-space-id";
    private final String testProcessId = "test-process-id";
    private OperationService operationServiceMock;
    private ProcessConflictPreventer processConflictPreventerMock;

    @BeforeEach
    void setUp() throws SLException {
        operationServiceMock = getOperationServiceMock();
        processConflictPreventerMock = new ProcessConflictPreventer(operationServiceMock);
    }

    @Test
    void testAcquireLockWithRunningOperation() {
        var runningOperation = ImmutableOperation.copyOf(getOperation())
                                                 .withSpaceId(testSpaceId)
                                                 .withHasAcquiredLock(true);
        SLException exception = assertThrows(SLException.class, () -> tryToAcquireLock(List.of(runningOperation)));
        assertEquals("Conflicting process \"test-process-id\" found for MTA \"test-mta-id\"", exception.getMessage());
    }

    @Test
    void testAcquireLockWithMultipleRunningOperations() {
        var runningOperation = ImmutableOperation.copyOf(getOperation())
                                                 .withSpaceId(testSpaceId)
                                                 .withHasAcquiredLock(true);
        SLException exception = assertThrows(SLException.class, () -> tryToAcquireLock(List.of(runningOperation, runningOperation)));
        assertEquals("Multiple operations found with lock for MTA \"test-mta-id\" in space \"test-space-id\": [test-process-id, test-process-id]", exception.getMessage());
    }

    @Test
    void testAcquireLockWithConcurrentlyRunningOperation() {
        var firstOperation = ImmutableOperation.copyOf(getOperation())
                                                 .withSpaceId(testSpaceId)
                                                 .withStartedAt(ZonedDateTime.ofInstant(Instant.ofEpochMilli(0), ZoneId.of("UTC")));
        var secondOperation = ImmutableOperation.copyOf(getOperation())
                                               .withSpaceId(testSpaceId)
                                               .withProcessId("test-process-id-1")
                                               .withStartedAt(ZonedDateTime.ofInstant(Instant.ofEpochMilli(1), ZoneId.of("UTC")));
        SLException exception = assertThrows(SLException.class, () -> tryToAcquireLock(List.of(secondOperation, firstOperation)));
        assertEquals("Conflicting process \"test-process-id-1\" found for MTA \"test-mta-id\"", exception.getMessage());
    }

    private void tryToAcquireLock(List<Operation> operations) {
        when(operationServiceMock.createQuery()
                                 .list()).thenReturn(operations);
        processConflictPreventerMock.acquireLock(testMtaId, null, testSpaceId, testProcessId);
    }

    @Test
    void testAcquireLockWithNoConflictingOperations() {
        assertDoesNotThrow(() -> processConflictPreventerMock.acquireLock(testMtaId, null, testSpaceId, testProcessId));
    }

    @Test
    void testReleaseLock() {
        Operation.State abortedState = Operation.State.ABORTED;

        processConflictPreventerMock.releaseLock(testProcessId, abortedState);
        ArgumentCaptor<Operation> argumentCaptor = ArgumentCaptor.forClass(Operation.class);
        verify(operationServiceMock).update(argumentCaptor.capture(), argThat(this::assertOperationAbort));
        assertEquals(testProcessId, argumentCaptor.getValue()
                                                  .getProcessId());
    }

    private OperationService getOperationServiceMock() throws SLException {
        OperationService operationServiceMock = mock(OperationService.class);
        OperationQuery operationQueryMock = mock(OperationQuery.class, Answers.RETURNS_SELF);
        when(operationServiceMock.createQuery()).thenReturn(operationQueryMock);
        Mockito.doReturn(getOperation())
               .when(operationQueryMock)
               .singleResult();
        return operationServiceMock;
    }

    private Operation getOperation() {
        return ImmutableOperation.builder()
                                 .processId(testProcessId)
                                 .processType(ProcessType.DEPLOY)
                                 .mtaId(testMtaId)
                                 .hasAcquiredLock(false)
                                 .state(Operation.State.RUNNING)
                                 .build();
    }

    private boolean assertOperationAbort(Operation operation) {
        return operation.getProcessId()
                        .equals(testProcessId)
            && operation.getProcessType()
                        .equals(ProcessType.DEPLOY)
            && operation.getMtaId()
                        .equals(testMtaId)
            && operation.hasAcquiredLock()
                        .equals(false)
            && operation.getState()
                        .equals(Operation.State.ABORTED)
            && operation.getEndedAt() != null;
    }

}
