package org.cloudfoundry.multiapps.controller.process.util;

import org.cloudfoundry.multiapps.controller.process.steps.AsyncExecution;
import org.cloudfoundry.multiapps.controller.process.steps.PollServiceKeyCreationOperationExecution;
import org.cloudfoundry.multiapps.controller.process.steps.PollServiceKeyDeletionLastOperationExecution;
import org.cloudfoundry.multiapps.controller.process.steps.PollServiceKeyDeletionOperationExecution;
import org.cloudfoundry.multiapps.controller.process.steps.PollServiceKeyLastOperationExecution;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

import com.sap.cloudfoundry.client.facade.domain.ServiceCredentialBindingOperation;

public class ServiceKeyPollingFactory extends ServiceBindingPollingFactory {

    public ServiceKeyPollingFactory(ProcessContext context, ServiceCredentialBindingOperation.Type serviceBindingOperationType) {
        super(context, serviceBindingOperationType);
    }

    @Override
    protected AsyncExecution createPollingExecutionWithTypeCreate() {
        if (getContext().getVariable(Variables.USE_LAST_OPERATION_FOR_SERVICE_KEY_CREATION)) {
            return new PollServiceKeyLastOperationExecution();
        }
        return new PollServiceKeyCreationOperationExecution();
    }

    @Override
    protected AsyncExecution createPollingExecutionWithTypeDelete() {
        if (getContext().getVariable(Variables.USE_LAST_OPERATION_FOR_SERVICE_KEY_DELETION)) {
            return new PollServiceKeyDeletionLastOperationExecution();
        }
        return new PollServiceKeyDeletionOperationExecution();
    }

}
