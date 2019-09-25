package com.sap.cloud.lm.sl.cf.process.jobs;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.flowable.common.engine.api.FlowableOptimisticLockingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sap.cloud.lm.sl.cf.core.dao.OperationDao;
import com.sap.cloud.lm.sl.cf.core.dao.filters.OperationFilter;
import com.sap.cloud.lm.sl.cf.process.flowable.AbortProcessAction;
import com.sap.cloud.lm.sl.cf.process.flowable.FlowableFacade;
import com.sap.cloud.lm.sl.cf.process.flowable.ProcessActionRegistry;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;

public class OperationsCleanerTest {

    private static final Date EXPIRATION_TIME = new Date(5000);
    private static final long TIME_BEFORE_EXPIRATION_1 = 2000;
    private static final long TIME_BEFORE_EXPIRATION_2 = 3000;
    private static final String OPERATION_ID_1 = "1";
    private static final String OPERATION_ID_2 = "2";
    private static final String OPERATION_ID_3 = "3";
    private static final int PAGE_SIZE = 2;

    @Mock
    private OperationDao dao;
    @Mock
    private FlowableFacade flowableFacade;
    @Mock
    private ProcessActionRegistry registry;
    @InjectMocks
    private OperationsCleaner cleaner;

    @BeforeEach
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
        cleaner.withPageSize(PAGE_SIZE);
        when(registry.getAction("abort")).thenReturn(new AbortProcessAction(flowableFacade, Collections.emptyList()));
    }

    @Test
    public void testExpiredOperationsAreAborted() {
        Operation operation1 = new Operation().processId(OPERATION_ID_1)
                                              .startedAt(epochMillisToZonedDateTime(TIME_BEFORE_EXPIRATION_1));
        Operation operation2 = new Operation().processId(OPERATION_ID_2)
                                              .startedAt(epochMillisToZonedDateTime(TIME_BEFORE_EXPIRATION_2));
        List<Operation> operationsList = Arrays.asList(operation1, operation2);

        when(dao.find(createExpectedFilterForPage(0))).thenReturn(operationsList);

        cleaner.execute(EXPIRATION_TIME);
        verify(flowableFacade).deleteProcessInstance(any(), eq(OPERATION_ID_1), any());
        verify(flowableFacade).deleteProcessInstance(any(), eq(OPERATION_ID_2), any());
    }

    @Test
    public void testAbortResilience() {
        Operation operation1 = new Operation().processId(OPERATION_ID_1)
                                              .startedAt(epochMillisToZonedDateTime(TIME_BEFORE_EXPIRATION_1));
        Operation operation2 = new Operation().processId(OPERATION_ID_2)
                                              .startedAt(epochMillisToZonedDateTime(TIME_BEFORE_EXPIRATION_2));
        List<Operation> operationsList = Arrays.asList(operation1, operation2);

        when(dao.find(createExpectedFilterForPage(0))).thenReturn(operationsList);
        doThrow(new FlowableOptimisticLockingException("I'm an exception")).when(flowableFacade)
                                                                           .deleteProcessInstance(any(), eq(OPERATION_ID_1), any());

        cleaner.execute(EXPIRATION_TIME);
        verify(flowableFacade).deleteProcessInstance(any(), eq(OPERATION_ID_1), any());
        verify(flowableFacade).deleteProcessInstance(any(), eq(OPERATION_ID_2), any());
    }

    @Test
    public void testPaging() {
        Operation operation1 = new Operation().processId(OPERATION_ID_1)
                                              .startedAt(epochMillisToZonedDateTime(TIME_BEFORE_EXPIRATION_1));
        Operation operation2 = new Operation().processId(OPERATION_ID_2)
                                              .startedAt(epochMillisToZonedDateTime(TIME_BEFORE_EXPIRATION_2));
        Operation operation3 = new Operation().processId(OPERATION_ID_3)
                                              .startedAt(epochMillisToZonedDateTime(TIME_BEFORE_EXPIRATION_2));
        List<Operation> operationsPage1 = Arrays.asList(operation1, operation2);
        List<Operation> operationsPage2 = Arrays.asList(operation3);

        when(dao.find(createExpectedFilterForPage(0))).thenReturn(operationsPage1);
        when(dao.find(createExpectedFilterForPage(1))).thenReturn(operationsPage2);

        cleaner.execute(EXPIRATION_TIME);
        verify(flowableFacade).deleteProcessInstance(any(), eq(OPERATION_ID_1), any());
        verify(flowableFacade).deleteProcessInstance(any(), eq(OPERATION_ID_2), any());
        verify(flowableFacade).deleteProcessInstance(any(), eq(OPERATION_ID_3), any());
    }

    @Test
    public void testExpiredOperationsAreDeleted() {
        cleaner.execute(EXPIRATION_TIME);
        verify(dao).removeExpiredInFinalState(EXPIRATION_TIME);
    }

    private ZonedDateTime epochMillisToZonedDateTime(long epochMillis) {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
    }

    private OperationFilter createExpectedFilterForPage(int pageIndex) {
        return new OperationFilter.Builder().inNonFinalState()
                                            .startedBefore(EXPIRATION_TIME)
                                            .firstElement(pageIndex * PAGE_SIZE)
                                            .maxResults(PAGE_SIZE)
                                            .orderByProcessId()
                                            .build();
    }

}
