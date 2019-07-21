package com.sap.cloud.lm.sl.cf.process.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableExceptionEvent;
import org.flowable.common.engine.impl.event.FlowableEngineEventImpl;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityImpl;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ExecutionQuery;
import org.flowable.idm.api.event.FlowableIdmEventType;
import org.flowable.idm.engine.delegate.event.impl.FlowableIdmEventImpl;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.sap.cloud.lm.sl.cf.core.dao.ProgressMessageDao;
import com.sap.cloud.lm.sl.cf.persistence.model.ImmutableProgressMessage;
import com.sap.cloud.lm.sl.cf.persistence.model.ProgressMessage.ProgressMessageType;
import com.sap.cloud.lm.sl.cf.process.flowable.FlowableFacade;

public class FlowableExceptionEventHandlerTest {

    @Mock
    private ProgressMessageDao progressMessageDaoMock;

    @Mock
    private FlowableFacade flowableFacadeMock;

    private Date now = DateTime.now()
        .toDate();

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testWithEventWhichIsNotCorrectType() {
        testSimpleWithEventAndExceptionEvent(new FlowableIdmEventImpl(FlowableIdmEventType.CUSTOM), null);

        Mockito.verifyZeroInteractions(progressMessageDaoMock, flowableFacadeMock);
    }

    @Test
    public void testWithEmptyExceptionMessage() {
        FlowableExceptionEvent mockedExceptionEvent = Mockito.mock(FlowableExceptionEvent.class);
        Mockito.when(mockedExceptionEvent.getCause())
            .thenReturn(new Exception());

        testSimpleWithEventAndExceptionEvent(new FlowableEngineEventImpl(FlowableEngineEventType.CUSTOM), mockedExceptionEvent);

        Mockito.verifyZeroInteractions(progressMessageDaoMock, flowableFacadeMock);
    }

    @Test
    public void testWithErrorMessageAlreadyPresented() {
        Mockito.when(flowableFacadeMock.getProcessInstanceId(Mockito.anyString()))
            .thenReturn("foo");
        Mockito.when(progressMessageDaoMock.find("foo"))
            .thenReturn(Arrays.asList(ImmutableProgressMessage.builder()
                .processId("foo")
                .taskId("")
                .text("")
                .type(ProgressMessageType.ERROR)
                .build()));
        FlowableExceptionEvent mockedExceptionEvent = Mockito.mock(FlowableExceptionEvent.class);
        Mockito.when(mockedExceptionEvent.getCause())
            .thenReturn(new Exception("test-message"));
        FlowableExceptionEventHandler handler = new FlowableExceptionEventHandlerMock(progressMessageDaoMock, flowableFacadeMock,
            mockedExceptionEvent);
        handler.handle(new FlowableEngineEventImpl(FlowableEngineEventType.CUSTOM));

        Mockito.verify(progressMessageDaoMock, Mockito.never())
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

        Mockito.when(progressMessageDaoMock.find("foo"))
            .thenReturn(Collections.emptyList());

        FlowableExceptionEvent mockedExceptionEvent = Mockito.mock(FlowableExceptionEvent.class);

        Mockito.when(mockedExceptionEvent.getCause())
            .thenReturn(new Exception("test-message"));

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

        FlowableExceptionEventHandler handler = new FlowableExceptionEventHandlerMock(progressMessageDaoMock, flowableFacadeMock,
            mockedExceptionEvent).withProcessEngineConfiguration(mockProcessEngineConfiguration);

        handler.handle(new FlowableEngineEventImpl(FlowableEngineEventType.CUSTOM, "bar", "foo", "testing"));

        Mockito.verify(progressMessageDaoMock, Mockito.times(1))
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
        return executionEntity == null ? Collections.emptyList() : Arrays.asList(executionEntity);
    }

    private ExecutionEntityImpl getExecutionEntity(boolean shouldUseExecutionEntity) {
        if (shouldUseExecutionEntity) {
            ExecutionEntityImpl executionEntityImpl = new ExecutionEntityImpl();
            executionEntityImpl.setActivityId("barbar");
            return executionEntityImpl;
        }

        return null;
    }

    private void testSimpleWithEventAndExceptionEvent(FlowableEvent event, FlowableExceptionEvent exceptionEvent) {
        FlowableExceptionEventHandler handler = new FlowableExceptionEventHandlerMock(null, null, exceptionEvent);
        handler.handle(event);
    }

    private class FlowableExceptionEventHandlerMock extends FlowableExceptionEventHandler {

        private FlowableExceptionEvent flowableExceptionEvent;
        private ProcessEngineConfiguration processEngineConfiguration;

        public FlowableExceptionEventHandlerMock(ProgressMessageDao progressMessageDao, FlowableFacade flowableFacade,
            FlowableExceptionEvent flowableExceptionEvent) {
            super(progressMessageDao, flowableFacade);
            this.flowableExceptionEvent = flowableExceptionEvent;
        }

        public FlowableExceptionEventHandlerMock withProcessEngineConfiguration(ProcessEngineConfiguration processEngineConfiguration) {
            this.processEngineConfiguration = processEngineConfiguration;
            return this;
        }

        @Override
        protected FlowableExceptionEvent getFlowableExceptionEvent(FlowableEvent event) {
            return flowableExceptionEvent;
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
