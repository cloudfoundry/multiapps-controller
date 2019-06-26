package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudEvent;
import org.flowable.engine.delegate.DelegateExecution;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.clients.EventsGetter;
import com.sap.cloud.lm.sl.cf.core.cf.clients.ServiceGetter;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperation;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperationState;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperationType;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ServiceOperationExecutor;

public class PollServiceInProgressOperationsExecution extends PollServiceOperationsExecution implements AsyncExecution {

    private ServiceOperationExecutor serviceOperationExecutor;
    private ServiceGetter serviceGetter;
    private EventsGetter eventsGetter;

    public PollServiceInProgressOperationsExecution(ServiceGetter serviceGetter, EventsGetter eventsGetter) {
        this.serviceGetter = serviceGetter;
        this.eventsGetter = eventsGetter;
        serviceOperationExecutor = new ServiceOperationExecutor();
    }

    @Override
    protected List<CloudServiceExtended> computeServicesToPoll(ExecutionWrapper execution,
                                                               Map<String, ServiceOperationType> triggeredServiceOperations) {
        List<CloudServiceExtended> servicesData = getServicesData(execution.getContext());
        return getServicesWithTriggeredOperations(servicesData, triggeredServiceOperations);
    }

    protected List<CloudServiceExtended> getServicesData(DelegateExecution context) {
        return StepsUtil.getServicesData(context);
    }

    @Override
    protected ServiceOperation getLastServiceOperation(ExecutionWrapper execution, CloudControllerClient client,
                                                       CloudServiceExtended service) {
        Map<String, Object> serviceInstanceEntity = getServiceInstanceEntity(execution, client, service);

        if (serviceInstanceEntity == null || serviceInstanceEntity.isEmpty()) {
            ServiceOperation lastOperation = getLastDeleteServiceOperation(execution, service);
            if (lastOperation != null) {
                return lastOperation;
            }
            handleMissingServiceInstance(execution, service);
            return null;
        }
        return getLastOperation(serviceInstanceEntity);
    }

    protected Map<String, Object> getServiceInstanceEntity(ExecutionWrapper execution, CloudControllerClient client,
                                                           CloudServiceExtended service) {
        return serviceOperationExecutor.executeServiceOperation(service,
                                                                () -> serviceGetter.getServiceInstanceEntity(client, service.getName(),
                                                                                                             StepsUtil.getSpaceId(execution.getContext())),
                                                                execution.getStepLogger());
    }

    private ServiceOperation getLastDeleteServiceOperation(ExecutionWrapper execution, CloudServiceExtended service) {
        if (service.getMetadata() == null) {
            return null;
        }
        boolean isServiceDeleted = isServiceDeleted(execution, service.getMetadata()
                                                                      .getGuid());
        ServiceOperationState operationState = isServiceDeleted ? ServiceOperationState.SUCCEEDED : ServiceOperationState.IN_PROGRESS;
        return new ServiceOperation(ServiceOperationType.DELETE, ServiceOperationType.DELETE.name(), operationState);
    }

    private boolean isServiceDeleted(ExecutionWrapper execution, UUID uuid) {
        List<CloudEvent> serviceEvent = eventsGetter.getEvents(uuid, execution.getControllerClient());
        return serviceEvent.stream()
                           .filter(Objects::nonNull)
                           .anyMatch(e -> eventsGetter.isDeleteEvent(e.getType()));
    }

    @SuppressWarnings("unchecked")
    protected ServiceOperation getLastOperation(Map<String, Object> serviceInstanceEntity) {
        Map<String, Object> lastOperationAsMap = (Map<String, Object>) serviceInstanceEntity.get(ServiceOperation.LAST_SERVICE_OPERATION);
        ServiceOperation lastOperation = ServiceOperation.fromMap(lastOperationAsMap);
        return handleFailedOperationWithoutDescription(lastOperation);
    }

    private ServiceOperation handleFailedOperationWithoutDescription(ServiceOperation lastOperation) {
        if (lastOperation.getDescription() == null && lastOperation.getState() == ServiceOperationState.FAILED) {
            return new ServiceOperation(lastOperation.getType(), Messages.DEFAULT_FAILED_OPERATION_DESCRIPTION, lastOperation.getState());
        }
        return lastOperation;
    }

    @Override
    public String getPollingErrorMessage(ExecutionWrapper execution) {
        return Messages.ERROR_MONITORING_OPERATIONS_OVER_SERVICES;
    }
}
