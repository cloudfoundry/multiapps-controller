package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.CloudService;

import com.sap.cloud.lm.sl.cf.client.XsCloudControllerClient;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperation;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperationState;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperationType;
import com.sap.cloud.lm.sl.cf.core.cf.services.TypedServiceOperationState;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

public abstract class PollServiceOperationsExecution implements AsyncExecution {

    @Override
    public AsyncExecutionState execute(ExecutionWrapper execution) {
        try {
            execution.getStepLogger()
                .debug(Messages.POLLING_SERVICE_OPERATIONS);

            XsCloudControllerClient xsClient = execution.getXsControllerClient();
            if (xsClient != null) {
                // The asynchronous creation of services is not supported yet on XSA.
                return AsyncExecutionState.FINISHED;
            }

            CloudControllerClient client = execution.getControllerClient();

            Map<String, ServiceOperationType> triggeredServiceOperations = StepsUtil.getTriggeredServiceOperations(execution.getContext());
            List<CloudServiceExtended> servicesToPoll = getServiceOperationsToPoll(execution, triggeredServiceOperations);
            if (CollectionUtils.isEmpty(servicesToPoll)) {
                return AsyncExecutionState.FINISHED;
            }

            Map<CloudServiceExtended, ServiceOperation> servicesWithLastOperation = new HashMap<>();
            for (CloudServiceExtended service : servicesToPoll) {
                ServiceOperation lastServiceOperation = getLastServiceOperation(execution, client, service);
                if (lastServiceOperation != null) {
                    servicesWithLastOperation.put(service, lastServiceOperation);
                }
                execution.getStepLogger()
                    .debug(Messages.LAST_OPERATION_FOR_SERVICE, service.getName(), JsonUtil.toJson(lastServiceOperation, true));
            }
            reportIndividualServiceState(execution, servicesWithLastOperation);
            reportServiceOperationsState(execution, servicesWithLastOperation, triggeredServiceOperations);
            List<CloudServiceExtended> remainingServicesToPoll = getRemainingServicesToPoll(servicesWithLastOperation);
            execution.getStepLogger()
                .debug(Messages.REMAINING_SERVICES_TO_POLL, JsonUtil.toJson(remainingServicesToPoll, true));
            StepsUtil.setServicesToPoll(execution.getContext(), remainingServicesToPoll);

            if (remainingServicesToPoll.isEmpty()) {
                return AsyncExecutionState.FINISHED;
            }
            return AsyncExecutionState.RUNNING;
        } catch (CloudOperationException coe) {
            CloudControllerException e = new CloudControllerException(coe);
            execution.getStepLogger()
                .error(e, Messages.ERROR_MONITORING_CREATION_OF_SERVICES);
            throw e;
        } catch (SLException e) {
            execution.getStepLogger()
                .error(e, Messages.ERROR_MONITORING_CREATION_OF_SERVICES);
            throw e;
        }
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

    protected abstract List<CloudServiceExtended> computeServicesToPoll(ExecutionWrapper execution,
        Map<String, ServiceOperationType> triggeredServiceOperations);

    protected abstract ServiceOperation getLastServiceOperation(ExecutionWrapper execution, CloudControllerClient client,
        CloudServiceExtended service);
    protected void reportIndividualServiceState(ExecutionWrapper execution,
        Map<CloudServiceExtended, ServiceOperation> servicesWithLastOperation) {
        for (Entry<CloudServiceExtended, ServiceOperation> serviceWithLastOperation : servicesWithLastOperation.entrySet()) {
            reportIndividualServiceState(execution, serviceWithLastOperation.getKey(), serviceWithLastOperation.getValue());
        }
    }

    protected void reportIndividualServiceState(ExecutionWrapper execution, CloudServiceExtended service, ServiceOperation lastOperation) {
        if (lastOperation.getState() == ServiceOperationState.SUCCEEDED) {
            execution.getStepLogger()
                .debug(getSuccessMessage(service, lastOperation.getType()));
            return;
        }

        if (lastOperation.getState() == ServiceOperationState.FAILED) {
            if (!service.isOptional()) {
                throw new SLException(getFailureMessage(service, lastOperation));
            }
            execution.getStepLogger()
                .warn(getWarningMessage(service, lastOperation));
        }
    }


    protected void reportServiceOperationsState(ExecutionWrapper execution,
        Map<CloudServiceExtended, ServiceOperation> servicesWithLastOperation,
        Map<String, ServiceOperationType> triggeredServiceOperations) {
        List<TypedServiceOperationState> nonFinalStates = getNonFinalStates(servicesWithLastOperation.values());
        List<String> nonFinalStateStrings = getStateStrings(nonFinalStates);

        int doneOperations = triggeredServiceOperations.size() - nonFinalStates.size();
        if (!nonFinalStateStrings.isEmpty()) {
            execution.getStepLogger()
                .info("{0} of {1} done, ({2})", doneOperations, triggeredServiceOperations.size(), String.join(",", nonFinalStateStrings));
        } else {
            execution.getStepLogger()
                .info("{0} of {0} done", triggeredServiceOperations.size());
        }
    }

    protected List<CloudServiceExtended> getRemainingServicesToPoll(Map<CloudServiceExtended, ServiceOperation> servicesWithLastOperation) {
        return servicesWithLastOperation.entrySet()
            .stream()
            .filter(serviceWithLastOperation -> serviceWithLastOperation.getValue()
                .getState() == ServiceOperationState.IN_PROGRESS)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
    
    protected void handleMissingServiceInstance(ExecutionWrapper execution, CloudServiceExtended service) {
        if (!service.isOptional()) {
            throw new SLException(Messages.CANNOT_RETRIEVE_INSTANCE_OF_SERVICE, service.getName());
        }
        // Here we're assuming that we cannot retrieve the service instance, because its creation was synchronous and it failed. If that
        // is really the case, then showing a warning progress message to the user is unnecessary, since one should have been shown back
        // in CreateOrUpdateServicesStep.
        execution.getStepLogger()
        .warnWithoutProgressMessage(Messages.CANNOT_RETRIEVE_SERVICE_INSTANCE_OF_OPTIONAL_SERVICE, service.getName());
    }

    private String getSuccessMessage(CloudServiceExtended service, ServiceOperationType type) {
        switch (type) {
            case CREATE:
                return MessageFormat.format(Messages.SERVICE_CREATED, service.getName());
            case UPDATE:
                return MessageFormat.format(Messages.SERVICE_UPDATED, service.getName());
            case DELETE:
                return MessageFormat.format(Messages.SERVICE_DELETED, service.getName());
            default:
                throw new IllegalStateException(
                    MessageFormat.format(com.sap.cloud.lm.sl.cf.core.message.Messages.ILLEGAL_SERVICE_OPERATION_TYPE, type));
        }
    }
    
    private String getFailureMessage(CloudServiceExtended service, ServiceOperation operation) {
        switch (operation.getType()) {
            case CREATE:
                return MessageFormat.format(Messages.ERROR_CREATING_SERVICE, service.getName(), service.getLabel(), service.getPlan(),
                    operation.getDescription());
            case UPDATE:
                return MessageFormat.format(Messages.ERROR_UPDATING_SERVICE, service.getName(), service.getLabel(), service.getPlan(),
                    operation.getDescription());
            case DELETE:
                return MessageFormat.format(Messages.ERROR_DELETING_SERVICE, service.getName(), service.getLabel(), service.getPlan(),
                    operation.getDescription());
            default:
                throw new IllegalStateException(
                    MessageFormat.format(com.sap.cloud.lm.sl.cf.core.message.Messages.ILLEGAL_SERVICE_OPERATION_TYPE, operation.getType()));
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
            case DELETE:
                return MessageFormat.format(Messages.ERROR_DELETING_OPTIONAL_SERVICE, service.getName(), service.getLabel(),
                    service.getPlan(), operation.getDescription());
            default:
                throw new IllegalStateException(
                    MessageFormat.format(com.sap.cloud.lm.sl.cf.core.message.Messages.ILLEGAL_SERVICE_OPERATION_TYPE, operation.getType()));
        }
    }
    
    private List<TypedServiceOperationState> getNonFinalStates(Collection<ServiceOperation> operations) {
        return operations.stream()
            .map(TypedServiceOperationState::fromServiceOperation)
            .filter(state -> state != TypedServiceOperationState.DONE)
            .collect(Collectors.toList());
    }

    private List<String> getStateStrings(Collection<TypedServiceOperationState> states) {
        Map<TypedServiceOperationState, Long> stateCounts = getStateCounts(states);
        List<String> stateStrings = new ArrayList<>();
        for (Map.Entry<TypedServiceOperationState, Long> stateCount : stateCounts.entrySet()) {
            stateStrings.add(format("{0} {1}", stateCount.getValue(), stateCount.getKey()
                .toString()
                .toLowerCase()));
        }
        return stateStrings;
    }

    private TreeMap<TypedServiceOperationState, Long> getStateCounts(Collection<TypedServiceOperationState> serviceOperationStates) {
        return serviceOperationStates.stream()
            .collect(Collectors.groupingBy(state -> state, TreeMap::new, Collectors.counting()));
    }

}
