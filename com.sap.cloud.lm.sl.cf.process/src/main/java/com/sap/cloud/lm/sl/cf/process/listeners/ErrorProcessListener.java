package com.sap.cloud.lm.sl.cf.process.listeners;

import javax.inject.Inject;

import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.delegate.event.ActivitiEventListener;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.cf.CloudControllerClientProvider;
import com.sap.cloud.lm.sl.cf.persistence.services.ProgressMessageService;
import com.sap.cloud.lm.sl.cf.process.util.ActivitiExceptionEventHandler;
import com.sap.cloud.lm.sl.cf.process.util.ClientReleaser;

@Component("errorProcessListener")
public class ErrorProcessListener implements ActivitiEventListener {

    @Inject
    protected CloudControllerClientProvider clientProvider;

    @Inject
    private ProgressMessageService progressMessageService;

    @Override
    public boolean isFailOnException() {
        return false;
    }

    @Override
    public void onEvent(ActivitiEvent event) {
        ActivitiExceptionEventHandler handler = new ActivitiExceptionEventHandler(progressMessageService);
        handler.handle(event);

        ClientReleaser clientReleaser = new ClientReleaser(event, clientProvider);
        clientReleaser.releaseClient();
    }

}
