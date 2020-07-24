package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.cf.clients.ServiceUpdater;
import org.cloudfoundry.multiapps.controller.core.model.ServiceOperation;
import org.cloudfoundry.multiapps.controller.core.util.MethodExecution;
import org.cloudfoundry.multiapps.controller.core.util.MethodExecution.ExecutionState;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.ServiceOperationGetter;
import org.cloudfoundry.multiapps.controller.process.util.ServiceProgressReporter;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

public abstract class ServiceStep extends AsyncFlowableStep {

    @Inject
    @Named("serviceUpdater")
    private ServiceUpdater serviceUpdater;

    @Inject
    private ServiceOperationGetter serviceOperationGetter;
    @Inject
    private ServiceProgressReporter serviceProgressReporter;

    @Override
    protected StepPhase executeAsyncStep(ProcessContext context) {
        CloudServiceInstanceExtended serviceToProcess = context.getVariable(Variables.SERVICE_TO_PROCESS);
        MethodExecution<String> methodExecution = executeOperationAndHandleExceptions(context, context.getControllerClient(),
                                                                                      serviceToProcess);
        if (methodExecution.getState()
                           .equals(ExecutionState.FINISHED)) {
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

    private MethodExecution<String> executeOperationAndHandleExceptions(ProcessContext context, CloudControllerClient controllerClient,
                                                                        CloudServiceInstanceExtended service) {
        try {
            return executeOperation(context, controllerClient, service);
        } catch (CloudOperationException e) {
            String serviceUpdateFailedMessage = MessageFormat.format(Messages.FAILED_SERVICE_UPDATE, service.getName(), e.getStatusText());
            throw new CloudOperationException(e.getStatusCode(), serviceUpdateFailedMessage, e.getDescription(), e);
        }
    }

    protected abstract MethodExecution<String> executeOperation(ProcessContext context, CloudControllerClient controllerClient,
                                                                CloudServiceInstanceExtended service);

    protected abstract ServiceOperation.Type getOperationType();

    protected ServiceUpdater getServiceUpdater() {
        return serviceUpdater;
    }

    protected ServiceOperationGetter getServiceOperationGetter() {
        return serviceOperationGetter;
    }

    protected ServiceProgressReporter getServiceProgressReporter() {
        return serviceProgressReporter;
    }

}
