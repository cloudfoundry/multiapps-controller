package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;

import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

import com.sap.cloudfoundry.client.facade.domain.CloudServiceBinding;

public class PollServiceBindingLastOperationFailSafeExecution extends PollServiceBindingLastOperationExecution {

    @Override
    protected AsyncExecutionState completePollingOfFailedOperation(CloudServiceBinding serviceBinding, ProcessContext context) {
        context.getStepLogger()
               .warnWithoutProgressMessage(MessageFormat.format(Messages.SERVICE_BINDING_0_EXISTS_IN_BROKEN_STATE_WILL_BE_RECREATED,
                                                                serviceBinding.getGuid()));
        context.setVariable(Variables.SHOULD_RECREATE_SERVICE_BINDING, true);
        return AsyncExecutionState.FINISHED;
    }
}
