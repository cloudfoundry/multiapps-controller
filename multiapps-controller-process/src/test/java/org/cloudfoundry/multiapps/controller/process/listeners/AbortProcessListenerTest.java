package org.cloudfoundry.multiapps.controller.process.listeners;

import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.api.model.ProcessType;
import org.cloudfoundry.multiapps.controller.process.util.MockDelegateExecution;
import org.cloudfoundry.multiapps.controller.process.util.OperationInFinalStateHandler;
import org.cloudfoundry.multiapps.controller.process.util.ProcessTypeParser;
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

class AbortProcessListenerTest {

    private final OperationInFinalStateHandler eventHandler = Mockito.mock(OperationInFinalStateHandler.class);
    private final ProcessTypeParser processTypeParser = Mockito.mock(ProcessTypeParser.class);
    private final DelegateExecution execution = MockDelegateExecution.createSpyInstance();

    private final AbortProcessListener abortProcessListenerWithContext = new AbortProcessListenerMock(eventHandler,
                                                                                                      processTypeParser,
                                                                                                      execution);
    private final AbortProcessListener abortProcessListener = new AbortProcessListenerMock(eventHandler, processTypeParser, null);

    @Test
    void testWithWrongEventClass() {
        abortProcessListenerWithContext.onEvent(Mockito.mock(FlowableEvent.class));
        Mockito.verifyNoInteractions(eventHandler);
    }

    @Test
    void testWithEntityCreatedEvent() {
        FlowableEngineEntityEvent entityCreatedEvent = mockFlowableEngineEvent(FlowableEngineEntityEvent.class,
                                                                               FlowableEngineEventType.ENTITY_CREATED);
        abortProcessListenerWithContext.onEvent(entityCreatedEvent);
        Mockito.verifyNoInteractions(eventHandler);
    }

    @Test
    void testWithEntityDeletedEvent() {
        FlowableEngineEntityEvent entityDeletedEvent = mockFlowableEngineEvent(FlowableEngineEntityEvent.class,
                                                                               FlowableEngineEventType.ENTITY_DELETED);
        mockEntity(entityDeletedEvent);
        Mockito.when(processTypeParser.getProcessType(execution, false))
               .thenReturn(ProcessType.DEPLOY);
        abortProcessListenerWithContext.onEvent(entityDeletedEvent);
        Mockito.verify(eventHandler)
               .handle(execution, ProcessType.DEPLOY, Operation.State.ABORTED);
    }

    @Test
    void testWithProcessCancelledEvent() {
        Mockito.when(processTypeParser.getProcessType(execution, false))
               .thenReturn(ProcessType.DEPLOY);
        abortProcessListenerWithContext.onEvent(mockFlowableEngineEvent(FlowableCancelledEvent.class,
                                                                        FlowableEngineEventType.PROCESS_CANCELLED));
        Mockito.verify(eventHandler)
               .handle(execution, ProcessType.DEPLOY, Operation.State.ABORTED);
    }

    @Test
    void testWithoutContext() {
        abortProcessListener.onEvent(mockFlowableEngineEvent(FlowableCancelledEvent.class, FlowableEngineEventType.PROCESS_CANCELLED));
        Mockito.verifyNoInteractions(eventHandler);
    }

    private static <T extends FlowableEngineEvent> T mockFlowableEngineEvent(Class<T> classOfT, FlowableEventType type) {
        T event = Mockito.mock(classOfT);
        Mockito.when(event.getType())
               .thenReturn(type);
        return event;
    }

    private void mockEntity(FlowableEngineEntityEvent event) {
        ExecutionEntity executionEntity = Mockito.mock(ExecutionEntity.class);
        Mockito.when(executionEntity.isProcessInstanceType())
               .thenReturn(true);
        Mockito.when(executionEntity.getDeleteReason())
               .thenReturn(Operation.State.ABORTED.name());
        Mockito.when(event.getEntity())
               .thenReturn(executionEntity);
    }

    private static class AbortProcessListenerMock extends AbortProcessListener {

        private final DelegateExecution execution;

        private AbortProcessListenerMock(OperationInFinalStateHandler eventHandler, ProcessTypeParser processTypeParser,
                                         DelegateExecution execution) {
            super(eventHandler, processTypeParser);
            this.execution = execution;
        }

        @Override
        protected DelegateExecution getExecution(FlowableEngineEvent event) {
            return execution;
        }

    }

}
