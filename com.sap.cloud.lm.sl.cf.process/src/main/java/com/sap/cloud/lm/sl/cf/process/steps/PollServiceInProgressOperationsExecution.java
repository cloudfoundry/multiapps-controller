package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;
import java.util.List;

import org.flowable.engine.delegate.DelegateExecution;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperation;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ServiceOperationGetter;
import com.sap.cloud.lm.sl.cf.process.util.ServiceProgressReporter;
import com.sap.cloud.lm.sl.cf.process.util.StepLogger;
import com.sap.cloud.lm.sl.common.SLException;

public class PollServiceInProgressOperationsExecution extends PollServiceOperationsExecution {

    public PollServiceInProgressOperationsExecution(ServiceOperationGetter serviceOperationGetter,
                                                    ServiceProgressReporter serviceProgressReporter) {
        super(serviceOperationGetter, serviceProgressReporter);
    }

    @Override
    protected List<CloudServiceExtended> getServicesData(DelegateExecution context) {
        return StepsUtil.getServicesData(context);
    }

    @Override
    protected void reportServiceState(ExecutionWrapper execution, CloudServiceExtended service, ServiceOperation lastServiceOperation) {
        if (lastServiceOperation.getState() == ServiceOperation.State.SUCCEEDED) {
            execution.getStepLogger()
                     .debug(getSuccessMessage(service, lastServiceOperation.getType()));
            return;
        }

        if (lastServiceOperation.getState() == ServiceOperation.State.FAILED) {
            throw new SLException(getFailureMessage(service, lastServiceOperation));
        }
    }

    @Override
    protected void handleMissingOperationState(StepLogger stepLogger, CloudServiceExtended service) {
        stepLogger.warnWithoutProgressMessage(Messages.MISSING_SERVICE_OPERATION_STATE, service.getName());
    }

    private String getSuccessMessage(CloudServiceExtended service, ServiceOperation.Type type) {
        switch (type) {
            case CREATE:
                return MessageFormat.format(Messages.SERVICE_CREATED, service.getName());
            case UPDATE:
                return MessageFormat.format(Messages.SERVICE_UPDATED, service.getName());
            case DELETE:
                return MessageFormat.format(Messages.SERVICE_DELETED, service.getName());
            default:
                throw new IllegalStateException(MessageFormat.format(com.sap.cloud.lm.sl.cf.core.message.Messages.ILLEGAL_SERVICE_OPERATION_TYPE,
                                                                     type));
        }
    }

    private String getFailureMessage(CloudServiceExtended service, ServiceOperation lastServiceOperation) {
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
                throw new IllegalStateException(MessageFormat.format(com.sap.cloud.lm.sl.cf.core.message.Messages.ILLEGAL_SERVICE_OPERATION_TYPE,
                                                                     lastServiceOperation.getType()));
        }
    }

    @Override
    public String getPollingErrorMessage(ExecutionWrapper execution) {
        return Messages.ERROR_MONITORING_OPERATIONS_OVER_SERVICES;
    }

}
