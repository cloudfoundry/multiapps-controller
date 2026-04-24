package org.cloudfoundry.multiapps.controller.process.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.cloudfoundry.multiapps.common.ParsingException;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.api.model.ImmutableOperation;
import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.core.test.MockBuilder;
import org.cloudfoundry.multiapps.controller.persistence.model.HistoricOperationEvent;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableProgressMessage;
import org.cloudfoundry.multiapps.controller.persistence.model.ProgressMessage.ProgressMessageType;
import org.cloudfoundry.multiapps.controller.persistence.query.ProgressMessageQuery;
import org.cloudfoundry.multiapps.controller.persistence.query.impl.OperationQueryImpl;
import org.cloudfoundry.multiapps.controller.persistence.services.HistoricOperationEventService;
import org.cloudfoundry.multiapps.controller.persistence.services.OperationService;
import org.cloudfoundry.multiapps.controller.persistence.services.ProgressMessageService;
import org.cloudfoundry.multiapps.controller.process.flowable.FlowableFacade;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEvent;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityImpl;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ExecutionQuery;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class OperationInErrorStateHandlerTest {

    @Mock
    private ProgressMessageService progressMessageServiceMock;
    @Mock(answer = Answers.RETURNS_SELF)
    private ProgressMessageQuery progressMessageQuery;
    @Mock
    private FlowableFacade flowableFacadeMock;
    @Mock
    private HistoricOperationEventService historicOperationEventServiceMock;
    @Mock
    private ProcessEngineConfiguration processEngineConfigurationMock;
    @Mock
    private ClientReleaser clientReleaserMock;
    @Mock
    private OperationService operationService;
    @Mock
    private OperationQueryImpl operationQuery;

    private final Date now = DateTime.now()
                                     .toDate();

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        when(progressMessageServiceMock.createQuery())
               .thenReturn(progressMessageQuery);
        prepareOperationService();
    }

    @Test
    void testToEventType() {
        OperationInErrorStateHandler handler = mockHandler();
        Throwable throwable = new RuntimeException(new SLException(new IOException()));
        HistoricOperationEvent.EventType eventType = handler.toEventType(throwable);
        assertEquals(HistoricOperationEvent.EventType.FAILED_BY_INFRASTRUCTURE_ERROR, eventType);
    }

    @Test
    void testToEventTypeWithContentException() {
        OperationInErrorStateHandler handler = mockHandler();
        Throwable throwable = new RuntimeException(new SLException(new IOException(new ParsingException(""))));
        HistoricOperationEvent.EventType eventType = handler.toEventType(throwable);
        assertEquals(HistoricOperationEvent.EventType.FAILED_BY_CONTENT_ERROR, eventType);
    }

    @Test
    void testWithErrorMessageAlreadyPersisted() {
        when(flowableFacadeMock.getProcessInstanceId(any()))
               .thenReturn("foo");
        ProgressMessageQuery queryMock = new MockBuilder<>(progressMessageQuery).on(query -> query.processId("foo"))
                                                                                .build();
        doReturn(List.of(ImmutableProgressMessage.builder()
                                                         .processId("foo")
                                                         .taskId("")
                                                         .text("")
                                                         .type(ProgressMessageType.ERROR)
                                                         .build()))
               .when(queryMock)
               .list();
        FlowableEngineEvent event = mock(FlowableEngineEvent.class);

        OperationInErrorStateHandler handler = mockHandler();
        handler.handle(event, new Exception("test-message"));

        verify(progressMessageServiceMock, never())
               .add(any());
        assertErrorStateSet();
    }

    @Test
    void testWithNoErrorMessagePersistedAndTaskIdFromFlowableEngine() {
        testWithNoErrorMessageWithExecutionEntity(true);
    }

    @Test
    void testWithNoErrorMessageAndTaskIdFromContext() {
        when(flowableFacadeMock.getCurrentTaskId("bar"))
               .thenReturn("barbar");

        testWithNoErrorMessageWithExecutionEntity(false);
    }

    private void testWithNoErrorMessageWithExecutionEntity(boolean shouldUseExecutionEntity) {
        when(flowableFacadeMock.getProcessInstanceId(anyString()))
               .thenReturn("foo");
        ProgressMessageQuery queryMock = new MockBuilder<>(progressMessageQuery).on(query -> query.processId("foo"))
                                                                                .build();
        doReturn(Collections.emptyList())
               .when(queryMock)
               .list();

        FlowableEngineEvent engineEvent = mock(FlowableEngineEvent.class);
        when(engineEvent.getExecutionId())
               .thenReturn("bar");
        when(engineEvent.getProcessInstanceId())
               .thenReturn("foo");
        when(engineEvent.getProcessDefinitionId())
               .thenReturn("testing");

        RuntimeService runtimeServiceMock = mock(RuntimeService.class);
        ExecutionQuery executionQueryMock = mock(ExecutionQuery.class);
        when(executionQueryMock.executionId("bar"))
               .thenReturn(executionQueryMock);

        when(executionQueryMock.processInstanceId("foo"))
               .thenReturn(executionQueryMock);

        getExecutionEntityMock(shouldUseExecutionEntity, executionQueryMock);

        when(runtimeServiceMock.createExecutionQuery())
               .thenReturn(executionQueryMock);

        when(processEngineConfigurationMock.getRuntimeService())
               .thenReturn(runtimeServiceMock);

        OperationInErrorStateHandler handler = mockHandler();

        handler.handle(engineEvent, new Exception("test-message"));

        verify(progressMessageServiceMock, times(1))
               .add(ImmutableProgressMessage.builder()
                                            .processId("foo")
                                            .taskId("barbar")
                                            .type(ProgressMessageType.ERROR)
                                            .text("Unexpected error: test-message")
                                            .timestamp(now)
                                            .build());
        assertErrorStateSet();
    }

    private void getExecutionEntityMock(boolean shouldUseExecutionEntity, ExecutionQuery executionQueryMock) {
        ExecutionEntityImpl executionEntity = getExecutionEntity(shouldUseExecutionEntity);

        when(executionQueryMock.list())
               .thenReturn(getList(executionEntity));
    }

    private List<Execution> getList(ExecutionEntityImpl executionEntity) {
        return executionEntity == null ? Collections.emptyList() : List.of(executionEntity);
    }

    private ExecutionEntityImpl getExecutionEntity(boolean shouldUseExecutionEntity) {
        if (shouldUseExecutionEntity) {
            ExecutionEntityImpl executionEntityImpl = new ExecutionEntityImpl();
            executionEntityImpl.setActivityId("barbar");
            return executionEntityImpl;
        }

        return null;
    }

    private void prepareOperationService() {
        Operation operation = ImmutableOperation.builder()
                                                .state(Operation.State.RUNNING)
                                                .build();
        when(operationService.createQuery())
               .thenReturn(operationQuery);
        when(operationQuery.processId(anyString()))
               .thenReturn(operationQuery);
        when(operationQuery.singleResult())
               .thenReturn(operation);
    }

    private void assertErrorStateSet() {
        Operation errorOperation = ImmutableOperation.builder()
                                                     .state(Operation.State.ERROR)
                                                     .build();
        verify(operationService)
               .update(errorOperation, errorOperation);
    }

    private OperationInErrorStateHandlerMock mockHandler() {
        return new OperationInErrorStateHandlerMock(progressMessageServiceMock,
                                                    flowableFacadeMock,
                                                    historicOperationEventServiceMock,
                                                    clientReleaserMock,
                                                    operationService).withProcessEngineConfiguration(processEngineConfigurationMock);
    }

    private class OperationInErrorStateHandlerMock extends OperationInErrorStateHandler {

        private ProcessEngineConfiguration processEngineConfiguration;

        public OperationInErrorStateHandlerMock(ProgressMessageService progressMessageService, FlowableFacade flowableFacade,
                                                HistoricOperationEventService historicOperationEventService,
                                                ClientReleaser clientReleaser, OperationService operationService) {
            super(progressMessageService, flowableFacade, historicOperationEventService, clientReleaser, operationService);
        }

        public OperationInErrorStateHandlerMock withProcessEngineConfiguration(ProcessEngineConfiguration processEngineConfiguration) {
            this.processEngineConfiguration = processEngineConfiguration;
            return this;
        }

        @Override
        protected ProcessEngineConfiguration getProcessEngineConfiguration() {
            return processEngineConfiguration;
        }

        @Override
        protected Date getCurrentTimestamp() {
            return now;
        }
    }

}
