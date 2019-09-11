package com.sap.cloud.lm.sl.cf.process.listeners;

import javax.inject.Inject;
import javax.inject.Named;

import org.flowable.common.engine.api.delegate.event.AbstractFlowableEventListener;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.engine.HistoryService;
import org.flowable.engine.impl.context.Context;

import com.sap.cloud.lm.sl.cf.core.cf.CloudControllerClientProvider;
import com.sap.cloud.lm.sl.cf.core.persistence.service.ProgressMessageService;
import com.sap.cloud.lm.sl.cf.process.flowable.FlowableFacade;
import com.sap.cloud.lm.sl.cf.process.util.ClientReleaser;
import com.sap.cloud.lm.sl.cf.process.util.FlowableExceptionEventHandler;
import com.sap.cloud.lm.sl.cf.process.util.HistoricOperationEventPersister;

@Named("errorProcessListener")
public class ErrorProcessListener extends AbstractFlowableEventListener {

    @Inject
    protected CloudControllerClientProvider clientProvider;

    @Inject
    private ProgressMessageService progressMessageService;

    @Inject
    private FlowableFacade flowableFacade;

    @Inject
    private HistoricOperationEventPersister historicOperationEventPersister;

    @Override
    public void onEvent(FlowableEvent event) {
        FlowableExceptionEventHandler handler = new FlowableExceptionEventHandler(progressMessageService,
                                                                                  flowableFacade,
                                                                                  historicOperationEventPersister);
        handler.handle(event);

        if (event instanceof FlowableEngineEvent) {
            releaseClient((FlowableEngineEvent) event);
        }
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
