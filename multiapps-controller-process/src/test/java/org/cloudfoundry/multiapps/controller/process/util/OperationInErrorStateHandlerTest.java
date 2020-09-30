package org.cloudfoundry.multiapps.controller.process.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.cloudfoundry.multiapps.common.ParsingException;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.core.test.MockBuilder;
import org.cloudfoundry.multiapps.controller.persistence.model.HistoricOperationEvent;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableProgressMessage;
import org.cloudfoundry.multiapps.controller.persistence.model.ProgressMessage.ProgressMessageType;
import org.cloudfoundry.multiapps.controller.persistence.query.ProgressMessageQuery;
import org.cloudfoundry.multiapps.controller.persistence.services.HistoricOperationEventService;
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
import org.mockito.Mockito;
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

    private final Date now = DateTime.now()
                                     .toDate();

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        Mockito.when(progressMessageServiceMock.createQuery())
               .thenReturn(progressMessageQuery);
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
        Mockito.when(flowableFacadeMock.getProcessInstanceId(Mockito.any()))
               .thenReturn("foo");
        ProgressMessageQuery queryMock = new MockBuilder<>(progressMessageQuery).on(query -> query.processId("foo"))
                                                                                .build();
        Mockito.doReturn(List.of(ImmutableProgressMessage.builder()
                                                         .processId("foo")
                                                         .taskId("")
                                                         .text("")
                                                         .type(ProgressMessageType.ERROR)
                                                         .build()))
               .when(queryMock)
               .list();
        FlowableEngineEvent event = Mockito.mock(FlowableEngineEvent.class);

        OperationInErrorStateHandler handler = mockHandler();
        handler.handle(event, new Exception("test-message"));

        Mockito.verify(progressMessageServiceMock, Mockito.never())
               .add(Mockito.any());
    }

    @Test
    void testWithNoErrorMessagePersistedAndTaskIdFromFlowableEngine() {
        testWithNoErrorMessageWithExecutionEntity(true);
    }

    @Test
    void testWithNoErrorMessageAndTaskIdFromContext() {
        Mockito.when(flowableFacadeMock.getCurrentTaskId("bar"))
               .thenReturn("barbar");

        testWithNoErrorMessageWithExecutionEntity(false);
    }

    private void testWithNoErrorMessageWithExecutionEntity(boolean shouldUseExecutionEntity) {
        Mockito.when(flowableFacadeMock.getProcessInstanceId(Mockito.anyString()))
               .thenReturn("foo");
        ProgressMessageQuery queryMock = new MockBuilder<>(progressMessageQuery).on(query -> query.processId("foo"))
                                                                                .build();
        Mockito.doReturn(Collections.emptyList())
               .when(queryMock)
               .list();

        FlowableEngineEvent engineEvent = Mockito.mock(FlowableEngineEvent.class);
        Mockito.when(engineEvent.getExecutionId())
               .thenReturn("bar");
        Mockito.when(engineEvent.getProcessInstanceId())
               .thenReturn("foo");
        Mockito.when(engineEvent.getProcessDefinitionId())
               .thenReturn("testing");

        RuntimeService runtimeServiceMock = Mockito.mock(RuntimeService.class);
        ExecutionQuery executionQueryMock = Mockito.mock(ExecutionQuery.class);
        Mockito.when(executionQueryMock.executionId("bar"))
               .thenReturn(executionQueryMock);

        Mockito.when(executionQueryMock.processInstanceId("foo"))
               .thenReturn(executionQueryMock);

        getExecutionEntityMock(shouldUseExecutionEntity, executionQueryMock);

        Mockito.when(runtimeServiceMock.createExecutionQuery())
               .thenReturn(executionQueryMock);

        Mockito.when(processEngineConfigurationMock.getRuntimeService())
               .thenReturn(runtimeServiceMock);

        OperationInErrorStateHandler handler = mockHandler();

        handler.handle(engineEvent, new Exception("test-message"));

        Mockito.verify(progressMessageServiceMock, Mockito.times(1))
               .add(ImmutableProgressMessage.builder()
                                            .processId("foo")
                                            .taskId("barbar")
                                            .type(ProgressMessageType.ERROR)
                                            .text("Unexpected error: test-message")
                                            .timestamp(now)
                                            .build());
    }

    private void getExecutionEntityMock(boolean shouldUseExecutionEntity, ExecutionQuery executionQueryMock) {
        ExecutionEntityImpl executionEntity = getExecutionEntity(shouldUseExecutionEntity);

        Mockito.when(executionQueryMock.list())
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

    private OperationInErrorStateHandlerMock mockHandler() {
        return new OperationInErrorStateHandlerMock(progressMessageServiceMock,
                                                    flowableFacadeMock,
                                                    historicOperationEventServiceMock,
                                                    clientReleaserMock).withProcessEngineConfiguration(processEngineConfigurationMock);
    }

    private class OperationInErrorStateHandlerMock extends OperationInErrorStateHandler {

        private ProcessEngineConfiguration processEngineConfiguration;

        public OperationInErrorStateHandlerMock(ProgressMessageService progressMessageService, FlowableFacade flowableFacade,
                                                HistoricOperationEventService historicOperationEventService,
                                                ClientReleaser clientReleaser) {
            super(progressMessageService, flowableFacade, historicOperationEventService, clientReleaser);
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
