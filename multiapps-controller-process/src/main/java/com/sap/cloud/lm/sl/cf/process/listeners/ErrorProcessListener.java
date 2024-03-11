package com.sap.cloud.lm.sl.cf.process.listeners;

import javax.inject.Inject;

import org.flowable.common.engine.api.delegate.event.AbstractFlowableEventListener;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.cf.CloudControllerClientProvider;
import com.sap.cloud.lm.sl.cf.persistence.services.ProgressMessageService;
import com.sap.cloud.lm.sl.cf.process.util.ClientReleaser;
import com.sap.cloud.lm.sl.cf.process.util.FlowableExceptionEventHandler;

@Component("errorProcessListener")
public class ErrorProcessListener extends AbstractFlowableEventListener {

    @Inject
    protected CloudControllerClientProvider clientProvider;

    @Inject
    private ProgressMessageService progressMessageService;

    @Override
    public void onEvent(FlowableEvent event) {
        FlowableExceptionEventHandler handler = new FlowableExceptionEventHandler(progressMessageService);
        handler.handle(event);

        if (event instanceof FlowableEngineEvent) {
            ClientReleaser clientReleaser = new ClientReleaser((FlowableEngineEvent) event, clientProvider);
            clientReleaser.releaseClient();
        }
    }

    @Override
    public boolean isFailOnException() {
        return false;
    }

}
