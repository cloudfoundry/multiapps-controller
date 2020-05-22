package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudOperationException;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceInstanceExtended;
import com.sap.cloud.lm.sl.cf.core.model.ServiceOperation;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerialization;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ServiceOperationGetter;
import com.sap.cloud.lm.sl.cf.process.util.ServiceProgressReporter;
import com.sap.cloud.lm.sl.cf.process.util.StepLogger;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

public abstract class PollServiceOperationsExecution implements AsyncExecution {

    private final ServiceOperationGetter serviceOperationGetter;
    private final ServiceProgressReporter serviceProgressReporter;

    public PollServiceOperationsExecution(ServiceOperationGetter serviceOperationGetter, ServiceProgressReporter serviceProgressReporter) {
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
        reportOverallProgress(context, new ArrayList<>(servicesWithLastOperation.values()), triggeredServiceOperations);
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

    protected List<CloudServiceInstanceExtended> getServicesWithTriggeredOperations(Collection<CloudServiceInstanceExtended> services,
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

    private void reportOverallProgress(ProcessContext context, List<ServiceOperation> lastServicesOperations,
                                       Map<String, ServiceOperation.Type> triggeredServiceOperations) {
        serviceProgressReporter.reportOverallProgress(context, lastServicesOperations, triggeredServiceOperations);
    }

    protected List<CloudServiceInstanceExtended> getRemainingServicesToPoll(Map<CloudServiceInstanceExtended, ServiceOperation> servicesWithLastOperation) {
        return servicesWithLastOperation.entrySet()
                                        .stream()
                                        .filter(serviceWithLastOperation -> serviceWithLastOperation.getValue()
                                                                                                    .getState() == ServiceOperation.State.IN_PROGRESS)
                                        .map(Map.Entry::getKey)
                                        .collect(Collectors.toList());
    }

    protected abstract List<CloudServiceInstanceExtended> getServicesData(ProcessContext context);

    protected abstract void reportServiceState(ProcessContext context, CloudServiceInstanceExtended service, ServiceOperation lastOperation);

    protected abstract void handleMissingOperationState(StepLogger stepLogger, CloudServiceInstanceExtended service);

}
