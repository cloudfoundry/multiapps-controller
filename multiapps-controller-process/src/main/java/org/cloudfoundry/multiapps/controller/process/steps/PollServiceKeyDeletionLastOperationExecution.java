package org.cloudfoundry.multiapps.controller.process.steps;

import com.sap.cloudfoundry.client.facade.domain.CloudServiceKey;

public class PollServiceKeyDeletionLastOperationExecution extends PollServiceKeyLastOperationExecution {

    @Override
    protected AsyncExecutionState checkServiceKeyLastOperation(CloudServiceKey serviceKey, ProcessContext context) {
        if (serviceKey == null) {
            return AsyncExecutionState.FINISHED;
        }
        return super.checkServiceKeyLastOperation(serviceKey, context);
    }
}
