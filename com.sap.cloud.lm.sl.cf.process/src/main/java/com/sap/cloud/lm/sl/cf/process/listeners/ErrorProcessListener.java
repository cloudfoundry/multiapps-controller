package com.sap.cloud.lm.sl.cf.process.listeners;

import javax.inject.Inject;

import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.delegate.event.ActivitiEventListener;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.cf.CloudFoundryClientProvider;
import com.sap.cloud.lm.sl.cf.process.util.ClientReleaser;

@Component("errorProcessListener")
public class ErrorProcessListener implements ActivitiEventListener {

    @Inject
    protected CloudFoundryClientProvider clientProvider;

    @Override
    public boolean isFailOnException() {
        return false;
    }

    @Override
    public void onEvent(ActivitiEvent event) {
        ClientReleaser clientReleaser = new ClientReleaser(event, clientProvider);
        clientReleaser.releaseClient();
    }

}
