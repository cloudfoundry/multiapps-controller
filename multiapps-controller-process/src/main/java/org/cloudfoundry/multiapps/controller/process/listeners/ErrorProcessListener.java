package org.cloudfoundry.multiapps.controller.process.listeners;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.core.util.LoggingUtil;
import org.cloudfoundry.multiapps.controller.process.util.OperationInErrorStateHandler;
import org.cloudfoundry.multiapps.controller.process.variables.VariableHandling;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEvent;
import org.flowable.common.engine.api.delegate.event.FlowableExceptionEvent;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.event.AbstractFlowableEngineEventListener;
import org.flowable.job.service.impl.persistence.entity.DeadLetterJobEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            handleWithCorrelationId(event, () -> handle(event, (DeadLetterJobEntity) entity));
        }
    }

    private void handleWithCorrelationId(FlowableEngineEvent event, Runnable handlerFunction) {
        DelegateExecution execution = getExecution(event);
        if (execution != null) {
            LoggingUtil.logWithCorrelationId(VariableHandling.get(execution, Variables.CORRELATION_ID), handlerFunction);
            return;
        }
        handlerFunction.run();
    }

    private void handle(FlowableEngineEvent event, DeadLetterJobEntity entity) {
        if (entity.getExceptionMessage() == null) {
            LOGGER.error("Dead letter job detected for process \"{}\" (definition: \"{}\"), but it does not contain an exception.",
                         event.getProcessInstanceId(), event.getProcessDefinitionId());
        } else {
            LOGGER.error("Dead letter job detected for process \"{}\" (definition: \"{}\"): {}", event.getProcessInstanceId(),
                         event.getProcessDefinitionId(), entity.getExceptionStacktrace());
            eventHandler.handle(event, entity.getExceptionMessage());
        }

    }

    @Override
    protected void jobExecutionFailure(FlowableEngineEntityEvent event) {
        if (event instanceof FlowableExceptionEvent) {
            handleWithCorrelationId(event, () -> handle(event, (FlowableExceptionEvent) event));
        }
    }

    private void handle(FlowableEngineEvent event, FlowableExceptionEvent exceptionEvent) {
        Throwable throwable = exceptionEvent.getCause();
        if (throwable == null) {
            LOGGER.error("Job execution failure detected for process \"{}\" (definition: \"{}\"), but the exception event does not contain an exception.",
                         event.getProcessInstanceId(), event.getProcessDefinitionId());
        } else {
            LOGGER.error("Job execution failure detected for process \"{}\" (definition: \"{}\").", event.getProcessInstanceId(),
                         event.getProcessDefinitionId(), throwable);
            eventHandler.handle(event, throwable);
        }
    }
}
