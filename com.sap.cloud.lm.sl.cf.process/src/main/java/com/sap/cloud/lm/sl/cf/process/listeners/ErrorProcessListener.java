package com.sap.cloud.lm.sl.cf.process.listeners;

import javax.inject.Inject;
import javax.inject.Named;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEvent;
import org.flowable.common.engine.api.delegate.event.FlowableExceptionEvent;
import org.flowable.engine.delegate.event.AbstractFlowableEngineEventListener;
import org.flowable.job.service.impl.persistence.entity.DeadLetterJobEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.process.util.OperationInErrorStateHandler;

@Named("errorProcessListener")
public class ErrorProcessListener extends AbstractFlowableEngineEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorProcessListener.class);

    private final OperationInErrorStateHandler eventHandler;

    @Inject
    public ErrorProcessListener(OperationInErrorStateHandler eventHandler) {
        this.eventHandler = eventHandler;
    }

    @Override
    public boolean isFailOnException() {
        return false;
    }

    @Override
    protected void entityCreated(FlowableEngineEntityEvent event) {
        Object entity = event.getEntity();
        if (entity instanceof DeadLetterJobEntity) {
            reportError(event, (DeadLetterJobEntity) entity);
        }
    }

    @Override
    protected void jobExecutionFailure(FlowableEngineEntityEvent event) {
        if (event instanceof FlowableExceptionEvent) {
            reportError(event, (FlowableExceptionEvent) event);
        }
    }

    private void reportError(FlowableEngineEvent event, DeadLetterJobEntity entity) {
        if (entity.getExceptionMessage() == null) {
            LOGGER.error("Job execution failure detected for process \"{}\" (definition: \"{}\"), but the dead letter job does not contain an exception.",
                         event.getProcessInstanceId(), event.getProcessDefinitionId());
        } else {
            LOGGER.error("Job execution failure detected for process \"{}\" (definition: \"{}\"): {}", //
                         event.getProcessInstanceId(), event.getProcessDefinitionId(), entity.getExceptionStacktrace());
            eventHandler.handle(event, entity.getExceptionMessage());
        }
    }

    private void reportError(FlowableEngineEvent event, FlowableExceptionEvent exceptionEvent) {
        Throwable throwable = exceptionEvent.getCause();
        if (throwable == null) {
            LOGGER.error("Job execution failure detected for process \"{}\" (definition: \"{}\"), but the exception event does not contain an exception.",
                         event.getProcessInstanceId(), event.getProcessDefinitionId());
        } else {
            LOGGER.error("Job execution failure detected for process \"{}\" (definition: \"{}\").", //
                         event.getProcessInstanceId(), event.getProcessDefinitionId(), throwable);
            eventHandler.handle(event, throwable);
        }
    }

}
