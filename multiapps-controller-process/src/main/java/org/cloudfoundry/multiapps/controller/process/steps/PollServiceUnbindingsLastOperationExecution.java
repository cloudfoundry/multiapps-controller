package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.List;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceBinding;

public class PollServiceUnbindingsLastOperationExecution extends PollServiceBindingsLastOperationExecution {

    @Override
    protected AsyncExecutionState checkServiceBindingOperationState(List<CloudServiceBinding> serviceBindings, ProcessContext context) {
        if (serviceBindings.isEmpty()) {
            return AsyncExecutionState.FINISHED;
        }
        return super.checkServiceBindingOperationState(serviceBindings, context);
    }

}
