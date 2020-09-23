package org.cloudfoundry.multiapps.controller.process.jobs;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.cloudfoundry.multiapps.controller.api.model.ImmutableOperation;
import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientProvider;
import org.cloudfoundry.multiapps.controller.core.test.MockBuilder;
import org.cloudfoundry.multiapps.controller.persistence.model.HistoricOperationEvent;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableHistoricOperationEvent;
import org.cloudfoundry.multiapps.controller.persistence.query.OperationQuery;
import org.cloudfoundry.multiapps.controller.persistence.services.HistoricOperationEventService;
import org.cloudfoundry.multiapps.controller.persistence.services.OperationService;
import org.cloudfoundry.multiapps.controller.persistence.services.ProgressMessageService;
import org.cloudfoundry.multiapps.controller.process.dynatrace.DynatracePublisher;
import org.cloudfoundry.multiapps.controller.process.flowable.AbortProcessAction;
import org.cloudfoundry.multiapps.controller.process.flowable.Action;
import org.cloudfoundry.multiapps.controller.process.flowable.AdditionalProcessAction;
import org.cloudfoundry.multiapps.controller.process.flowable.FlowableFacade;
import org.cloudfoundry.multiapps.controller.process.flowable.ProcessActionRegistry;
import org.cloudfoundry.multiapps.controller.process.util.ProcessConflictPreventer;
import org.flowable.common.engine.api.FlowableOptimisticLockingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class OperationsCleanerTest {

    private static final Date EXPIRATION_TIME = new Date(5000);
    private static final long TIME_BEFORE_EXPIRATION_1 = 2000;
    private static final long TIME_BEFORE_EXPIRATION_2 = 3000;
    private static final String OPERATION_ID_1 = "1";
    private static final String OPERATION_ID_2 = "2";
    private static final String OPERATION_ID_3 = "3";
    private static final int PAGE_SIZE = 2;

    @Mock
    private OperationService operationService;
    @Mock
    private ProgressMessageService progressMessageService;
    @Mock
    private DynatracePublisher dynatracePublisher;
    @Mock(answer = Answers.RETURNS_SELF)
    private OperationQuery operationQuery;
    @Mock
    private FlowableFacade flowableFacade;
    @Mock
    private ProcessActionRegistry registry;
    @Mock
    private HistoricOperationEventService historicOperationEventService;
    @Mock
    private CloudControllerClientProvider cloudControllerClientProvider;
    @InjectMocks
    private OperationsCleaner cleaner;

    @BeforeEach
    void initMocks() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        cleaner.withPageSize(PAGE_SIZE);

        when(registry.getAction(Action.ABORT)).thenReturn(new AbortProcessActionMock(flowableFacade,
                                                                                     Collections.emptyList(),
                                                                                     historicOperationEventService,
                                                                                     operationService,
                                                                                     cloudControllerClientProvider,
                                                                                     progressMessageService,
                                                                                     dynatracePublisher));
    }

    @Test
    void testExpiredOperationsAreAborted() {
        Operation operation1 = ImmutableOperation.builder()
                                                 .processId(OPERATION_ID_1)
                                                 .startedAt(epochMillisToZonedDateTime(TIME_BEFORE_EXPIRATION_1))
                                                 .build();
        Operation operation2 = ImmutableOperation.builder()
                                                 .processId(OPERATION_ID_2)
                                                 .startedAt(epochMillisToZonedDateTime(TIME_BEFORE_EXPIRATION_2))
                                                 .build();
        List<Operation> operationsList = List.of(operation1, operation2);
        when(operationService.createQuery()).thenReturn(operationQuery);
        initQueryMockForPage(0, operationsList);

        cleaner.execute(EXPIRATION_TIME);
        verify(historicOperationEventService).add(ImmutableHistoricOperationEvent.of(OPERATION_ID_1,
                                                                                     HistoricOperationEvent.EventType.ABORTED));
        verify(historicOperationEventService).add(ImmutableHistoricOperationEvent.of(OPERATION_ID_2,
                                                                                     HistoricOperationEvent.EventType.ABORTED));
    }

    @Test
    void testAbortResilience() {
        Operation operation1 = ImmutableOperation.builder()
                                                 .processId(OPERATION_ID_1)
                                                 .startedAt(epochMillisToZonedDateTime(TIME_BEFORE_EXPIRATION_1))
                                                 .state(null)
                                                 .build();
        Operation operation2 = ImmutableOperation.builder()
                                                 .processId(OPERATION_ID_2)
                                                 .startedAt(epochMillisToZonedDateTime(TIME_BEFORE_EXPIRATION_2))
                                                 .state(null)
                                                 .build();
        List<Operation> operationsList = List.of(operation1, operation2);

        when(operationService.createQuery()).thenReturn(operationQuery);
        initQueryMockForPage(0, operationsList);
        doThrow(new FlowableOptimisticLockingException("I'm an exception")).when(flowableFacade)
                                                                           .deleteProcessInstance(eq(OPERATION_ID_1), any());

        cleaner.execute(EXPIRATION_TIME);
        verify(historicOperationEventService).add(ImmutableHistoricOperationEvent.of(OPERATION_ID_1,
                                                                                     HistoricOperationEvent.EventType.ABORTED));
        verify(historicOperationEventService).add(ImmutableHistoricOperationEvent.of(OPERATION_ID_2,
                                                                                     HistoricOperationEvent.EventType.ABORTED));
    }

    @Test
    void testPaging() {
        Operation operation1 = ImmutableOperation.builder()
                                                 .processId(OPERATION_ID_1)
                                                 .startedAt(epochMillisToZonedDateTime(TIME_BEFORE_EXPIRATION_1))
                                                 .state(null)
                                                 .build();
        Operation operation2 = ImmutableOperation.builder()
                                                 .processId(OPERATION_ID_2)
                                                 .startedAt(epochMillisToZonedDateTime(TIME_BEFORE_EXPIRATION_2))
                                                 .state(null)
                                                 .build();
        Operation operation3 = ImmutableOperation.builder()
                                                 .processId(OPERATION_ID_3)
                                                 .startedAt(epochMillisToZonedDateTime(TIME_BEFORE_EXPIRATION_2))
                                                 .state(null)
                                                 .build();
        List<Operation> operationsPage1 = List.of(operation1, operation2);
        List<Operation> operationsPage2 = List.of(operation3);

        when(operationService.createQuery()).thenReturn(operationQuery);
        initQueryMockForPage(0, operationsPage1);
        initQueryMockForPage(1, operationsPage2);

        cleaner.execute(EXPIRATION_TIME);
        verify(historicOperationEventService).add(ImmutableHistoricOperationEvent.of(OPERATION_ID_1,
                                                                                     HistoricOperationEvent.EventType.ABORTED));
        verify(historicOperationEventService).add(ImmutableHistoricOperationEvent.of(OPERATION_ID_2,
                                                                                     HistoricOperationEvent.EventType.ABORTED));
        verify(historicOperationEventService).add(ImmutableHistoricOperationEvent.of(OPERATION_ID_3,
                                                                                     HistoricOperationEvent.EventType.ABORTED));
    }

    @Test
    void testPagingAllOperationsAreIterated() {
        Operation operation1 = ImmutableOperation.builder()
                                                 .processId(OPERATION_ID_1)
                                                 .startedAt(epochMillisToZonedDateTime(TIME_BEFORE_EXPIRATION_1))
                                                 .state(Operation.State.FINISHED)
                                                 .build();
        Operation operation2 = ImmutableOperation.builder()
                                                 .processId(OPERATION_ID_2)
                                                 .startedAt(epochMillisToZonedDateTime(TIME_BEFORE_EXPIRATION_2))
                                                 .state(Operation.State.FINISHED)
                                                 .build();
        Operation operation3 = ImmutableOperation.builder()
                                                 .processId(OPERATION_ID_3)
                                                 .startedAt(epochMillisToZonedDateTime(TIME_BEFORE_EXPIRATION_2))
                                                 .state(null)
                                                 .build();
        List<Operation> operationsPage1 = List.of(operation1, operation2);
        List<Operation> operationsPage2 = List.of(operation3);

        when(operationService.createQuery()).thenReturn(operationQuery);
        initQueryMockForPage(0, operationsPage1);
        initQueryMockForPage(1, operationsPage2);

        cleaner.execute(EXPIRATION_TIME);
        verify(historicOperationEventService, never()).add(ImmutableHistoricOperationEvent.of(OPERATION_ID_1,
                                                                                              HistoricOperationEvent.EventType.ABORTED));
        verify(historicOperationEventService, never()).add(ImmutableHistoricOperationEvent.of(OPERATION_ID_2,
                                                                                              HistoricOperationEvent.EventType.ABORTED));
        verify(historicOperationEventService).add(ImmutableHistoricOperationEvent.of(OPERATION_ID_3,
                                                                                     HistoricOperationEvent.EventType.ABORTED));
    }

    private void initQueryMockForPage(int pageIndex, List<Operation> result) {
        OperationQuery queryMock = createOperationQueryMock(pageIndex);
        when(queryMock.list()).thenReturn(result);
    }

    private OperationQuery createOperationQueryMock(int pageIndex) {
        return new MockBuilder<>(operationQuery).on(query -> query.startedBefore(EXPIRATION_TIME))
                                                .on(query -> query.offsetOnSelect(pageIndex * PAGE_SIZE))
                                                .on(query -> query.limitOnSelect(PAGE_SIZE))
                                                .on(query -> query.orderByProcessId(any()))
                                                .build();
    }

    @Test
    void testExpiredOperationsAreDeleted() {
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
                                      HistoricOperationEventService historicOperationEventService, OperationService operationService,
                                      CloudControllerClientProvider cloudControllerClientProvider,
                                      ProgressMessageService progressMessageService, DynatracePublisher dynatracePublisher) {
            super(flowableFacade,
                  additionalProcessActions,
                  historicOperationEventService,
                  operationService,
                  cloudControllerClientProvider,
                  progressMessageService,
                  dynatracePublisher);
        }

        @Override
        protected ProcessConflictPreventer getProcessConflictPreventer() {
            return Mockito.mock(ProcessConflictPreventer.class);
        }
    }

}