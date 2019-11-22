package com.sap.cloud.lm.sl.cf.process.listeners;

import java.text.MessageFormat;

import javax.inject.Inject;
import javax.inject.Named;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEvent;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.event.AbstractFlowableEngineEventListener;
import org.flowable.engine.delegate.event.FlowableCancelledEvent;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.OperationInFinalStateHandler;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;

@Named("abortProcessListener")
public class AbortProcessListener extends AbstractFlowableEngineEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbortProcessListener.class);

    private final OperationInFinalStateHandler eventHandler;

    @Inject
    public AbortProcessListener(OperationInFinalStateHandler eventHandler) {
        this.eventHandler = eventHandler;
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
        DelegateExecution context = getExecution(event);
        if (context == null) {
            LOGGER.warn(MessageFormat.format(Messages.CANNOT_GET_CONTEXT_FOR_EVENT_0_AND_PROCESS_1, event.getType(),
                                             event.getProcessInstanceId()));
            return;
        }
        eventHandler.handle(context, Operation.State.ABORTED);
    }

    private static boolean hasCorrectEntityType(FlowableEngineEntityEvent event) {
        if (!(event.getEntity() instanceof ExecutionEntity)) {
            return false;
        }
        ExecutionEntity executionEntity = (ExecutionEntity) event.getEntity();
        return executionEntity.isProcessInstanceType() && Constants.PROCESS_ABORTED.equals(executionEntity.getDeleteReason());
    }

}
