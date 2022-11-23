package org.cloudfoundry.multiapps.controller.process.util;

import org.cloudfoundry.multiapps.controller.process.steps.AsyncExecution;
import org.cloudfoundry.multiapps.controller.process.steps.PollServiceBindingLastOperationExecution;
import org.cloudfoundry.multiapps.controller.process.steps.PollServiceBindingOperationExecution;
import org.cloudfoundry.multiapps.controller.process.steps.PollServiceUnbindingLastOperationExecution;
import org.cloudfoundry.multiapps.controller.process.steps.PollServiceUnbindingOperationExecution;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

import com.sap.cloudfoundry.client.facade.domain.ServiceCredentialBindingOperation;

public class ServiceBindingPollingFactory {

    private final ProcessContext context;
    private final ServiceCredentialBindingOperation.Type serviceBindingOperationType;

    public ServiceBindingPollingFactory(ProcessContext context, ServiceCredentialBindingOperation.Type serviceBindingOperationType) {
        this.context = context;
        this.serviceBindingOperationType = serviceBindingOperationType;
    }

    public AsyncExecution createPollingExecution() {
        if (serviceBindingOperationType == ServiceCredentialBindingOperation.Type.CREATE) {
            return createPollingExecutionWithTypeCreate();
        }
        return createPollingExecutionWithTypeDelete();
    }

    protected AsyncExecution createPollingExecutionWithTypeCreate() {
        if (context.getVariable(Variables.USE_LAST_OPERATION_FOR_SERVICE_BINDING_CREATION)) {
            return new PollServiceBindingLastOperationExecution();
        }
        return new PollServiceBindingOperationExecution();
    }

    protected AsyncExecution createPollingExecutionWithTypeDelete() {
        if (context.getVariable(Variables.USE_LAST_OPERATION_FOR_SERVICE_BINDING_DELETION)) {
            return new PollServiceUnbindingLastOperationExecution();
        }
        return new PollServiceUnbindingOperationExecution();
    }

    protected ProcessContext getContext() {
        return context;
    }
}
