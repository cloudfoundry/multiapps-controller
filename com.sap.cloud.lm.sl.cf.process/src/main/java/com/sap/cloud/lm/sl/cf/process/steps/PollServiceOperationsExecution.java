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
import org.flowable.engine.delegate.DelegateExecution;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperation;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperationState;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperationType;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ServiceOperationGetter;
import com.sap.cloud.lm.sl.cf.process.util.ServiceProgressReporter;
import com.sap.cloud.lm.sl.cf.process.util.StepLogger;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

public abstract class PollServiceOperationsExecution implements AsyncExecution {

    private ServiceOperationGetter serviceOperationGetter;
    private ServiceProgressReporter serviceProgressReporter;

    public PollServiceOperationsExecution(ServiceOperationGetter serviceOperationGetter, ServiceProgressReporter serviceProgressReporter) {
        this.serviceOperationGetter = serviceOperationGetter;
        this.serviceProgressReporter = serviceProgressReporter;
    }

    @Override
    public AsyncExecutionState execute(ExecutionWrapper execution) {
        execution.getStepLogger()
                 .debug(Messages.POLLING_SERVICE_OPERATIONS);

        Map<String, ServiceOperationType> triggeredServiceOperations = StepsUtil.getTriggeredServiceOperations(execution.getContext());
        List<CloudServiceExtended> servicesToPoll = getServiceOperationsToPoll(execution, triggeredServiceOperations);
        if (CollectionUtils.isEmpty(servicesToPoll)) {
            return AsyncExecutionState.FINISHED;
        }

        Map<CloudServiceExtended, ServiceOperation> servicesWithLastOperation = new HashMap<>();
        for (CloudServiceExtended service : servicesToPoll) {
            ServiceOperation lastServiceOperation = getLastServiceOperationAndHandleExceptions(execution, service);
            if (lastServiceOperation != null) {
                servicesWithLastOperation.put(service, lastServiceOperation);
            }
            execution.getStepLogger()
                     .debug(Messages.LAST_OPERATION_FOR_SERVICE, service.getName(), JsonUtil.toJson(lastServiceOperation, true));
        }
        reportDetailedServicesStates(execution, servicesWithLastOperation);
        reportOverallProgress(execution, new ArrayList<>(servicesWithLastOperation.values()), triggeredServiceOperations);
        List<CloudServiceExtended> remainingServicesToPoll = getRemainingServicesToPoll(servicesWithLastOperation);
        execution.getStepLogger()
                 .debug(Messages.REMAINING_SERVICES_TO_POLL, JsonUtil.toJson(remainingServicesToPoll, true));
        StepsUtil.setServicesToPoll(execution.getContext(), remainingServicesToPoll);

        if (remainingServicesToPoll.isEmpty()) {
            return AsyncExecutionState.FINISHED;
        }
        return AsyncExecutionState.RUNNING;
    }

    protected List<CloudServiceExtended> getServiceOperationsToPoll(ExecutionWrapper execution,
                                                                    Map<String, ServiceOperationType> triggeredServiceOperations) {
        List<CloudServiceExtended> servicesToPoll = StepsUtil.getServicesToPoll(execution.getContext());
        if (CollectionUtils.isEmpty(servicesToPoll)) {
            return computeServicesToPoll(execution, triggeredServiceOperations);
        }
        return servicesToPoll;
    }

    protected List<CloudServiceExtended> getServicesWithTriggeredOperations(Collection<CloudServiceExtended> services,
                                                                            Map<String, ServiceOperationType> triggeredServiceOperations) {
        return services.stream()
                       .filter(e -> triggeredServiceOperations.containsKey(e.getName()))
                       .collect(Collectors.toList());
    }

    protected List<CloudServiceExtended> computeServicesToPoll(ExecutionWrapper execution,
                                                               Map<String, ServiceOperationType> triggeredServiceOperations) {
        List<CloudServiceExtended> servicesData = getServicesData(execution.getContext());
        return getServicesWithTriggeredOperations(servicesData, triggeredServiceOperations);
    }

    private ServiceOperation getLastServiceOperationAndHandleExceptions(ExecutionWrapper execution, CloudServiceExtended service) {
        try {
            ServiceOperation lastServiceOperation = getLastServiceOperation(execution, service);
            if (lastServiceOperation != null) {
                return mapOperationState(execution.getStepLogger(), lastServiceOperation, service);
            }
            handleMissingOperationState(execution.getStepLogger(), service);
            return null;
        } catch (CloudOperationException e) {
            String errorMessage = format(Messages.ERROR_POLLING_OF_SERVICE, service.getName(), e.getStatusText());
            CloudControllerException exception = new CloudControllerException(e.getStatusCode(), errorMessage, e.getDescription());
            throw exception;
        }
    }

    private ServiceOperation getLastServiceOperation(ExecutionWrapper execution, CloudServiceExtended service) {
        return serviceOperationGetter.getLastServiceOperation(execution, service);
    }

    protected ServiceOperation mapOperationState(StepLogger stepLogger, ServiceOperation lastServiceOperation,
                                                 CloudServiceExtended service) {
        if (lastServiceOperation.getDescription() == null && lastServiceOperation.getState() == ServiceOperationState.FAILED) {
            return new ServiceOperation(lastServiceOperation.getType(),
                                        Messages.DEFAULT_FAILED_OPERATION_DESCRIPTION,
                                        lastServiceOperation.getState());
        }
        return lastServiceOperation;
    }

    protected void reportDetailedServicesStates(ExecutionWrapper execution,
                                                Map<CloudServiceExtended, ServiceOperation> servicesWithLastOperation) {
        for (Entry<CloudServiceExtended, ServiceOperation> serviceWithLastOperation : servicesWithLastOperation.entrySet()) {
            reportServiceState(execution, serviceWithLastOperation.getKey(), serviceWithLastOperation.getValue());
        }
    }

    private void reportOverallProgress(ExecutionWrapper execution, List<ServiceOperation> lastServicesOperations,
                                       Map<String, ServiceOperationType> triggeredServiceOperations) {
        serviceProgressReporter.reportOverallProgress(execution, lastServicesOperations, triggeredServiceOperations);
    }

    protected List<CloudServiceExtended> getRemainingServicesToPoll(Map<CloudServiceExtended, ServiceOperation> servicesWithLastOperation) {
        return servicesWithLastOperation.entrySet()
                                        .stream()
                                        .filter(serviceWithLastOperation -> serviceWithLastOperation.getValue()
                                                                                                    .getState() == ServiceOperationState.IN_PROGRESS)
                                        .map(Map.Entry::getKey)
                                        .collect(Collectors.toList());
    }

    protected abstract List<CloudServiceExtended> getServicesData(DelegateExecution context);

    protected abstract void reportServiceState(ExecutionWrapper execution, CloudServiceExtended service, ServiceOperation lastOperation);

    protected abstract void handleMissingOperationState(StepLogger stepLogger, CloudServiceExtended service);

}
