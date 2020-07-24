package com.sap.cloud.lm.sl.cf.process.listeners;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventType;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.event.FlowableCancelledEvent;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.mock.MockDelegateExecution;
import com.sap.cloud.lm.sl.cf.process.util.OperationInFinalStateHandler;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;

public class AbortProcessListenerTest {

    private final OperationInFinalStateHandler eventHandler = Mockito.mock(OperationInFinalStateHandler.class);
    private final DelegateExecution execution = MockDelegateExecution.createSpyInstance();

    private final AbortProcessListener abortProcessListenerWithContext = new AbortProcessListenerMock(eventHandler, execution);
    private final AbortProcessListener abortProcessListener = new AbortProcessListenerMock(eventHandler, null);

    @Test
    public void testWithWrongEventClass() {
        abortProcessListenerWithContext.onEvent(Mockito.mock(FlowableEvent.class));
        Mockito.verifyNoInteractions(eventHandler);
    }

    @Test
    public void testWithEntityCreatedEvent() {
        FlowableEngineEntityEvent entityCreatedEvent = mockFlowableEngineEvent(FlowableEngineEntityEvent.class,
                                                                               FlowableEngineEventType.ENTITY_CREATED);
        abortProcessListenerWithContext.onEvent(entityCreatedEvent);
        Mockito.verifyNoInteractions(eventHandler);
    }

    @Test
    public void testWithEntityDeletedEvent() {
        FlowableEngineEntityEvent entityDeletedEvent = mockFlowableEngineEvent(FlowableEngineEntityEvent.class,
                                                                               FlowableEngineEventType.ENTITY_DELETED);
        mockEntity(entityDeletedEvent);
        abortProcessListenerWithContext.onEvent(entityDeletedEvent);
        Mockito.verify(eventHandler)
               .handle(execution, Operation.State.ABORTED);
    }

    @Test
    public void testWithProcessCancelledEvent() {
        abortProcessListenerWithContext.onEvent(mockFlowableEngineEvent(FlowableCancelledEvent.class,
                                                                        FlowableEngineEventType.PROCESS_CANCELLED));
        Mockito.verify(eventHandler)
               .handle(execution, Operation.State.ABORTED);
    }

    @Test
    public void testWithoutContext() {
        abortProcessListener.onEvent(mockFlowableEngineEvent(FlowableCancelledEvent.class, FlowableEngineEventType.PROCESS_CANCELLED));
        Mockito.verifyNoInteractions(eventHandler);
    }

    private static <T extends FlowableEngineEvent> T mockFlowableEngineEvent(Class<T> classOfT, FlowableEventType type) {
        T event = Mockito.mock(classOfT);
        Mockito.when(event.getType())
               .thenReturn(type);
        return event;
    }

    private static void mockEntity(FlowableEngineEntityEvent event) {
        ExecutionEntity executionEntity = Mockito.mock(ExecutionEntity.class);
        Mockito.when(executionEntity.isProcessInstanceType())
               .thenReturn(true);
        Mockito.when(executionEntity.getDeleteReason())
               .thenReturn(Constants.PROCESS_ABORTED);
        Mockito.when(event.getEntity())
               .thenReturn(executionEntity);
    }

    private static class AbortProcessListenerMock extends AbortProcessListener {

        private final DelegateExecution execution;

        private AbortProcessListenerMock(OperationInFinalStateHandler eventHandler, DelegateExecution execution) {
            super(eventHandler);
            this.execution = execution;
        }

        @Override
        protected DelegateExecution getExecution(FlowableEngineEvent event) {
            return execution;
        }

    }

}
