package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import com.sap.cloudfoundry.client.facade.CloudControllerException;
import com.sap.cloudfoundry.client.facade.CloudServiceBrokerException;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.util.OperationExecutionState;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.ServiceOperationGetter;
import org.cloudfoundry.multiapps.controller.process.util.ServiceProgressReporter;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.domain.ServiceOperation;
import org.springframework.http.HttpStatus;

public abstract class ServiceStep extends AsyncFlowableStep {

    @Inject
    private ServiceOperationGetter serviceOperationGetter;
    @Inject
    private ServiceProgressReporter serviceProgressReporter;

    @Override
    protected StepPhase executeAsyncStep(ProcessContext context) {
        CloudServiceInstanceExtended serviceToProcess = context.getVariable(Variables.SERVICE_TO_PROCESS);
        OperationExecutionState executionState = executeOperationAndHandleExceptions(context, serviceToProcess);
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

    private OperationExecutionState executeOperationAndHandleExceptions(ProcessContext context, CloudServiceInstanceExtended service) {
        try {
            return executeOperation(context, context.getControllerClient(), service);
        } catch (CloudOperationException e) {
            String serviceUpdateFailedMessage = MessageFormat.format(Messages.FAILED_SERVICE_UPDATE, service.getName(), e.getStatusText());
            throw new CloudOperationException(e.getStatusCode(), serviceUpdateFailedMessage, e.getDescription(), e);
        }
    }

    protected abstract OperationExecutionState executeOperation(ProcessContext context, CloudControllerClient controllerClient,
                                                                CloudServiceInstanceExtended service);

    protected void processServiceActionFailure(ProcessContext context, CloudServiceInstanceExtended serviceInstance,
                                               CloudOperationException e) {
        if (!serviceInstance.isOptional()) {
            String detailedDescription = MessageFormat.format(Messages.ERROR_CREATING_SERVICE, serviceInstance.getName(),
                    serviceInstance.getLabel(), serviceInstance.getPlan(), e.getDescription());
            if (e.getStatusCode() == HttpStatus.BAD_GATEWAY) {
                context.setVariable(Variables.SERVICE_OFFERING, serviceInstance.getLabel());
                throw new CloudServiceBrokerException(e.getStatusCode(), e.getStatusText(), detailedDescription);
            }
            throw new CloudControllerException(e.getStatusCode(), e.getStatusText(), detailedDescription);
        }
        getStepLogger().warn(e.getDescription());
    }

    protected abstract ServiceOperation.Type getOperationType();

    protected ServiceOperationGetter getServiceOperationGetter() {
        return serviceOperationGetter;
    }

    protected ServiceProgressReporter getServiceProgressReporter() {
        return serviceProgressReporter;
    }

}
