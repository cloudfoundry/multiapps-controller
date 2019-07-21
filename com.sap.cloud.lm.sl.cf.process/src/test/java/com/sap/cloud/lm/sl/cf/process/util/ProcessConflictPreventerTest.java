package com.sap.cloud.lm.sl.cf.process.util;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.sap.cloud.lm.sl.cf.core.dao.OperationDao;
import com.sap.cloud.lm.sl.cf.core.dao.filters.OperationFilter;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.cf.web.api.model.ProcessType;
import com.sap.cloud.lm.sl.common.SLException;

public class ProcessConflictPreventerTest {

    private String testMtaId = "test-mta-id";
    private String testSpaceId = "test-space-id";
    private String testProcessId = "test-process-id";
    private OperationDao daoMock;
    private ProcessConflictPreventer processConflictPreventerMock;

    @Before
    public void setUp() throws SLException {
        daoMock = getOperationDaoMock();
        processConflictPreventerMock = new ProcessConflictPreventerMock(daoMock);
    }

    @Test
    public void testAcquireLock() {
        try {
            Operation operation = new Operation().processId(testProcessId)
                .processType(ProcessType.DEPLOY)
                .spaceId(testSpaceId)
                .mtaId(testMtaId)
                .acquiredLock(false);
            OperationFilter expectedFilter = new OperationFilter.Builder().mtaId(testMtaId)
                .spaceId(testSpaceId)
                .withAcquiredLock()
                .build();
            when(daoMock.find(expectedFilter)).thenReturn(Arrays.asList(operation));
            processConflictPreventerMock.acquireLock(testMtaId, testSpaceId, testProcessId);
            verify(daoMock).update(daoMock.findRequired(testProcessId));
        } catch (SLException e) {
            assertEquals("Conflicting process \"test-process-id\" found for MTA \"test-mta-id\"", e.getMessage());
        }
    }

    @Test
    public void testAcquireLockWithNoConflictingOperations() throws SLException {
        processConflictPreventerMock.acquireLock(testMtaId, testSpaceId, testProcessId);
    }

    private OperationDao getOperationDaoMock() throws SLException {
        OperationDao daoMock = mock(OperationDao.class);
        Operation operation = new Operation().processId(testProcessId)
            .processType(ProcessType.DEPLOY)
            .mtaId(testMtaId)
            .acquiredLock(false);
        when(daoMock.findRequired(testProcessId)).thenReturn(operation);
        return daoMock;
    }

    private class ProcessConflictPreventerMock extends ProcessConflictPreventer {
        public ProcessConflictPreventerMock(OperationDao dao) {
            super(dao);
        }
    }
}
