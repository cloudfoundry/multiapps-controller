package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;
import java.util.List;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceInstanceExtended;
import com.sap.cloud.lm.sl.cf.core.model.ServiceOperation;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ServiceOperationGetter;
import com.sap.cloud.lm.sl.cf.process.util.ServiceProgressReporter;
import com.sap.cloud.lm.sl.cf.process.util.StepLogger;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.common.SLException;

public class PollServiceInProgressOperationsExecution extends PollServiceOperationsExecution {

    public PollServiceInProgressOperationsExecution(ServiceOperationGetter serviceOperationGetter,
                                                    ServiceProgressReporter serviceProgressReporter) {
        super(serviceOperationGetter, serviceProgressReporter);
    }

    @Override
    protected List<CloudServiceInstanceExtended> getServicesData(ProcessContext context) {
        return context.getVariable(Variables.SERVICES_DATA);
    }

    @Override
    protected void reportServiceState(ProcessContext context, CloudServiceInstanceExtended service, ServiceOperation lastServiceOperation) {
        if (lastServiceOperation.getState() == ServiceOperation.State.SUCCEEDED) {
            context.getStepLogger()
                   .debug(getSuccessMessage(service, lastServiceOperation.getType()));
            return;
        }

        if (lastServiceOperation.getState() == ServiceOperation.State.FAILED) {
            throw new SLException(getFailureMessage(service, lastServiceOperation));
        }
    }

    @Override
    protected void handleMissingOperationState(StepLogger stepLogger, CloudServiceInstanceExtended service) {
        stepLogger.warnWithoutProgressMessage(Messages.MISSING_SERVICE_OPERATION_STATE, service.getName());
    }

    private String getSuccessMessage(CloudServiceInstanceExtended service, ServiceOperation.Type type) {
        switch (type) {
            case CREATE:
                return MessageFormat.format(Messages.SERVICE_CREATED, service.getName());
            case UPDATE:
                return MessageFormat.format(Messages.SERVICE_UPDATED, service.getName());
            case DELETE:
                return MessageFormat.format(Messages.SERVICE_DELETED, service.getName());
            default:
                throw new IllegalStateException(MessageFormat.format(com.sap.cloud.lm.sl.cf.core.Messages.ILLEGAL_SERVICE_OPERATION_TYPE,
                                                                     type));
        }
    }

    private String getFailureMessage(CloudServiceInstanceExtended service, ServiceOperation lastServiceOperation) {
        switch (lastServiceOperation.getType()) {
            case CREATE:
                return MessageFormat.format(Messages.ERROR_CREATING_SERVICE, service.getName(), service.getLabel(), service.getPlan(),
                                            lastServiceOperation.getDescription());
            case UPDATE:
                return MessageFormat.format(Messages.ERROR_UPDATING_SERVICE, service.getName(), service.getLabel(), service.getPlan(),
                                            lastServiceOperation.getDescription());
            case DELETE:
                return MessageFormat.format(Messages.ERROR_DELETING_SERVICE, service.getName(), service.getLabel(), service.getPlan(),
                                            lastServiceOperation.getDescription());
            default:
                throw new IllegalStateException(MessageFormat.format(com.sap.cloud.lm.sl.cf.core.Messages.ILLEGAL_SERVICE_OPERATION_TYPE,
                                                                     lastServiceOperation.getType()));
        }
    }

    @Override
    public String getPollingErrorMessage(ProcessContext context) {
        return Messages.ERROR_MONITORING_OPERATIONS_OVER_SERVICES;
    }

}
