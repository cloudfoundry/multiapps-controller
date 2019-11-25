package com.sap.cloud.lm.sl.cf.process.listeners;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableExceptionEvent;
import org.flowable.job.service.impl.persistence.entity.DeadLetterJobEntity;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.process.util.OperationInErrorStateHandler;

public class ErrorProcessListenerTest {

    private static final String ERROR_MESSAGE = "Error!";

    private final OperationInErrorStateHandler eventHandler = Mockito.mock(OperationInErrorStateHandler.class);
    private final ErrorProcessListener errorProcessListener = new ErrorProcessListener(eventHandler);

    @Test
    public void testJobExecutionFailureWithWrongEventClass() {
        FlowableEngineEntityEvent engineEntityEvent = Mockito.mock(FlowableEngineEntityEvent.class);
        errorProcessListener.jobExecutionFailure(engineEntityEvent);
        Mockito.verifyZeroInteractions(eventHandler);
    }

    @Test
    public void testJobExecutionFailureWithNoException() {
        FlowableEngineEntityEvent engineEntityEvent = Mockito.mock(FlowableEngineEntityEvent.class, Mockito.withSettings()
                                                                                                           .extraInterfaces(FlowableExceptionEvent.class));
        errorProcessListener.jobExecutionFailure(engineEntityEvent);
        Mockito.verifyZeroInteractions(eventHandler);
    }

    @Test
    public void testJobExecutionFailure() {
        FlowableEngineEntityEvent engineEntityEvent = Mockito.mock(FlowableEngineEntityEvent.class, Mockito.withSettings()
                                                                                                           .extraInterfaces(FlowableExceptionEvent.class));
        FlowableExceptionEvent exceptionEvent = (FlowableExceptionEvent) engineEntityEvent;
        Throwable t = new Throwable();
        Mockito.when(exceptionEvent.getCause())
               .thenReturn(t);

        errorProcessListener.jobExecutionFailure(engineEntityEvent);
        Mockito.verify(eventHandler)
               .handle(engineEntityEvent, t);
        Mockito.verifyZeroInteractions(eventHandler);
    }

    @Test
    public void testEntityCreatedWithWrongEntityClass() {
        FlowableEngineEntityEvent engineEntityEvent = Mockito.mock(FlowableEngineEntityEvent.class);
        JobEntity job = Mockito.mock(JobEntity.class);
        Mockito.when(engineEntityEvent.getEntity())
               .thenReturn(job);

        errorProcessListener.entityCreated(engineEntityEvent);
        Mockito.verifyZeroInteractions(eventHandler);
    }

    @Test
    public void testEntityCreatedWithNoException() {
        FlowableEngineEntityEvent engineEntityEvent = Mockito.mock(FlowableEngineEntityEvent.class);
        DeadLetterJobEntity job = Mockito.mock(DeadLetterJobEntity.class);
        Mockito.when(engineEntityEvent.getEntity())
               .thenReturn(job);

        errorProcessListener.entityCreated(engineEntityEvent);
        Mockito.verifyZeroInteractions(eventHandler);
    }

    @Test
    public void testEntityCreated() {
        FlowableEngineEntityEvent engineEntityEvent = Mockito.mock(FlowableEngineEntityEvent.class);
        DeadLetterJobEntity job = Mockito.mock(DeadLetterJobEntity.class);
        Mockito.when(engineEntityEvent.getEntity())
               .thenReturn(job);
        Mockito.when(job.getExceptionMessage())
               .thenReturn(ERROR_MESSAGE);

        errorProcessListener.entityCreated(engineEntityEvent);
        Mockito.verify(eventHandler)
               .handle(engineEntityEvent, ERROR_MESSAGE);
        Mockito.verifyZeroInteractions(eventHandler);
    }

}
