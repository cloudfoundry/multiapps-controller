package org.cloudfoundry.multiapps.controller.process.listeners;

import java.text.MessageFormat;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.OperationInFinalStateHandler;
import org.cloudfoundry.multiapps.controller.process.util.ProcessTypeParser;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEvent;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.event.AbstractFlowableEngineEventListener;
import org.flowable.engine.delegate.event.FlowableCancelledEvent;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named("abortProcessListener")
public class AbortProcessListener extends AbstractFlowableEngineEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbortProcessListener.class);

    private final OperationInFinalStateHandler eventHandler;
    protected final ProcessTypeParser processTypeParser;

    @Inject
    public AbortProcessListener(OperationInFinalStateHandler eventHandler, ProcessTypeParser processTypeParser) {
        this.eventHandler = eventHandler;
        this.processTypeParser = processTypeParser;
    }

    @Override
    public boolean isFailOnException() {
        return false;
    }

    @Override
    protected void processCancelled(FlowableCancelledEvent event) {
        execute(event);
    }

    @Override
    protected void entityDeleted(FlowableEngineEntityEvent event) {
        if (!hasCorrectEntityType(event)) {
            return;
        }
        execute(event);
    }

    private void execute(FlowableEngineEvent event) {
        DelegateExecution execution = getExecution(event);
        if (execution == null) {
            LOGGER.warn(MessageFormat.format(Messages.CANNOT_GET_CONTEXT_FOR_EVENT_0_AND_PROCESS_1, event.getType(),
                                             event.getProcessInstanceId()));
            return;
        }
        eventHandler.handle(execution, processTypeParser.getProcessType(execution), Operation.State.ABORTED);
    }

    private static boolean hasCorrectEntityType(FlowableEngineEntityEvent event) {
        if (!(event.getEntity() instanceof ExecutionEntity)) {
            return false;
        }
        ExecutionEntity executionEntity = (ExecutionEntity) event.getEntity();
        return executionEntity.isProcessInstanceType() && Operation.State.ABORTED.name()
                                                                                 .equals(executionEntity.getDeleteReason());
    }

}
