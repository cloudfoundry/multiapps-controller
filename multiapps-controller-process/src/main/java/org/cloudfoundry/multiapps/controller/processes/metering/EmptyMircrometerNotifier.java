package org.cloudfoundry.multiapps.controller.processes.metering;

import org.cloudfoundry.multiapps.controller.api.model.Operation.State;
import org.flowable.engine.delegate.DelegateExecution;

public class EmptyMircrometerNotifier implements MicrometerNotifier {

    @Override
    public void recordOverallTime(DelegateExecution execution, State state, long processDurationInMillis) {
        return;
    }

    @Override
    public void recordStartProcessEvent(DelegateExecution execution) {
        return;
    }

    @Override
    public void recordEndProcessEvent(DelegateExecution execution) {
        return;
    }

    @Override
    public void recordErrorProcessEvent(DelegateExecution execution, String errorMessage) {
        return;
    }

    @Override
    public void clearRegistry() {
        return;
    }

}
