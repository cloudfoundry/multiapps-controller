package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;

import org.flowable.engine.delegate.DelegateExecution;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.model.ServiceOperation;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ServiceOperationGetter;
import com.sap.cloud.lm.sl.cf.process.util.ServiceProgressReporter;
import com.sap.cloud.lm.sl.cf.process.util.StepLogger;
import com.sap.cloud.lm.sl.common.SLException;

public class PollServiceCreateOrUpdateOperationsExecution extends PollServiceOperationsExecution implements AsyncExecution {

    public PollServiceCreateOrUpdateOperationsExecution(ServiceOperationGetter serviceOperationGetter,
                                                        ServiceProgressReporter serviceProgressReporter) {
        super(serviceOperationGetter, serviceProgressReporter);
    }

    @Override
    protected List<CloudServiceExtended> getServicesData(DelegateExecution context) {
        List<CloudServiceExtended> allServicesToCreate = StepsUtil.getServicesToCreate(context);
        // There's no need to poll the creation or update of user-provided services, because it is done synchronously:
        return allServicesToCreate.stream()
                                  .filter(s -> !s.isUserProvided())
                                  .collect(Collectors.toList());
    }

    @Override
    protected ServiceOperation mapOperationState(StepLogger stepLogger, ServiceOperation lastServiceOperation,
                                                 CloudServiceExtended service) {
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
    protected void handleMissingOperationState(StepLogger stepLogger, CloudServiceExtended service) {
        if (!service.isOptional()) {
            throw new SLException(Messages.CANNOT_RETRIEVE_INSTANCE_OF_SERVICE, service.getName());
        }
        // Here we're assuming that we cannot retrieve the service instance, because its creation was synchronous and it failed. If that
        // is really the case, then showing a warning progress message to the user is unnecessary, since one should have been shown back
        // in CreateOrUpdateServicesStep.
        stepLogger.warnWithoutProgressMessage(Messages.CANNOT_RETRIEVE_SERVICE_INSTANCE_OF_OPTIONAL_SERVICE, service.getName());
    }

    @Override
    protected void reportServiceState(ExecutionWrapper execution, CloudServiceExtended service, ServiceOperation lastServiceOperation) {
        if (lastServiceOperation.getState() == ServiceOperation.State.SUCCEEDED) {
            execution.getStepLogger()
                     .debug(getSuccessMessage(service, lastServiceOperation.getType()));
            return;
        }

        if (lastServiceOperation.getState() == ServiceOperation.State.FAILED) {
            handleFailedState(execution.getStepLogger(), service, lastServiceOperation);
        }
    }

    private String getSuccessMessage(CloudServiceExtended service, ServiceOperation.Type type) {
        switch (type) {
            case CREATE:
                return MessageFormat.format(Messages.SERVICE_CREATED, service.getName());
            case UPDATE:
                return MessageFormat.format(Messages.SERVICE_UPDATED, service.getName());
            default:
                throw new IllegalStateException(MessageFormat.format(com.sap.cloud.lm.sl.cf.core.Messages.ILLEGAL_SERVICE_OPERATION_TYPE,
                                                                     type));
        }
    }

    private void handleFailedState(StepLogger stepLogger, CloudServiceExtended service, ServiceOperation lastServiceOperation) {
        if (shouldFail(service)) {
            throw new SLException(getFailureMessage(service, lastServiceOperation));
        }
        stepLogger.warn(getWarningMessage(service, lastServiceOperation));
    }

    private boolean shouldFail(CloudServiceExtended service) {
        return !service.isOptional();
    }

    private String getFailureMessage(CloudServiceExtended service, ServiceOperation operation) {
        switch (operation.getType()) {
            case CREATE:
                return MessageFormat.format(Messages.ERROR_CREATING_SERVICE, service.getName(), service.getLabel(), service.getPlan(),
                                            operation.getDescription());
            case UPDATE:
                return MessageFormat.format(Messages.ERROR_UPDATING_SERVICE, service.getName(), service.getLabel(), service.getPlan(),
                                            operation.getDescription());
            default:
                throw new IllegalStateException(MessageFormat.format(com.sap.cloud.lm.sl.cf.core.Messages.ILLEGAL_SERVICE_OPERATION_TYPE,
                                                                     operation.getType()));
        }
    }

    private String getWarningMessage(CloudServiceExtended service, ServiceOperation operation) {
        switch (operation.getType()) {
            case CREATE:
                return MessageFormat.format(Messages.ERROR_CREATING_OPTIONAL_SERVICE, service.getName(), service.getLabel(),
                                            service.getPlan(), operation.getDescription());
            case UPDATE:
                return MessageFormat.format(Messages.ERROR_UPDATING_OPTIONAL_SERVICE, service.getName(), service.getLabel(),
                                            service.getPlan(), operation.getDescription());
            default:
                throw new IllegalStateException(MessageFormat.format(com.sap.cloud.lm.sl.cf.core.Messages.ILLEGAL_SERVICE_OPERATION_TYPE,
                                                                     operation.getType()));
        }
    }

    public String getPollingErrorMessage(ExecutionWrapper execution) {
        return Messages.ERROR_MONITORING_CREATION_OR_UPDATE_OF_SERVICES;
    }
}
