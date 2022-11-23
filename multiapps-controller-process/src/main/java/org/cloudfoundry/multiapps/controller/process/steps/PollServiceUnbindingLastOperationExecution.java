package org.cloudfoundry.multiapps.controller.process.steps;

import com.sap.cloudfoundry.client.facade.domain.CloudServiceBinding;

public class PollServiceUnbindingLastOperationExecution extends PollServiceBindingLastOperationExecution {

    @Override
    protected AsyncExecutionState checkServiceBindingOperationState(CloudServiceBinding serviceBinding, ProcessContext context) {
        if (serviceBinding == null) {
            return AsyncExecutionState.FINISHED;
        }
        return super.checkServiceBindingOperationState(serviceBinding, context);
    }

}
