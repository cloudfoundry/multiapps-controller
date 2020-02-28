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
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.sap.cloud.lm.sl.cf.core.model.HistoricOperationEvent;
import com.sap.cloud.lm.sl.cf.core.persistence.query.OperationQuery;
import com.sap.cloud.lm.sl.cf.core.persistence.service.OperationService;
import com.sap.cloud.lm.sl.cf.core.util.MockBuilder;
import com.sap.cloud.lm.sl.cf.process.flowable.AbortProcessAction;
import com.sap.cloud.lm.sl.cf.process.flowable.AdditionalProcessAction;
import com.sap.cloud.lm.sl.cf.process.flowable.FlowableFacade;
import com.sap.cloud.lm.sl.cf.process.flowable.ProcessActionRegistry;
import com.sap.cloud.lm.sl.cf.process.util.HistoricOperationEventPersister;
import com.sap.cloud.lm.sl.cf.process.util.ProcessConflictPreventer;
import com.sap.cloud.lm.sl.cf.web.api.model.ImmutableOperation;
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
    private OperationService operationService;
    @Mock(answer = Answers.RETURNS_SELF)
    private OperationQuery operationQuery;
    @Mock
    private FlowableFacade flowableFacade;
    @Mock
    private ProcessActionRegistry registry;
    @Mock
    private HistoricOperationEventPersister historicOperationEventPersister;
    @InjectMocks
    private OperationsCleaner cleaner;

    @BeforeEach
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
        cleaner.withPageSize(PAGE_SIZE);

        when(registry.getAction("abort")).thenReturn(new AbortProcessActionMock(flowableFacade,
                                                                                Collections.emptyList(),
                                                                                historicOperationEventPersister,
                                                                                operationService));
    }

    @Test
    public void testExpiredOperationsAreAborted() {
        Operation operation1 = ImmutableOperation.builder()
                                                 .processId(OPERATION_ID_1)
                                                 .startedAt(epochMillisToZonedDateTime(TIME_BEFORE_EXPIRATION_1))
                                                 .build();
        Operation operation2 = ImmutableOperation.builder()
                                                 .processId(OPERATION_ID_2)
                                                 .startedAt(epochMillisToZonedDateTime(TIME_BEFORE_EXPIRATION_2))
                                                 .build();
        List<Operation> operationsList = Arrays.asList(operation1, operation2);
        when(operationService.createQuery()).thenReturn(operationQuery);
        initQueryMockForPage(0, operationsList);

        cleaner.execute(EXPIRATION_TIME);
        verify(historicOperationEventPersister).add(OPERATION_ID_1, HistoricOperationEvent.EventType.ABORTED);
        verify(historicOperationEventPersister).add(OPERATION_ID_2, HistoricOperationEvent.EventType.ABORTED);
    }

    @Test
    public void testAbortResilience() {
        Operation operation1 = ImmutableOperation.builder()
                                                 .processId(OPERATION_ID_1)
                                                 .startedAt(epochMillisToZonedDateTime(TIME_BEFORE_EXPIRATION_1))
                                                 .build();
        Operation operation2 = ImmutableOperation.builder()
                                                 .processId(OPERATION_ID_2)
                                                 .startedAt(epochMillisToZonedDateTime(TIME_BEFORE_EXPIRATION_2))
                                                 .build();
        List<Operation> operationsList = Arrays.asList(operation1, operation2);

        when(operationService.createQuery()).thenReturn(operationQuery);
        initQueryMockForPage(0, operationsList);
        doThrow(new FlowableOptimisticLockingException("I'm an exception")).when(flowableFacade)
                                                                           .deleteProcessInstance(eq(OPERATION_ID_1), any());

        cleaner.execute(EXPIRATION_TIME);
        verify(historicOperationEventPersister).add(OPERATION_ID_1, HistoricOperationEvent.EventType.ABORTED);
        verify(historicOperationEventPersister).add(OPERATION_ID_2, HistoricOperationEvent.EventType.ABORTED);
    }

    @Test
    public void testPaging() {
        Operation operation1 = ImmutableOperation.builder()
                                                 .processId(OPERATION_ID_1)
                                                 .startedAt(epochMillisToZonedDateTime(TIME_BEFORE_EXPIRATION_1))
                                                 .build();
        Operation operation2 = ImmutableOperation.builder()
                                                 .processId(OPERATION_ID_2)
                                                 .startedAt(epochMillisToZonedDateTime(TIME_BEFORE_EXPIRATION_2))
                                                 .build();
        Operation operation3 = ImmutableOperation.builder()
                                                 .processId(OPERATION_ID_3)
                                                 .startedAt(epochMillisToZonedDateTime(TIME_BEFORE_EXPIRATION_2))
                                                 .build();
        List<Operation> operationsPage1 = Arrays.asList(operation1, operation2);
        List<Operation> operationsPage2 = Collections.singletonList(operation3);

        when(operationService.createQuery()).thenReturn(operationQuery);
        initQueryMockForPage(0, operationsPage1);
        initQueryMockForPage(1, operationsPage2);

        cleaner.execute(EXPIRATION_TIME);
        verify(historicOperationEventPersister).add(OPERATION_ID_1, HistoricOperationEvent.EventType.ABORTED);
        verify(historicOperationEventPersister).add(OPERATION_ID_2, HistoricOperationEvent.EventType.ABORTED);
        verify(historicOperationEventPersister).add(OPERATION_ID_3, HistoricOperationEvent.EventType.ABORTED);
    }

    private void initQueryMockForPage(int pageIndex, List<Operation> result) {
        OperationQuery queryMock = createOperationQueryMock(pageIndex);
        when(queryMock.list()).thenReturn(result);
    }

    private OperationQuery createOperationQueryMock(int pageIndex) {
        return new MockBuilder<>(operationQuery).on(OperationQuery::inNonFinalState)
                                                .on(query -> query.startedBefore(EXPIRATION_TIME))
                                                .on(query -> query.offsetOnSelect(pageIndex * PAGE_SIZE))
                                                .on(query -> query.limitOnSelect(PAGE_SIZE))
                                                .on(query -> query.orderByProcessId(any()))
                                                .build();
    }

    @Test
    public void testExpiredOperationsAreDeleted() {
        when(operationService.createQuery()).thenReturn(operationQuery);
        OperationQuery mock = new MockBuilder<>(operationQuery).on(query -> query.startedBefore(EXPIRATION_TIME))
                                                               .build();
        cleaner.execute(EXPIRATION_TIME);
        verify(mock).delete();
    }

    private ZonedDateTime epochMillisToZonedDateTime(long epochMillis) {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
    }

    private static class AbortProcessActionMock extends AbortProcessAction {

        public AbortProcessActionMock(FlowableFacade flowableFacade, List<AdditionalProcessAction> additionalProcessActions,
                                      HistoricOperationEventPersister historicEventPersister, OperationService operationService) {
            super(flowableFacade, additionalProcessActions, historicEventPersister, operationService);
        }

        @Override
        protected ProcessConflictPreventer getProcessConflictPreventer() {
            return Mockito.mock(ProcessConflictPreventer.class);
        }
    }

}
