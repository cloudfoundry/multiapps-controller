package org.cloudfoundry.multiapps.controller.process.steps;

import static java.text.MessageFormat.format;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.ServiceOperation;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.security.serialization.SecureSerialization;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.ServiceOperationGetter;
import org.cloudfoundry.multiapps.controller.process.util.ServiceProgressReporter;
import org.cloudfoundry.multiapps.controller.process.util.StepLogger;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

public abstract class PollServiceOperationsExecution implements AsyncExecution {

    private final ServiceOperationGetter serviceOperationGetter;
    private final ServiceProgressReporter serviceProgressReporter;

    protected PollServiceOperationsExecution(ServiceOperationGetter serviceOperationGetter,
                                             ServiceProgressReporter serviceProgressReporter) {
        this.serviceOperationGetter = serviceOperationGetter;
        this.serviceProgressReporter = serviceProgressReporter;
    }

    @Override
    public AsyncExecutionState execute(ProcessContext context) {
        context.getStepLogger()
               .debug(Messages.POLLING_SERVICE_OPERATIONS);

        Map<String, ServiceOperation.Type> triggeredServiceOperations = context.getVariable(Variables.TRIGGERED_SERVICE_OPERATIONS);
        List<CloudServiceInstanceExtended> servicesToPoll = getServiceOperationsToPoll(context, triggeredServiceOperations);
        if (CollectionUtils.isEmpty(servicesToPoll)) {
            return AsyncExecutionState.FINISHED;
        }

        Map<CloudServiceInstanceExtended, ServiceOperation> servicesWithLastOperation = new HashMap<>();
        for (CloudServiceInstanceExtended service : servicesToPoll) {
            ServiceOperation lastServiceOperation = getLastServiceOperationAndHandleExceptions(context, service);
            if (lastServiceOperation != null) {
                servicesWithLastOperation.put(service, lastServiceOperation);
            }
            context.getStepLogger()
                   .debug(Messages.LAST_OPERATION_FOR_SERVICE, service.getName(), JsonUtil.toJson(lastServiceOperation, true));
        }
        reportDetailedServicesStates(context, servicesWithLastOperation);
        reportOverallProgress(context, servicesWithLastOperation.values(), triggeredServiceOperations);
        List<CloudServiceInstanceExtended> remainingServicesToPoll = getRemainingServicesToPoll(servicesWithLastOperation);
        context.getStepLogger()
               .debug(Messages.REMAINING_SERVICES_TO_POLL, SecureSerialization.toJson(remainingServicesToPoll));
        context.setVariable(Variables.SERVICES_TO_POLL, remainingServicesToPoll);

        if (remainingServicesToPoll.isEmpty()) {
            return AsyncExecutionState.FINISHED;
        }
        return AsyncExecutionState.RUNNING;
    }

    protected List<CloudServiceInstanceExtended> getServiceOperationsToPoll(ProcessContext context,
                                                                            Map<String, ServiceOperation.Type> triggeredServiceOperations) {
        List<CloudServiceInstanceExtended> servicesToPoll = context.getVariable(Variables.SERVICES_TO_POLL);
        if (CollectionUtils.isEmpty(servicesToPoll)) {
            return computeServicesToPoll(context, triggeredServiceOperations);
        }
        return servicesToPoll;
    }

    protected List<CloudServiceInstanceExtended>
              getServicesWithTriggeredOperations(Collection<CloudServiceInstanceExtended> services,
                                                 Map<String, ServiceOperation.Type> triggeredServiceOperations) {
        return services.stream()
                       .filter(cloudService -> triggeredServiceOperations.containsKey(cloudService.getName()))
                       .collect(Collectors.toList());
    }

    protected List<CloudServiceInstanceExtended> computeServicesToPoll(ProcessContext context,
                                                                       Map<String, ServiceOperation.Type> triggeredServiceOperations) {
        List<CloudServiceInstanceExtended> servicesData = getServicesData(context);
        return getServicesWithTriggeredOperations(servicesData, triggeredServiceOperations);
    }

    private ServiceOperation getLastServiceOperationAndHandleExceptions(ProcessContext context, CloudServiceInstanceExtended service) {
        try {
            ServiceOperation lastServiceOperation = getLastServiceOperation(context, service);
            if (lastServiceOperation != null) {
                return mapOperationState(context.getStepLogger(), lastServiceOperation, service);
            }
            handleMissingOperationState(context.getStepLogger(), service);
            return null;
        } catch (CloudOperationException e) {
            String errorMessage = format(Messages.ERROR_POLLING_OF_SERVICE, service.getName(), e.getStatusText());
            throw new CloudControllerException(e.getStatusCode(), errorMessage, e.getDescription());
        }
    }

    private ServiceOperation getLastServiceOperation(ProcessContext context, CloudServiceInstanceExtended service) {
        return serviceOperationGetter.getLastServiceOperation(context, service);
    }

    protected ServiceOperation mapOperationState(StepLogger stepLogger, ServiceOperation lastServiceOperation,
                                                 CloudServiceInstanceExtended service) {
        if (lastServiceOperation.getDescription() == null && lastServiceOperation.getState() == ServiceOperation.State.FAILED) {
            return new ServiceOperation(lastServiceOperation.getType(),
                                        Messages.DEFAULT_FAILED_OPERATION_DESCRIPTION,
                                        lastServiceOperation.getState());
        }
        return lastServiceOperation;
    }

    protected void reportDetailedServicesStates(ProcessContext context,
                                                Map<CloudServiceInstanceExtended, ServiceOperation> servicesWithLastOperation) {
        for (Entry<CloudServiceInstanceExtended, ServiceOperation> serviceWithLastOperation : servicesWithLastOperation.entrySet()) {
            reportServiceState(context, serviceWithLastOperation.getKey(), serviceWithLastOperation.getValue());
        }
    }

    private void reportOverallProgress(ProcessContext context, Collection<ServiceOperation> lastServicesOperations,
                                       Map<String, ServiceOperation.Type> triggeredServiceOperations) {
        serviceProgressReporter.reportOverallProgress(context, lastServicesOperations, triggeredServiceOperations);
    }

    protected List<CloudServiceInstanceExtended>
              getRemainingServicesToPoll(Map<CloudServiceInstanceExtended, ServiceOperation> servicesWithLastOperation) {
        return servicesWithLastOperation.entrySet()
                                        .stream()
                                        .filter(serviceWithLastOperation -> serviceWithLastOperation.getValue()
                                                                                                    .getState() == ServiceOperation.State.IN_PROGRESS)
                                        .map(Map.Entry::getKey)
                                        .collect(Collectors.toList());
    }

    protected abstract List<CloudServiceInstanceExtended> getServicesData(ProcessContext context);

    protected abstract void reportServiceState(ProcessContext context, CloudServiceInstanceExtended service,
                                               ServiceOperation lastOperation);

    protected abstract void handleMissingOperationState(StepLogger stepLogger, CloudServiceInstanceExtended service);

}
