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

public class PollServiceDeleteOperationsExecution extends PollServiceOperationsExecution {

    public PollServiceDeleteOperationsExecution(ServiceOperationGetter serviceOperationGetter,
                                                ServiceProgressReporter serviceProgressReporter) {
        super(serviceOperationGetter, serviceProgressReporter);
    }

    @Override
    protected List<CloudServiceExtended> getServicesData(DelegateExecution context) {
        return StepsUtil.getServicesData(context);
    }

    @Override
    protected void handleMissingOperationState(StepLogger stepLogger, CloudServiceExtended service) {
        throw new SLException(Messages.CANNOT_RETRIEVE_INSTANCE_OF_SERVICE, service.getName());
    }

    @Override
    protected void reportServiceState(ExecutionWrapper execution, CloudServiceExtended service, ServiceOperation lastServiceOperation) {
        if (lastServiceOperation.getState() == ServiceOperation.State.SUCCEEDED) {
            execution.getStepLogger()
                     .debug(getSuccessMessage(service));
            return;
        }

        if (lastServiceOperation.getState() == ServiceOperation.State.FAILED) {
            throw new SLException(getFailureMessage(service, lastServiceOperation));
        }
    }

    private String getSuccessMessage(CloudServiceExtended service) {
        return MessageFormat.format(Messages.SERVICE_DELETED, service.getName());
    }

    private String getFailureMessage(CloudServiceExtended service, ServiceOperation lastServiceOperation) {
        return MessageFormat.format(Messages.ERROR_DELETING_SERVICE, service.getName(), service.getLabel(), service.getPlan(),
                                    lastServiceOperation.getDescription());
    }

    @Override
    public String getPollingErrorMessage(ExecutionWrapper execution) {
        return Messages.ERROR_MONITORING_DELETION_OF_SERVICES;
    }

}
