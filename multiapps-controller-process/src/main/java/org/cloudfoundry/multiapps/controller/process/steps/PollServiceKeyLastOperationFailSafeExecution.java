package org.cloudfoundry.multiapps.controller.process.steps;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceKey;
import org.cloudfoundry.multiapps.controller.process.Messages;

public class PollServiceKeyLastOperationFailSafeExecution extends PollServiceKeyLastOperationExecution {

    @Override
    protected AsyncExecutionState doOnError(CloudServiceKey serviceKey, ProcessContext context) {
        context.getStepLogger()
               .warnWithoutProgressMessage(Messages.OPERATION_FOR_SERVICE_KEY_0_FAILED_WITH_DESCRIPTION_1, serviceKey.getName(),
                                           serviceKey.getServiceKeyOperation()
                                                     .getDescription());
        return AsyncExecutionState.FINISHED;
    }
}
