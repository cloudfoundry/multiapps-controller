package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.List;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.model.ServiceOperation;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.ServiceOperationGetter;
import org.cloudfoundry.multiapps.controller.process.util.ServiceProgressReporter;
import org.cloudfoundry.multiapps.controller.process.util.StepLogger;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

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
