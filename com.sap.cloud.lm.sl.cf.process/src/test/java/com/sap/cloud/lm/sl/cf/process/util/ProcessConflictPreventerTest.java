package com.sap.cloud.lm.sl.cf.process.util;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.core.persistence.query.OperationQuery;
import com.sap.cloud.lm.sl.cf.core.persistence.service.OperationService;
import com.sap.cloud.lm.sl.cf.web.api.model.ImmutableOperation;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.cf.web.api.model.ProcessType;
import com.sap.cloud.lm.sl.common.SLException;

public class ProcessConflictPreventerTest {

    private final String testMtaId = "test-mta-id";
    private final String testSpaceId = "test-space-id";
    private final String testProcessId = "test-process-id";
    private OperationService operationServiceMock;
    private ProcessConflictPreventer processConflictPreventerMock;

    @BeforeEach
    public void setUp() throws SLException {
        operationServiceMock = getOperationServiceMock();
        processConflictPreventerMock = new ProcessConflictPreventer(operationServiceMock);
    }

    @Test
    public void testAcquireLock() {
        SLException exception = assertThrows(SLException.class, this::tryToAcquireLock);
        assertEquals("Conflicting process \"test-process-id\" found for MTA \"test-mta-id\"", exception.getMessage());
    }

    private void tryToAcquireLock() {
        Operation operation = ImmutableOperation.builder()
                                                .processId(testProcessId)
                                                .processType(ProcessType.DEPLOY)
                                                .spaceId(testSpaceId)
                                                .mtaId(testMtaId)
                                                .hasAcquiredLock(false)
                                                .build();
        when(operationServiceMock.createQuery()
                                 .list()).thenReturn(Collections.singletonList(operation));
        processConflictPreventerMock.acquireLock(testMtaId, testSpaceId, testProcessId);
        Operation op = operationServiceMock.createQuery()
                                           .processId(testProcessId)
                                           .singleResult();
        verify(operationServiceMock).update(op.getProcessId(), op);
    }

    @Test
    public void testAcquireLockWithNoConflictingOperations() {
        assertDoesNotThrow(() -> processConflictPreventerMock.acquireLock(testMtaId, testSpaceId, testProcessId));
    }

    @Test
    void testReleaseLock() {
        Operation.State abortedState = Operation.State.ABORTED;

        processConflictPreventerMock.releaseLock(testProcessId, abortedState);

        verify(operationServiceMock).update(eq(testProcessId), argThat(this::assertOperationAbort));
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
