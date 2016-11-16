package com.sap.cloud.lm.sl.cf.process.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import com.sap.cloud.lm.sl.cf.core.dao.OngoingOperationDao;
import com.sap.cloud.lm.sl.cf.core.model.OngoingOperation;
import com.sap.cloud.lm.sl.cf.core.model.ProcessType;
import com.sap.cloud.lm.sl.common.SLException;

public class ProcessConflictPreventerTest {

    private String testMtaId = "test-mta-id";
    private String testSpaceId = "test-space-id";
    private String testProcessId = "test-process-id";
    private OngoingOperationDao daoMock;
    private ProcessConflictPreventer processConflictPreventerMock;

    @Before
    public void setUp() throws SLException {
        daoMock = getOngoingOperationDaoMock();
        processConflictPreventerMock = new ProcessConflictPreventerMock(daoMock);
    }

    @Test
    public void testAttemptToAcquireLock() {
        try {
            when(daoMock.findProcessWithLock(testMtaId, testSpaceId)).thenReturn(
                new OngoingOperation(testProcessId, ProcessType.DEPLOY, null, testSpaceId, testMtaId, "", false, null));
            processConflictPreventerMock.attemptToAcquireLock(testMtaId, testSpaceId, testProcessId);
            verify(daoMock).merge(daoMock.find(testProcessId));
        } catch (SLException e) {
            assertEquals("Conflicting process \"test-process-id\" found for MTA \"test-mta-id\"", e.getMessage());
        }
    }

    @Test
    public void testAttemptToAcquireLockWithConflictProcessFound() throws SLException {
        try {
            when(daoMock.findProcessWithLock(testMtaId, testSpaceId)).thenReturn(null);
            processConflictPreventerMock.attemptToAcquireLock(testMtaId, testSpaceId, testProcessId);
        } catch (SLException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testAttemptToReleaseLock() throws SLException {
        processConflictPreventerMock.attemptToReleaseLock(testProcessId);
        verify(daoMock).merge(daoMock.find(testProcessId));
    }

    private OngoingOperationDao getOngoingOperationDaoMock() throws SLException {
        OngoingOperationDao daoMock = mock(OngoingOperationDao.class);
        when(daoMock.find(testProcessId)).thenReturn(
            new OngoingOperation(testProcessId, ProcessType.DEPLOY, "", "", testMtaId, "", false, null));
        return daoMock;
    }

    private class ProcessConflictPreventerMock extends ProcessConflictPreventer {
        public ProcessConflictPreventerMock(OngoingOperationDao dao) {
            super(dao);
        }
    }
}
