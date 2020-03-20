package com.sap.cloud.lm.sl.cf.process.util;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.collections4.MapUtils;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudEvent;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.clients.EventsGetter;
import com.sap.cloud.lm.sl.cf.core.cf.clients.ServiceGetter;
import com.sap.cloud.lm.sl.cf.core.model.ServiceOperation;
import com.sap.cloud.lm.sl.cf.process.steps.ProcessContext;
import com.sap.cloud.lm.sl.cf.process.steps.StepsUtil;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;

@Named
public class ServiceOperationGetter {

    private ServiceGetter serviceGetter;
    private EventsGetter eventsGetter;

    @Inject
    public ServiceOperationGetter(ServiceGetter serviceGetter, EventsGetter eventsGetter) {
        this.serviceGetter = serviceGetter;
        this.eventsGetter = eventsGetter;
    }

    public ServiceOperation getLastServiceOperation(ProcessContext context, CloudServiceExtended service) {
        Map<String, Object> serviceInstanceEntity = getServiceInstanceEntity(context, service);

        if (MapUtils.isEmpty(serviceInstanceEntity)) {
            return getLastDeleteServiceOperation(context, service);
        }

        return getLastServiceOperation(serviceInstanceEntity);
    }

    private Map<String, Object> getServiceInstanceEntity(ProcessContext context, CloudServiceExtended service) {
        CloudControllerClient client = context.getControllerClient();
        return serviceGetter.getServiceInstanceEntity(client, service.getName(), context.getVariable(Variables.SPACE_ID));
    }

    private ServiceOperation getLastDeleteServiceOperation(ProcessContext context, CloudServiceExtended service) {
        if (service.getMetadata() == null) {
            return null;
        }
        boolean isServiceDeleted = isServiceDeleted(context, service.getMetadata()
                                                                    .getGuid());
        ServiceOperation.State operationState = isServiceDeleted ? ServiceOperation.State.SUCCEEDED : ServiceOperation.State.IN_PROGRESS;
        return new ServiceOperation(ServiceOperation.Type.DELETE, ServiceOperation.Type.DELETE.name(), operationState);
    }

    private boolean isServiceDeleted(ProcessContext context, UUID uuid) {
        List<CloudEvent> serviceEvent = eventsGetter.getEvents(uuid, context.getControllerClient());
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
