package com.sap.cloud.lm.sl.cf.process.jobs;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.quartz.JobExecutionException;

import com.sap.cloud.lm.sl.cf.core.dao.OperationDao;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.cf.web.api.model.State;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.persistence.services.ProcessLogsPersistenceService;
import com.sap.cloud.lm.sl.persistence.services.ProgressMessageService;

public class InactiveOperationsCleanerTest extends OperationsCleanerTest {

    private static final Date EXPIRATION_TIME = new Date(5000);
    private static final long TIME_BEFORE_EXPIRATION = 2000;
    private static final long TIME_AFTER_EXPIRATION = 6000;
    private static final String SPACE_ID = "space";

    @Mock
    private OperationDao dao;
    @Mock
    private ProgressMessageService progressMessageService;
    @Mock
    private ProcessLogsPersistenceService processLogsPersistenceService;
    @InjectMocks
    private InactiveOperationsCleaner cleaner;

    @BeforeEach
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testCleanUpFinishedOperationsDataOK() throws JobExecutionException {
        Operation operation1 = new Operation().startedAt(epochMillisToZonedDateTime(TIME_BEFORE_EXPIRATION))
            .state(State.ABORTED)
            .processId("1")
            .spaceId(SPACE_ID);
        Operation operation2 = new Operation().startedAt(epochMillisToZonedDateTime(TIME_AFTER_EXPIRATION))
            .state(State.FINISHED)
            .processId("2")
            .spaceId(SPACE_ID);
        List<Operation> operationsList = Arrays.asList(operation1, operation2);
        mockOperationDao(dao, operationsList);

        cleaner.execute(EXPIRATION_TIME);
        verify(dao).merge(any());
    }

    @Test
    public void testCleanUpFinishedOperationsDataNoMergeError() throws JobExecutionException {
        Operation operation1 = new Operation().startedAt(epochMillisToZonedDateTime(TIME_BEFORE_EXPIRATION))
            .state(State.ABORTED)
            .processId("1")
            .spaceId(SPACE_ID);
        Operation operation2 = new Operation().startedAt(epochMillisToZonedDateTime(TIME_AFTER_EXPIRATION))
            .state(State.FINISHED)
            .processId("2")
            .spaceId(SPACE_ID);
        List<Operation> operationsList = Arrays.asList(operation1, operation2);
        mockOperationDao(dao, operationsList);

        try {
            when(progressMessageService.removeAllByProcessIds(any())).thenThrow(new SLException("I'm an exception"));
            cleaner.execute(EXPIRATION_TIME);
        } catch (Exception e) {
            verify(dao, never()).merge(any());
        }
    }

}
