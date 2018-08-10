package com.sap.cloud.lm.sl.cf.process.jobs;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.activiti.engine.ActivitiOptimisticLockingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.quartz.JobExecutionException;

import com.sap.cloud.lm.sl.cf.core.activiti.ActivitiFacade;
import com.sap.cloud.lm.sl.cf.core.dao.OperationDao;
import com.sap.cloud.lm.sl.cf.core.dao.filters.OperationFilter;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.cf.web.api.model.State;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.persistence.services.ProcessLogsPersistenceService;
import com.sap.cloud.lm.sl.persistence.services.ProgressMessageService;

public class OperationsCleanerTest {

    private static final Date EXPIRATION_TIME = new Date(5000);
    private static final long TIME_BEFORE_EXPIRATION_1 = 2000;
    private static final long TIME_BEFORE_EXPIRATION_2 = 3000;
    private static final long TIME_AFTER_EXPIRATION = 6000;
    private static final String OPERATION_ID_1 = "1";
    private static final String OPERATION_ID_2 = "2";
    private static final String OPERATION_ID_3 = "3";
    private static final String SPACE_ID = "space";

    @Mock
    private OperationDao dao;
    @Mock
    private ActivitiFacade activitiFacade;
    @Mock
    private ProgressMessageService progressMessageService;
    @Mock
    private ProcessLogsPersistenceService processLogsPersistenceService;
    @InjectMocks
    private OperationsCleaner cleaner;

    @BeforeEach
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testAbortOfExpiredOperationsInActiveState() throws JobExecutionException {
        Operation operation1 = new Operation().startedAt(epochMillisToZonedDateTime(TIME_BEFORE_EXPIRATION_1))
            .processId(OPERATION_ID_1);
        Operation operation2 = new Operation().startedAt(epochMillisToZonedDateTime(TIME_AFTER_EXPIRATION))
            .processId(OPERATION_ID_2);
        Operation operation3 = new Operation().startedAt(epochMillisToZonedDateTime(TIME_BEFORE_EXPIRATION_2))
            .processId(OPERATION_ID_3);

        List<Operation> operationsList = Arrays.asList(operation1, operation2, operation3);
        mockOperationDao(dao, operationsList);

        cleaner.execute(EXPIRATION_TIME);
        verify(activitiFacade).deleteProcessInstance(any(), eq(OPERATION_ID_1), any());
        verify(activitiFacade).deleteProcessInstance(any(), eq(OPERATION_ID_3), any());
    }

    @Test
    public void testAbortOfExpiredOperationsResilience() throws JobExecutionException {
        Operation operation1 = new Operation().startedAt(epochMillisToZonedDateTime(TIME_BEFORE_EXPIRATION_1))
            .processId(OPERATION_ID_1);
        Operation operation2 = new Operation().startedAt(epochMillisToZonedDateTime(TIME_BEFORE_EXPIRATION_2))
            .processId(OPERATION_ID_2);

        List<Operation> operationsList = Arrays.asList(operation1, operation2);
        mockOperationDao(dao, operationsList);
        doThrow(new ActivitiOptimisticLockingException("I'm an exception")).when(activitiFacade)
            .deleteProcessInstance(any(), eq(operation1.getProcessId()), any());

        cleaner.execute(EXPIRATION_TIME);
        verify(activitiFacade).deleteProcessInstance(any(), eq(OPERATION_ID_1), any());
        verify(activitiFacade).deleteProcessInstance(any(), eq(OPERATION_ID_2), any());
    }

    @Test
    public void testOperationsAreMarkedAsCleanedUp() throws JobExecutionException {
        Operation operation1 = new Operation().startedAt(epochMillisToZonedDateTime(TIME_AFTER_EXPIRATION))
            .processId(OPERATION_ID_1)
            .spaceId(SPACE_ID);
        Operation operation2 = new Operation().startedAt(epochMillisToZonedDateTime(TIME_BEFORE_EXPIRATION_1))
            .state(State.FINISHED)
            .processId(OPERATION_ID_2)
            .spaceId(SPACE_ID);
        List<Operation> operationsList = Arrays.asList(operation1, operation2);
        mockOperationDao(dao, operationsList);

        cleaner.execute(EXPIRATION_TIME);
        verify(dao).remove(OPERATION_ID_2);
    }

    @Test
    public void testOperationsAreNotMarkedAsCleanedUpInCaseOfErrors() throws JobExecutionException {
        Operation operation1 = new Operation().startedAt(epochMillisToZonedDateTime(TIME_AFTER_EXPIRATION))
            .processId(OPERATION_ID_1)
            .spaceId(SPACE_ID);
        Operation operation2 = new Operation().startedAt(epochMillisToZonedDateTime(TIME_BEFORE_EXPIRATION_1))
            .state(State.ABORTED)
            .processId(OPERATION_ID_2)
            .spaceId(SPACE_ID);
        List<Operation> operationsList = Arrays.asList(operation1, operation2);
        mockOperationDao(dao, operationsList);

        try {
            when(progressMessageService.removeAllByProcessIds(any())).thenThrow(new SLException("I'm an exception"));
            cleaner.execute(EXPIRATION_TIME);
        } catch (Exception e) {
            verify(dao, never()).remove(OPERATION_ID_1);
            verify(dao, never()).remove(OPERATION_ID_2);
        }
    }

    private ZonedDateTime epochMillisToZonedDateTime(long epochMillis) {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
    }

    private void mockOperationDao(OperationDao dao, List<Operation> operationsList) {
        when(dao.find((OperationFilter) any())).thenAnswer(new Answer<List<Operation>>() {
            @Override
            public List<Operation> answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                OperationFilter filter = (OperationFilter) args[0];

                List<Operation> result = operationsList.stream()
                    .filter(operation -> filterOperations(operation, filter))
                    .collect(Collectors.toList());

                return result;
            }
        });
    }

    private boolean filterOperations(Operation operation, OperationFilter filter) {
        Instant startTimeUpperBound = filter.getStartedBefore()
            .toInstant();
        Instant startTime = operation.getStartedAt()
            .toInstant();

        return startTime.isBefore(startTimeUpperBound);
    }

}
