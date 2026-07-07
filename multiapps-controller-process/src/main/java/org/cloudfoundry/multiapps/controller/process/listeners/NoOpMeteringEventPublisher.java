package org.cloudfoundry.multiapps.controller.process.listeners;

import jakarta.inject.Named;

import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.api.model.ProcessType;
import org.flowable.engine.delegate.DelegateExecution;

@Named
public class NoOpMeteringEventPublisher implements MeteringEventPublisher {

    @Override
    public void publishStarted(DelegateExecution execution, ProcessType processType) {
    }

    @Override
    public void publishFinalState(DelegateExecution execution, ProcessType processType, Operation.State state) {
    }
}
