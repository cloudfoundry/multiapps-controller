package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.model.ServiceOperation;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.ServiceOperationGetter;
import org.cloudfoundry.multiapps.controller.process.util.ServiceProgressReporter;
import org.cloudfoundry.multiapps.controller.process.util.StepLogger;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

public class PollServiceCreateOrUpdateOperationsExecution extends PollServiceOperationsExecution implements AsyncExecution {

    public PollServiceCreateOrUpdateOperationsExecution(ServiceOperationGetter serviceOperationGetter,
                                                        ServiceProgressReporter serviceProgressReporter) {
        super(serviceOperationGetter, serviceProgressReporter);
    }

    @Override
    protected List<CloudServiceInstanceExtended> getServicesData(ProcessContext context) {
        List<CloudServiceInstanceExtended> allServicesToCreate = context.getVariable(Variables.SERVICES_TO_CREATE);
        // There's no need to poll the creation or update of user-provided services, because it is done synchronously:
        return allServicesToCreate.stream()
                                  .filter(s -> !s.isUserProvided())
                                  .collect(Collectors.toList());
    }

    @Override
    protected ServiceOperation mapOperationState(StepLogger stepLogger, ServiceOperation lastServiceOperation,
                                                 CloudServiceInstanceExtended service) {
        lastServiceOperation = super.mapOperationState(stepLogger, lastServiceOperation, service);
        // Be fault tolerant on failure on update of service
        if (lastServiceOperation.getType() == ServiceOperation.Type.UPDATE
            && lastServiceOperation.getState() == ServiceOperation.State.FAILED) {
            stepLogger.warn(Messages.FAILED_SERVICE_UPDATE, service.getName(), lastServiceOperation.getDescription());
            return new ServiceOperation(lastServiceOperation.getType(),
                                        lastServiceOperation.getDescription(),
                                        ServiceOperation.State.SUCCEEDED);
        }
        return lastServiceOperation;
    }

    @Override
    protected void handleMissingOperationState(StepLogger stepLogger, CloudServiceInstanceExtended service) {
        if (!service.isOptional()) {
            throw new SLException(Messages.CANNOT_RETRIEVE_INSTANCE_OF_SERVICE, service.getName());
        }
        // Here we're assuming that we cannot retrieve the service instance, because its creation was synchronous and it failed. If that
        // is really the case, then showing a warning progress message to the user is unnecessary, since one should have been shown back
        // in CreateOrUpdateServicesStep.
        stepLogger.warnWithoutProgressMessage(Messages.CANNOT_RETRIEVE_SERVICE_INSTANCE_OF_OPTIONAL_SERVICE, service.getName());
    }

    @Override
    protected void reportServiceState(ProcessContext context, CloudServiceInstanceExtended service, ServiceOperation lastServiceOperation) {
        if (lastServiceOperation.getState() == ServiceOperation.State.SUCCEEDED) {
            context.getStepLogger()
                   .debug(getSuccessMessage(service, lastServiceOperation.getType()));
            return;
        }

        if (lastServiceOperation.getState() == ServiceOperation.State.FAILED) {
            handleFailedState(context.getStepLogger(), service, lastServiceOperation);
        }
    }

    private String getSuccessMessage(CloudServiceInstanceExtended service, ServiceOperation.Type type) {
        switch (type) {
            case CREATE:
                return MessageFormat.format(Messages.SERVICE_CREATED, service.getName());
            case UPDATE:
                return MessageFormat.format(Messages.SERVICE_UPDATED, service.getName());
            default:
                throw new IllegalStateException(MessageFormat.format(org.cloudfoundry.multiapps.controller.core.Messages.ILLEGAL_SERVICE_OPERATION_TYPE,
                                                                     type));
        }
    }

    private void handleFailedState(StepLogger stepLogger, CloudServiceInstanceExtended service, ServiceOperation lastServiceOperation) {
        if (shouldFail(service)) {
            throw new SLException(getFailureMessage(service, lastServiceOperation));
        }
        stepLogger.warn(getWarningMessage(service, lastServiceOperation));
    }

    private boolean shouldFail(CloudServiceInstanceExtended service) {
        return !service.isOptional();
    }

    private String getFailureMessage(CloudServiceInstanceExtended service, ServiceOperation operation) {
        switch (operation.getType()) {
            case CREATE:
                return MessageFormat.format(Messages.ERROR_CREATING_SERVICE, service.getName(), service.getLabel(), service.getPlan(),
                                            operation.getDescription());
            case UPDATE:
                return MessageFormat.format(Messages.ERROR_UPDATING_SERVICE, service.getName(), service.getLabel(), service.getPlan(),
                                            operation.getDescription());
            default:
                throw new IllegalStateException(MessageFormat.format(org.cloudfoundry.multiapps.controller.core.Messages.ILLEGAL_SERVICE_OPERATION_TYPE,
                                                                     operation.getType()));
        }
    }

    private String getWarningMessage(CloudServiceInstanceExtended service, ServiceOperation operation) {
        switch (operation.getType()) {
            case CREATE:
                return MessageFormat.format(Messages.ERROR_CREATING_OPTIONAL_SERVICE, service.getName(), service.getLabel(),
                                            service.getPlan(), operation.getDescription());
            case UPDATE:
                return MessageFormat.format(Messages.ERROR_UPDATING_OPTIONAL_SERVICE, service.getName(), service.getLabel(),
                                            service.getPlan(), operation.getDescription());
            default:
                throw new IllegalStateException(MessageFormat.format(org.cloudfoundry.multiapps.controller.core.Messages.ILLEGAL_SERVICE_OPERATION_TYPE,
                                                                     operation.getType()));
        }
    }

    public String getPollingErrorMessage(ProcessContext context) {
        return Messages.ERROR_MONITORING_CREATION_OR_UPDATE_OF_SERVICES;
    }
}
