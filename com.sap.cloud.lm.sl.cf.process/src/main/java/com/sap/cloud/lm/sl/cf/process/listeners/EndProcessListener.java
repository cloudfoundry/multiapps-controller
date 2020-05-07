package com.sap.cloud.lm.sl.cf.process.listeners;

import javax.inject.Inject;
import javax.inject.Named;

import org.flowable.engine.delegate.DelegateExecution;

import com.sap.cloud.lm.sl.cf.process.util.OperationInFinalStateHandler;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;

@Named("endProcessListener")
public class EndProcessListener extends AbstractProcessExecutionListener {

    private static final long serialVersionUID = 2L;

    private final OperationInFinalStateHandler eventHandler;

    @Inject
    public EndProcessListener(OperationInFinalStateHandler eventHandler) {
        this.eventHandler = eventHandler;
    }

    @Override
    protected void notifyInternal(DelegateExecution execution) {
        if (isRootProcess(execution)) {
            eventHandler.handle(execution, Operation.State.FINISHED);
        }
    }

}
