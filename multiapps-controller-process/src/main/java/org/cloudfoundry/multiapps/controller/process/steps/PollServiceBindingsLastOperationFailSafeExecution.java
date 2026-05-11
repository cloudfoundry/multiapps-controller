package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.List;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceBinding;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

public class PollServiceBindingsLastOperationFailSafeExecution extends PollServiceBindingsLastOperationExecution {

    @Override
    protected AsyncExecutionState completePollingOfFailedOperation(List<CloudServiceBinding> failedServiceBindings,
                                                                   ProcessContext context) {
        if (failedServiceBindings.size() == 1) {
            context.getStepLogger()
                   .warnWithoutProgressMessage(Messages.SERVICE_BINDING_0_EXISTS_IN_BROKEN_STATE_WILL_BE_RECREATED,
                                               failedServiceBindings.get(0)
                                                                    .getGuid());
        } else {
            context.getStepLogger()
                   .warnWithoutProgressMessage(Messages.SERVICE_BINDINGS_EXISTS_IN_BROKEN_STATE_WILL_BE_RECREATED,
                                               failedServiceBindings.stream()
                                                                    .map(CloudServiceBinding::getGuid)
                                                                    .toList());
        }
        context.setVariable(Variables.SHOULD_RECREATE_SERVICE_BINDING, true);
        return AsyncExecutionState.FINISHED;
    }
}
