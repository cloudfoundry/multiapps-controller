package com.sap.cloud.lm.sl.cf.process.listeners;

import javax.inject.Inject;
import javax.inject.Named;

import org.flowable.common.engine.api.delegate.event.AbstractFlowableEventListener;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.engine.HistoryService;
import org.flowable.engine.impl.context.Context;

import com.sap.cloud.lm.sl.cf.core.cf.CloudControllerClientProvider;
import com.sap.cloud.lm.sl.cf.process.util.ClientReleaser;
import com.sap.cloud.lm.sl.cf.process.util.FlowableExceptionEventHandler;

@Named("errorProcessListener")
public class ErrorProcessListener extends AbstractFlowableEventListener {

    @Inject
    protected CloudControllerClientProvider clientProvider;
    @Inject
    protected FlowableExceptionEventHandler flowableExceptionEventHandler;

    @Override
    public void onEvent(FlowableEvent event) {
        if (event instanceof FlowableEngineEvent) {
            handleEngineEvent((FlowableEngineEvent) event);
        }
    }

    private void handleEngineEvent(FlowableEngineEvent event) {
        flowableExceptionEventHandler.handle(event);
        releaseClient(event);
    }

    private void releaseClient(FlowableEngineEvent event) {
        HistoryService historyService = Context.getProcessEngineConfiguration()
                                               .getHistoryService();
        ClientReleaser clientReleaser = new ClientReleaser(clientProvider);
        clientReleaser.releaseClientFor(historyService, event.getProcessInstanceId());
    }

    @Override
    public boolean isFailOnException() {
        return false;
    }

}
