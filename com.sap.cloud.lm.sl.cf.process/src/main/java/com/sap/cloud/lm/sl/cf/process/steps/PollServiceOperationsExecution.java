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

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;

import com.sap.cloud.lm.sl.cf.client.ClientExtensions;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.clients.ServiceInstanceGetter;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperation;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperationState;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperationType;
import com.sap.cloud.lm.sl.cf.core.cf.services.TypedServiceOperationState;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ServiceOperationExecutor;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.CommonUtil;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

public class PollServiceOperationsExecution implements AsyncExecution {

    private static final String LAST_SERVICE_OPERATION = "last_operation";
    private static final String SERVICE_NAME = "name";
    private static final String SERVICE_OPERATION_TYPE = "type";
    private static final String SERVICE_OPERATION_STATE = "state";
    private static final String SERVICE_OPERATION_DESCRIPTION = "description";

    private ServiceOperationExecutor serviceOperationExecutor = new ServiceOperationExecutor();
    private ServiceInstanceGetter serviceInstanceGetter;

    public PollServiceOperationsExecution(ServiceInstanceGetter serviceInstanceGetter) {
        this.serviceInstanceGetter = serviceInstanceGetter;
    }

    @Override
    public AsyncExecutionState execute(ExecutionWrapper execution) {
        try {
            execution.getStepLogger()
                .debug(Messages.POLLING_SERVICE_OPERATIONS);

            ClientExtensions clientExtensions = execution.getClientExtensions();
            if (clientExtensions != null) {
                // The asynchronous creation of services is not supported yet on XSA.
                return AsyncExecutionState.FINISHED;
            }

            CloudFoundryOperations client = execution.getCloudFoundryClient();

            Map<String, ServiceOperationType> triggeredServiceOperations = StepsUtil.getTriggeredServiceOperations(execution.getContext());
            List<CloudServiceExtended> servicesToPoll = getServiceOperationsToPoll(execution, triggeredServiceOperations);
            if (servicesToPoll.isEmpty()) {
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

            if (remainingServicesToPoll.size() == 0) {
                return AsyncExecutionState.FINISHED;
            }
            return AsyncExecutionState.RUNNING;
        } catch (CloudFoundryException cfe) {
            CloudControllerException e = new CloudControllerException(cfe);
            execution.getStepLogger()
                .error(e, Messages.ERROR_MONITORING_CREATION_OF_SERVICES);
            throw e;
        } catch (SLException e) {
            execution.getStepLogger()
                .error(e, Messages.ERROR_MONITORING_CREATION_OF_SERVICES);
            throw e;
        }
    }

    private List<CloudServiceExtended> getServiceOperationsToPoll(ExecutionWrapper execution,
        Map<String, ServiceOperationType> triggeredServiceOperations) {
        List<CloudServiceExtended> servicesToPoll = StepsUtil.getServicesToPoll(execution.getContext());
        if (servicesToPoll == null) {
            return computeServicesToPoll(execution.getContext(), triggeredServiceOperations);
        }
        return servicesToPoll;
    }

    private List<CloudServiceExtended> computeServicesToPoll(DelegateExecution context,
        Map<String, ServiceOperationType> triggeredServiceOperations) {
        List<CloudServiceExtended> services = StepsUtil.getServicesToCreate(context);
        // There's no need to poll the creation or update of user-provided services, because it is done synchronously:
        return services.stream()
            .filter(service -> triggeredServiceOperations.containsKey(service.getName()))
            .filter(service -> !service.isUserProvided())
            .collect(Collectors.toList());
    }

    private ServiceOperation getLastServiceOperation(ExecutionWrapper execution, CloudFoundryOperations client,
        CloudServiceExtended service) {
        Map<String, Object> cloudServiceInstance = serviceOperationExecutor.executeServiceOperation(service, () -> {
            return serviceInstanceGetter.getServiceInstance(client, service.getName(), StepsUtil.getSpaceId(execution.getContext()));
        }, execution.getStepLogger());

        validateCloudServiceInstance(execution, service, cloudServiceInstance);
        if (cloudServiceInstance == null) {
            return null;
        }

        return getLastOperation(execution, cloudServiceInstance);
    }

    private void validateCloudServiceInstance(ExecutionWrapper execution, CloudServiceExtended service,
        Map<String, Object> cloudServiceInstance) {
        if (cloudServiceInstance == null && !service.isOptional()) {
            throw new SLException(Messages.CANNOT_RETRIEVE_INSTANCE_OF_SERVICE, service.getName());
        }

        if (cloudServiceInstance == null && service.isOptional()) {
            // Here we're assuming that we cannot retrieve the service instance, because its creation was synchronous and it failed. If that
            // is really the case, then showing a warning progress message to the user is unnecessary, since one should have been shown back
            // in CreateOrUpdateServicesStep.
            execution.getStepLogger()
                .warnWithoutProgressMessage(Messages.CANNOT_RETRIEVE_SERVICE_INSTANCE_OF_OPTIONAL_SERVICE, service.getName());
        }
    }

    @SuppressWarnings("unchecked")
    private ServiceOperation getLastOperation(ExecutionWrapper execution, Map<String, Object> cloudServiceInstance) {
        Map<String, Object> lastOperationAsMap = (Map<String, Object>) cloudServiceInstance.get(LAST_SERVICE_OPERATION);
        ServiceOperation lastOperation = parseServiceOperationFromMap(lastOperationAsMap);
        // TODO Separate create and update steps
        // Be fault tolerant on failure on update of service
        if (lastOperation.getType() == ServiceOperationType.UPDATE && lastOperation.getState() == ServiceOperationState.FAILED) {
            execution.getStepLogger()
                .warn(Messages.FAILED_SERVICE_UPDATE, cloudServiceInstance.get(SERVICE_NAME), lastOperation.getDescription());
            return new ServiceOperation(lastOperation.getType(), lastOperation.getDescription(), ServiceOperationState.SUCCEEDED);
        }
        return lastOperation;
    }

    private ServiceOperation parseServiceOperationFromMap(Map<String, Object> serviceOperation) {
        ServiceOperationType type = ServiceOperationType.fromString((String) serviceOperation.get(SERVICE_OPERATION_TYPE));
        ServiceOperationState state = ServiceOperationState.fromString((String) serviceOperation.get(SERVICE_OPERATION_STATE));
        String description = (String) serviceOperation.get(SERVICE_OPERATION_DESCRIPTION);
        if (description == null && state == ServiceOperationState.FAILED) {
            description = Messages.DEFAULT_FAILED_OPERATION_DESCRIPTION;
        }
        return new ServiceOperation(type, description, state);
    }

    private void reportIndividualServiceState(ExecutionWrapper execution,
        Map<CloudServiceExtended, ServiceOperation> servicesWithLastOperation) {
        for (Entry<CloudServiceExtended, ServiceOperation> serviceWithLastOperation : servicesWithLastOperation.entrySet()) {
            reportIndividualServiceState(execution, serviceWithLastOperation.getKey(), serviceWithLastOperation.getValue());
        }
    }

    private void reportIndividualServiceState(ExecutionWrapper execution, CloudServiceExtended service, ServiceOperation lastOperation) {
        if (lastOperation.getState() == ServiceOperationState.SUCCEEDED) {
            execution.getStepLogger()
                .info(getSuccessMessage(service, lastOperation.getType()));
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
                return MessageFormat.format(Messages.ERROR_CREATING_SERVICE, service.getName(), operation.getDescription());
            case UPDATE:
                return MessageFormat.format(Messages.ERROR_UPDATING_SERVICE, service.getName(), operation.getDescription());
            case DELETE:
                return MessageFormat.format(Messages.ERROR_DELETING_SERVICE, service.getName(), operation.getDescription());
            default:
                throw new IllegalStateException(
                    MessageFormat.format(com.sap.cloud.lm.sl.cf.core.message.Messages.ILLEGAL_SERVICE_OPERATION_TYPE, operation.getType()));
        }
    }

    private String getWarningMessage(CloudServiceExtended service, ServiceOperation operation) {
        switch (operation.getType()) {
            case CREATE:
                return MessageFormat.format(Messages.ERROR_CREATING_OPTIONAL_SERVICE, service.getName(), operation.getDescription());
            case UPDATE:
                return MessageFormat.format(Messages.ERROR_UPDATING_OPTIONAL_SERVICE, service.getName(), operation.getDescription());
            case DELETE:
                return MessageFormat.format(Messages.ERROR_DELETING_OPTIONAL_SERVICE, service.getName(), operation.getDescription());
            default:
                throw new IllegalStateException(
                    MessageFormat.format(com.sap.cloud.lm.sl.cf.core.message.Messages.ILLEGAL_SERVICE_OPERATION_TYPE, operation.getType()));
        }
    }

    private void reportServiceOperationsState(ExecutionWrapper execution,
        Map<CloudServiceExtended, ServiceOperation> servicesWithLastOperation,
        Map<String, ServiceOperationType> triggeredServiceOperations) {
        List<TypedServiceOperationState> nonFinalStates = getNonFinalStates(servicesWithLastOperation.values());
        List<String> nonFinalStateStrings = getStateStrings(nonFinalStates);

        int doneOperations = triggeredServiceOperations.size() - nonFinalStates.size();
        if (nonFinalStateStrings.size() != 0) {
            execution.getStepLogger()
                .info("{0} of {1} done, ({2})", doneOperations, triggeredServiceOperations.size(),
                    CommonUtil.toCommaDelimitedString(nonFinalStateStrings, ""));
        } else {
            execution.getStepLogger()
                .info("{0} of {0} done", triggeredServiceOperations.size());
        }
    };

    private List<TypedServiceOperationState> getNonFinalStates(Collection<ServiceOperation> operations) {
        return operations.stream()
            .map(operation -> TypedServiceOperationState.fromServiceOperation(operation))
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
            .collect(Collectors.groupingBy(state -> state, () -> new TreeMap<>(), Collectors.counting()));
    }

    private List<CloudServiceExtended> getRemainingServicesToPoll(Map<CloudServiceExtended, ServiceOperation> servicesWithLastOperation) {
        return servicesWithLastOperation.entrySet()
            .stream()
            .filter(serviceWithLastOperation -> serviceWithLastOperation.getValue()
                .getState() == ServiceOperationState.IN_PROGRESS)
            .map(serviceWithLastOperation -> serviceWithLastOperation.getKey())
            .collect(Collectors.toList());
    }

}
