package org.cloudfoundry.multiapps.controller.process.listeners;

import org.cloudfoundry.multiapps.controller.api.model.ProcessType;
import org.flowable.engine.delegate.DelegateExecution;

public interface MeteringEventPublisher {

    void publishStarted(DelegateExecution execution, ProcessType processType);

    void publishFinalState(DelegateExecution execution, ProcessType processType);
}
