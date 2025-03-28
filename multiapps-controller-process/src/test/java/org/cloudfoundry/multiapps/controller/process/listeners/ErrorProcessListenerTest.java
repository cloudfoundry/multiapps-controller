package org.cloudfoundry.multiapps.controller.process.listeners;

import org.cloudfoundry.multiapps.controller.process.util.MockDelegateExecution;
import org.cloudfoundry.multiapps.controller.process.util.OperationInErrorStateHandler;
import org.cloudfoundry.multiapps.controller.process.variables.VariableHandling;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEvent;
import org.flowable.common.engine.api.delegate.event.FlowableExceptionEvent;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.job.service.impl.persistence.entity.DeadLetterJobEntity;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class ErrorProcessListenerTest {

    private static final String ERROR_MESSAGE = "Error!";

    private static class ErrorProcessListenerMock extends ErrorProcessListener {

        public ErrorProcessListenerMock(OperationInErrorStateHandler eventHandler) {
            super(eventHandler);
        }

        protected DelegateExecution execution;

        @Override
        protected DelegateExecution getExecution(FlowableEngineEvent event) {
            return execution;
        }

    }

    @Mock
    private OperationInErrorStateHandler eventHandler;

    @InjectMocks
    private ErrorProcessListenerMock errorProcessListener;

    @BeforeEach
    void initMocks() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
    }

    @Test
    void testJobExecutionFailureWithWrongEventClass() {
        FlowableEngineEntityEvent engineEntityEvent = Mockito.mock(FlowableEngineEntityEvent.class);
        errorProcessListener.jobExecutionFailure(engineEntityEvent);
        Mockito.verifyNoInteractions(eventHandler);
    }

    @Test
    void testJobExecutionFailureWithNoException() {
        FlowableEngineEntityEvent engineEntityEvent = Mockito.mock(FlowableEngineEntityEvent.class, Mockito.withSettings()
                                                                                                           .extraInterfaces(
                                                                                                               FlowableExceptionEvent.class));
        errorProcessListener.jobExecutionFailure(engineEntityEvent);
        Mockito.verifyNoInteractions(eventHandler);
    }

    @Test
    void testJobExecutionFailure() {
        FlowableEngineEntityEvent engineEntityEvent = Mockito.mock(FlowableEngineEntityEvent.class, Mockito.withSettings()
                                                                                                           .extraInterfaces(
                                                                                                               FlowableExceptionEvent.class));
        FlowableExceptionEvent exceptionEvent = (FlowableExceptionEvent) engineEntityEvent;
        Throwable t = new Throwable();
        Mockito.when(exceptionEvent.getCause())
               .thenReturn(t);
        errorProcessListener.jobExecutionFailure(engineEntityEvent);
        Mockito.verify(eventHandler)
               .handle(engineEntityEvent, t);
    }

    @Test
    void testEntityCreatedWithWrongEntityClass() {
        FlowableEngineEntityEvent engineEntityEvent = Mockito.mock(FlowableEngineEntityEvent.class);
        JobEntity job = Mockito.mock(JobEntity.class);
        Mockito.when(engineEntityEvent.getEntity())
               .thenReturn(job);

        errorProcessListener.entityCreated(engineEntityEvent);
        Mockito.verifyNoInteractions(eventHandler);
    }

    @Test
    void testEntityCreatedWithNoException() {
        FlowableEngineEntityEvent engineEntityEvent = Mockito.mock(FlowableEngineEntityEvent.class);
        DeadLetterJobEntity job = Mockito.mock(DeadLetterJobEntity.class);
        Mockito.when(engineEntityEvent.getEntity())
               .thenReturn(job);

        errorProcessListener.entityCreated(engineEntityEvent);
        Mockito.verifyNoInteractions(eventHandler);
    }

    @Test
    void testEntityCreated() {
        FlowableEngineEntityEvent engineEntityEvent = Mockito.mock(FlowableEngineEntityEvent.class);
        DeadLetterJobEntity job = Mockito.mock(DeadLetterJobEntity.class);
        Mockito.when(engineEntityEvent.getEntity())
               .thenReturn(job);
        Mockito.when(job.getExceptionMessage())
               .thenReturn(ERROR_MESSAGE);

        errorProcessListener.entityCreated(engineEntityEvent);
        Mockito.verify(eventHandler)
               .handle(engineEntityEvent, ERROR_MESSAGE);
    }

    @Test
    void testHandlingWithCorrelationId() {
        errorProcessListener.execution = mockExecutionWithCorrelationId();
        testEntityCreated();
        testJobExecutionFailure();
    }

    private DelegateExecution mockExecutionWithCorrelationId() {
        DelegateExecution execution = MockDelegateExecution.createSpyInstance();
        VariableHandling.set(execution, Variables.CORRELATION_ID, "abc");
        return execution;
    }

}
