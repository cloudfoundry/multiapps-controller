package com.sap.cloud.lm.sl.cf.process.util;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEvent;
import org.flowable.common.engine.api.delegate.event.FlowableExceptionEvent;
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

import com.sap.cloud.lm.sl.cf.core.persistence.query.ProgressMessageQuery;
import com.sap.cloud.lm.sl.cf.core.persistence.service.ProgressMessageService;
import com.sap.cloud.lm.sl.cf.core.util.MockBuilder;
import com.sap.cloud.lm.sl.cf.persistence.model.ImmutableProgressMessage;
import com.sap.cloud.lm.sl.cf.persistence.model.ProgressMessage.ProgressMessageType;
import com.sap.cloud.lm.sl.cf.process.flowable.FlowableFacade;

public class FlowableExceptionEventHandlerTest {

    @Mock
    private ProgressMessageService progressMessageServiceMock;
    @Mock(answer = Answers.RETURNS_SELF)
    private ProgressMessageQuery progressMessageQuery;
    @Mock
    private FlowableFacade flowableFacadeMock;
    @Mock
    private HistoricOperationEventPersister historicOperationEventPersisterMock;

    private final Date now = DateTime.now()
                                     .toDate();

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testWithEventWhichIsNotCorrectType() {
        testSimpleWithEventAndExceptionEvent(Mockito.mock(FlowableEngineEvent.class));

        Mockito.verifyZeroInteractions(progressMessageServiceMock, flowableFacadeMock);
    }

    @Test
    public void testWithEmptyExceptionMessage() {
        FlowableExceptionEvent event = Mockito.mock(FlowableExceptionEvent.class, Mockito.withSettings()
                                                                                         .extraInterfaces(FlowableEngineEvent.class));
        Mockito.when(event.getCause())
               .thenReturn(new Exception());

        testSimpleWithEventAndExceptionEvent((FlowableEngineEvent) event);

        Mockito.verifyZeroInteractions(progressMessageServiceMock, flowableFacadeMock);
    }

    @Test
    public void testWithErrorMessageAlreadyPresented() {
        Mockito.when(flowableFacadeMock.getProcessInstanceId(Mockito.any()))
               .thenReturn("foo");
        Mockito.when(progressMessageServiceMock.createQuery())
               .thenReturn(progressMessageQuery);
        ProgressMessageQuery queryMock = new MockBuilder<>(progressMessageQuery).on(query -> query.processId("foo"))
                                                                                .build();
        Mockito.doReturn(Collections.singletonList(ImmutableProgressMessage.builder()
                                                                           .processId("foo")
                                                                           .taskId("")
                                                                           .text("")
                                                                           .type(ProgressMessageType.ERROR)
                                                                           .build()))
               .when(queryMock)
               .list();
        FlowableExceptionEvent event = Mockito.mock(FlowableExceptionEvent.class, Mockito.withSettings()
                                                                                         .extraInterfaces(FlowableEngineEvent.class));
        Mockito.when(event.getCause())
               .thenReturn(new Exception("test-message"));
        FlowableExceptionEventHandler handler = new FlowableExceptionEventHandlerMock(progressMessageServiceMock,
                                                                                      flowableFacadeMock,
                                                                                      historicOperationEventPersisterMock);
        handler.handle((FlowableEngineEvent) event);

        Mockito.verify(progressMessageServiceMock, Mockito.never())
               .add(Mockito.any());
    }

    @Test
    public void testWithNoErrorMessagePresentedAndTaskIdFromFlowableEngine() {
        testWithNoErrorMessageWithExecutionEntity(true);
    }

    @Test
    public void testWithNoErrorMessageAndTaskIdFromContext() {
        Mockito.when(flowableFacadeMock.getCurrentTaskId("bar"))
               .thenReturn("barbar");

        testWithNoErrorMessageWithExecutionEntity(false);
    }

    private void testWithNoErrorMessageWithExecutionEntity(boolean shouldUseExecutionEntity) {
        Mockito.when(flowableFacadeMock.getProcessInstanceId(Mockito.anyString()))
               .thenReturn("foo");
        Mockito.when(progressMessageServiceMock.createQuery())
               .thenReturn(progressMessageQuery);

        ProgressMessageQuery queryMock = new MockBuilder<>(progressMessageQuery).on(query -> query.processId("foo"))
                                                                                .build();

        Mockito.doReturn(Collections.emptyList())
               .when(queryMock)
               .list();

        FlowableExceptionEvent exceptionEvent = Mockito.mock(FlowableExceptionEvent.class, Mockito.withSettings()
                                                                                                  .extraInterfaces(FlowableEngineEvent.class));
        FlowableEngineEvent engineEvent = (FlowableEngineEvent) exceptionEvent;
        Mockito.when(exceptionEvent.getCause())
               .thenReturn(new Exception("test-message"));
        Mockito.when(engineEvent.getExecutionId())
               .thenReturn("bar");
        Mockito.when(engineEvent.getProcessInstanceId())
               .thenReturn("foo");
        Mockito.when(engineEvent.getProcessDefinitionId())
               .thenReturn("testing");

        ProcessEngineConfiguration mockProcessEngineConfiguration = Mockito.mock(ProcessEngineConfiguration.class);
        RuntimeService runtimeServiceMock = Mockito.mock(RuntimeService.class);
        ExecutionQuery executionQueryMock = Mockito.mock(ExecutionQuery.class);
        Mockito.when(executionQueryMock.executionId("bar"))
               .thenReturn(executionQueryMock);

        Mockito.when(executionQueryMock.processInstanceId("foo"))
               .thenReturn(executionQueryMock);

        getExecutionEntityMock(shouldUseExecutionEntity, executionQueryMock);

        Mockito.when(runtimeServiceMock.createExecutionQuery())
               .thenReturn(executionQueryMock);

        Mockito.when(mockProcessEngineConfiguration.getRuntimeService())
               .thenReturn(runtimeServiceMock);

        FlowableExceptionEventHandler handler = new FlowableExceptionEventHandlerMock(progressMessageServiceMock,
                                                                                      flowableFacadeMock,
                                                                                      historicOperationEventPersisterMock).withProcessEngineConfiguration(mockProcessEngineConfiguration);

        handler.handle(engineEvent);

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
        return executionEntity == null ? Collections.emptyList() : Collections.singletonList(executionEntity);
    }

    private ExecutionEntityImpl getExecutionEntity(boolean shouldUseExecutionEntity) {
        if (shouldUseExecutionEntity) {
            ExecutionEntityImpl executionEntityImpl = new ExecutionEntityImpl();
            executionEntityImpl.setActivityId("barbar");
            return executionEntityImpl;
        }

        return null;
    }

    private void testSimpleWithEventAndExceptionEvent(FlowableEngineEvent event) {
        FlowableExceptionEventHandler handler = new FlowableExceptionEventHandlerMock(null, null, null);
        handler.handle(event);
    }

    private class FlowableExceptionEventHandlerMock extends FlowableExceptionEventHandler {

        private ProcessEngineConfiguration processEngineConfiguration;

        public FlowableExceptionEventHandlerMock(ProgressMessageService progressMessageService, FlowableFacade flowableFacade,

                                                 HistoricOperationEventPersister historicOperationEventPersister) {
            super(progressMessageService, flowableFacade, historicOperationEventPersister);
        }

        public FlowableExceptionEventHandlerMock withProcessEngineConfiguration(ProcessEngineConfiguration processEngineConfiguration) {
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
