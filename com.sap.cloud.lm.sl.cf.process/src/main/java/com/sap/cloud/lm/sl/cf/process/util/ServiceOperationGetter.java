package com.sap.cloud.lm.sl.cf.process.util;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.commons.collections4.MapUtils;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudEvent;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.clients.EventsGetter;
import com.sap.cloud.lm.sl.cf.core.cf.clients.ServiceGetter;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperation;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperationState;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperationType;
import com.sap.cloud.lm.sl.cf.process.steps.ExecutionWrapper;
import com.sap.cloud.lm.sl.cf.process.steps.StepsUtil;

@Component
public class ServiceOperationGetter {

    private ServiceGetter serviceGetter;
    private EventsGetter eventsGetter;

    @Inject
    public ServiceOperationGetter(ServiceGetter serviceGetter, EventsGetter eventsGetter) {
        this.serviceGetter = serviceGetter;
        this.eventsGetter = eventsGetter;
    }

    public ServiceOperation getLastServiceOperation(ExecutionWrapper execution, CloudServiceExtended service) {
        Map<String, Object> serviceInstanceEntity = getServiceInstanceEntity(execution, service);

        if (MapUtils.isEmpty(serviceInstanceEntity)) {
            return getLastDeleteServiceOperation(execution, service);
        }

        return getLastServiceOperation(serviceInstanceEntity);
    }

    private Map<String, Object> getServiceInstanceEntity(ExecutionWrapper execution, CloudServiceExtended service) {
        CloudControllerClient client = execution.getControllerClient();
        return serviceGetter.getServiceInstanceEntity(client, service.getName(), StepsUtil.getSpaceId(execution.getContext()));
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
    private ServiceOperation getLastServiceOperation(Map<String, Object> serviceInstanceEntity) {
        Map<String, Object> lastOperationAsMap = (Map<String, Object>) serviceInstanceEntity.get(ServiceOperation.LAST_SERVICE_OPERATION);
        return lastOperationAsMap != null ? ServiceOperation.fromMap(lastOperationAsMap) : null;
    }
}
