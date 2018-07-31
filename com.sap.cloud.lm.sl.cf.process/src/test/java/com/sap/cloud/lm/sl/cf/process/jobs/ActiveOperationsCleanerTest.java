package com.sap.cloud.lm.sl.cf.process.jobs;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.activiti.engine.ActivitiOptimisticLockingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.quartz.JobExecutionException;

import com.sap.cloud.lm.sl.cf.core.activiti.ActivitiFacade;
import com.sap.cloud.lm.sl.cf.core.dao.OperationDao;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.cf.web.api.model.State;

public class ActiveOperationsCleanerTest extends OperationsCleanerTest {

    private static final Date EXPIRATION_TIME = new Date(5000);
    private static final long TIME_BEFORE_EXPIRATION_1 = 2000;
    private static final long TIME_BEFORE_EXPIRATION_2 = 3000;
    private static final long TIME_BEFORE_EXPIRATION_3 = 4000;
    private static final long TIME_AFTER_EXPIRATION = 6000;
    private static final String OPERATION_ID_1 = "1";
    private static final String OPERATION_ID_2 = "2";
    private static final String OPERATION_ID_3 = "3";
    private static final String OPERATION_ID_4 = "4";

    @Mock
    private OperationDao dao;
    @Mock
    private ActivitiFacade activitiFacade;
    @InjectMocks
    private ActiveOperationsCleaner cleaner;

    @BeforeEach
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testAbortOldOperationsInActiveStateOK() throws JobExecutionException {
        Operation operation1 = new Operation().startedAt(epochMillisToZonedDateTime(TIME_BEFORE_EXPIRATION_1))
            .state(State.RUNNING)
            .processId(OPERATION_ID_1)
            .cleanedUp(false);
        Operation operation2 = new Operation().startedAt(epochMillisToZonedDateTime(TIME_AFTER_EXPIRATION))
            .state(State.RUNNING)
            .processId(OPERATION_ID_2);
        Operation operation3 = new Operation().startedAt(epochMillisToZonedDateTime(TIME_BEFORE_EXPIRATION_2))
            .state(State.ABORTED)
            .processId(OPERATION_ID_3);
        Operation operation4 = new Operation().startedAt(epochMillisToZonedDateTime(TIME_BEFORE_EXPIRATION_3))
            .state(State.ERROR)
            .processId(OPERATION_ID_4);

        List<Operation> operationsList = Arrays.asList(operation1, operation2, operation3, operation4);
        mockOperationDao(dao, operationsList);

        cleaner.execute(EXPIRATION_TIME);
        verify(activitiFacade).deleteProcessInstance(any(), eq(OPERATION_ID_1), any());
        verify(activitiFacade).deleteProcessInstance(any(), eq(OPERATION_ID_4), any());
    }

    @Test
    public void testAbortOldOperationsInActiveStateErrorResilience() throws JobExecutionException {
        Operation operation1 = new Operation().startedAt(epochMillisToZonedDateTime(TIME_BEFORE_EXPIRATION_1))
            .state(State.RUNNING)
            .processId(OPERATION_ID_1);
        Operation operation2 = new Operation().startedAt(epochMillisToZonedDateTime(TIME_BEFORE_EXPIRATION_2))
            .state(State.ERROR)
            .processId(OPERATION_ID_2);

        List<Operation> operationsList = Arrays.asList(operation1, operation2);
        mockOperationDao(dao, operationsList);
        doThrow(new ActivitiOptimisticLockingException("I'm an exception")).when(activitiFacade)
            .deleteProcessInstance(any(), eq(operation1.getProcessId()), any());

        cleaner.execute(EXPIRATION_TIME);
        verify(activitiFacade).deleteProcessInstance(any(), eq(OPERATION_ID_1), any());
        verify(activitiFacade).deleteProcessInstance(any(), eq(OPERATION_ID_2), any());
    }

}
