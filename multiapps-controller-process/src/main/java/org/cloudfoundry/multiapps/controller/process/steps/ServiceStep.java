package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Inject;

import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.util.OperationExecutionState;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.ServiceOperationGetter;
import org.cloudfoundry.multiapps.controller.process.util.ServiceProgressReporter;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.http.HttpStatus;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.CloudControllerException;
import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.CloudServiceBrokerException;
import com.sap.cloudfoundry.client.facade.domain.ServiceOperation;

public abstract class ServiceStep extends AsyncFlowableStep {

    @Inject
    private ServiceOperationGetter serviceOperationGetter;
    @Inject
    private ServiceProgressReporter serviceProgressReporter;

    @Override
    protected StepPhase executeAsyncStep(ProcessContext context) {
        CloudServiceInstanceExtended serviceToProcess = context.getVariable(Variables.SERVICE_TO_PROCESS);
        OperationExecutionState executionState = executeOperation(context, context.getControllerClient(), serviceToProcess);
        if (executionState == OperationExecutionState.FINISHED) {
            return StepPhase.DONE;
        }

        Map<String, ServiceOperation.Type> serviceOperation = new HashMap<>();
        serviceOperation.put(serviceToProcess.getName(), getOperationType());

        context.getStepLogger()
               .debug(Messages.TRIGGERED_SERVICE_OPERATIONS, JsonUtil.toJson(serviceOperation, true));
        context.setVariable(Variables.TRIGGERED_SERVICE_OPERATIONS, serviceOperation);

        context.setVariable(Variables.IS_SERVICE_UPDATED, true);
        return StepPhase.POLL;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_SERVICE_OPERATION;
    }

    protected abstract OperationExecutionState executeOperation(ProcessContext context, CloudControllerClient controllerClient,
                                                                CloudServiceInstanceExtended service);

    protected void processServiceActionFailure(ProcessContext context, CloudServiceInstanceExtended serviceInstance,
                                               CloudOperationException e) {
        if (serviceInstance.isOptional()) {
            getStepLogger().warn(MessageFormat.format(Messages.ERROR_CREATING_OR_UPDATING_OPTIONAL_SERVICE_INSTANCE, e.getDescription()));
            return;
        }
        if (e.getStatusCode() == HttpStatus.CONFLICT && !context.getVariable(Variables.WAS_SERVICE_BINDING_KEY_OPERATION_ALREADY_DONE)) {
            getStepLogger().warn(Messages.OPERATION_OF_SERVICE_BINDING_OR_KEY_IS_IN_PROGRESS);
            context.setVariable(Variables.IS_SERVICE_BINDING_KEY_OPERATION_IN_PROGRESS, true);
            context.setVariable(Variables.SERVICE_WITH_BIND_IN_PROGRESS, serviceInstance.getName());
            return;
        }
        String detailedMessage = MessageFormat.format(Messages.ERROR_CREATING_OR_UPDATING_SERVICE_INSTANCE, e.getDescription());
        if (e.getStatusCode() == HttpStatus.BAD_GATEWAY) {
            context.setVariable(Variables.SERVICE_OFFERING, serviceInstance.getLabel());
            throw new CloudServiceBrokerException(e.getStatusCode(), e.getStatusText(), detailedMessage);
        }
        throw new CloudControllerException(e.getStatusCode(), e.getStatusText(), detailedMessage);
    }

    protected abstract ServiceOperation.Type getOperationType();

    protected ServiceOperationGetter getServiceOperationGetter() {
        return serviceOperationGetter;
    }

    protected ServiceProgressReporter getServiceProgressReporter() {
        return serviceProgressReporter;
    }

}
