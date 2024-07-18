package org.cloudfoundry.multiapps.controller.process.listeners;

import jakarta.inject.Named;

import org.flowable.engine.delegate.event.AbstractFlowableEngineEventListener;

@Named("abortProcessStatisticsListener")
public class AbortProcessStatisticsListener extends AbstractFlowableEngineEventListener {

    @Override
    public boolean isFailOnException() {
        return false;
    }

}
