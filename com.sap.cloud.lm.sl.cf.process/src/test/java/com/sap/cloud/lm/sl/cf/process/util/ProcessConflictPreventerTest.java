package com.sap.cloud.lm.sl.cf.process.util;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.core.persistence.query.OperationQuery;
import com.sap.cloud.lm.sl.cf.core.persistence.service.OperationService;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.cf.web.api.model.ProcessType;
import com.sap.cloud.lm.sl.common.SLException;

public class ProcessConflictPreventerTest {

    private String testMtaId = "test-mta-id";
    private String testSpaceId = "test-space-id";
    private String testProcessId = "test-process-id";
    private OperationService operationServiceMock;
    private ProcessConflictPreventer processConflictPreventerMock;

    @BeforeEach
    public void setUp() throws SLException {
        operationServiceMock = getOperationServiceMock();
        processConflictPreventerMock = new ProcessConflictPreventerMock(operationServiceMock);
    }

    @Test
    public void testAcquireLock() {
        try {
            Operation operation = new Operation().processId(testProcessId)
                                                 .processType(ProcessType.DEPLOY)
                                                 .spaceId(testSpaceId)
                                                 .mtaId(testMtaId)
                                                 .acquiredLock(false);
            when(operationServiceMock.createQuery()
                                     .list()).thenReturn(Arrays.asList(operation));
            processConflictPreventerMock.acquireLock(testMtaId, testSpaceId, testProcessId);
            Operation op = operationServiceMock.createQuery()
                                               .processId(testProcessId)
                                               .singleResult();
            verify(operationServiceMock).update(op.getProcessId(), op);
        } catch (SLException e) {
            assertEquals("Conflicting process \"test-process-id\" found for MTA \"test-mta-id\"", e.getMessage());
        }
    }

    @Test
    public void testAcquireLockWithNoConflictingOperations() {
        assertDoesNotThrow(() -> processConflictPreventerMock.acquireLock(testMtaId, testSpaceId, testProcessId));
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
        return new Operation().processId(testProcessId)
                              .processType(ProcessType.DEPLOY)
                              .mtaId(testMtaId)
                              .acquiredLock(false);
    }

    private class ProcessConflictPreventerMock extends ProcessConflictPreventer {
        public ProcessConflictPreventerMock(OperationService operationService) {
            super(operationServiceMock);
        }
    }
}
