package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;
import java.util.List;

import org.cloudfoundry.multiapps.common.SLException;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceInstanceExtended;
import com.sap.cloud.lm.sl.cf.core.model.ServiceOperation;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ServiceOperationGetter;
import com.sap.cloud.lm.sl.cf.process.util.ServiceProgressReporter;
import com.sap.cloud.lm.sl.cf.process.util.StepLogger;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;

public class PollServiceDeleteOperationsExecution extends PollServiceOperationsExecution {

    public PollServiceDeleteOperationsExecution(ServiceOperationGetter serviceOperationGetter,
                                                ServiceProgressReporter serviceProgressReporter) {
        super(serviceOperationGetter, serviceProgressReporter);
    }

    @Override
    protected List<CloudServiceInstanceExtended> getServicesData(ProcessContext context) {
        return context.getVariable(Variables.SERVICES_DATA);
    }

    @Override
    protected void handleMissingOperationState(StepLogger stepLogger, CloudServiceInstanceExtended service) {
        throw new SLException(Messages.CANNOT_RETRIEVE_INSTANCE_OF_SERVICE, service.getName());
    }

    @Override
    protected void reportServiceState(ProcessContext context, CloudServiceInstanceExtended service, ServiceOperation lastServiceOperation) {
        if (lastServiceOperation.getState() == ServiceOperation.State.SUCCEEDED) {
            context.getStepLogger()
                   .debug(getSuccessMessage(service));
            return;
        }

        if (lastServiceOperation.getState() == ServiceOperation.State.FAILED) {
            throw new SLException(getFailureMessage(service, lastServiceOperation));
        }
    }

    private String getSuccessMessage(CloudServiceInstanceExtended service) {
        return MessageFormat.format(Messages.SERVICE_DELETED, service.getName());
    }

    private String getFailureMessage(CloudServiceInstanceExtended service, ServiceOperation lastServiceOperation) {
        return MessageFormat.format(Messages.ERROR_DELETING_SERVICE, service.getName(), service.getLabel(), service.getPlan(),
                                    lastServiceOperation.getDescription());
    }

    @Override
    public String getPollingErrorMessage(ProcessContext context) {
        return Messages.ERROR_MONITORING_DELETION_OF_SERVICES;
    }

}
