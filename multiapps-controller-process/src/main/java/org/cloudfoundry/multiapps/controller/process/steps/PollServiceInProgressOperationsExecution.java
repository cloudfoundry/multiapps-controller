package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.List;

import org.cloudfoundry.client.lib.domain.ServiceOperation;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.ServiceOperationGetter;
import org.cloudfoundry.multiapps.controller.process.util.ServiceProgressReporter;
import org.cloudfoundry.multiapps.controller.process.util.StepLogger;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

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
        if (lastServiceOperation.getState() == ServiceOperation.State.FAILED) {
            throw new SLException(getFailureMessage(service, lastServiceOperation));
        }

        if (lastServiceOperation.getState() == ServiceOperation.State.SUCCEEDED) {
            context.getStepLogger()
                   .debug(getSuccessMessage(service, lastServiceOperation.getType()));
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
                throw new IllegalStateException(MessageFormat.format(org.cloudfoundry.multiapps.controller.core.Messages.ILLEGAL_SERVICE_OPERATION_TYPE,
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
                throw new IllegalStateException(MessageFormat.format(org.cloudfoundry.multiapps.controller.core.Messages.ILLEGAL_SERVICE_OPERATION_TYPE,
                                                                     lastServiceOperation.getType()));
        }
    }

    @Override
    public String getPollingErrorMessage(ProcessContext context) {
        return Messages.ERROR_MONITORING_OPERATIONS_OVER_SERVICES;
    }

}
