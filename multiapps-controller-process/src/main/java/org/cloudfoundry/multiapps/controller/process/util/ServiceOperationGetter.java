package org.cloudfoundry.multiapps.controller.process.util;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.collections4.MapUtils;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudEvent;
import org.cloudfoundry.client.lib.domain.ServiceOperation;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.cf.clients.EventsGetter;
import org.cloudfoundry.multiapps.controller.core.cf.clients.ServiceGetter;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

@Named
public class ServiceOperationGetter {

    private ServiceGetter serviceGetter;
    private EventsGetter eventsGetter;

    @Inject
    public ServiceOperationGetter(ServiceGetter serviceGetter, EventsGetter eventsGetter) {
        this.serviceGetter = serviceGetter;
        this.eventsGetter = eventsGetter;
    }

    public ServiceOperation getLastServiceOperation(ProcessContext context, CloudServiceInstanceExtended service) {
        Map<String, Object> serviceInstanceEntity = getServiceInstanceEntity(context, service);

        if (MapUtils.isEmpty(serviceInstanceEntity)) {
            return getLastDeleteServiceOperation(context, service);
        }

        return getLastServiceOperation(serviceInstanceEntity);
    }

    private Map<String, Object> getServiceInstanceEntity(ProcessContext context, CloudServiceInstanceExtended service) {
        CloudControllerClient client = context.getControllerClient();
        return serviceGetter.getServiceInstanceEntity(client, service.getName(), context.getVariable(Variables.SPACE_GUID));
    }

    private ServiceOperation getLastDeleteServiceOperation(ProcessContext context, CloudServiceInstanceExtended service) {
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
                           .anyMatch(cloudEvent -> eventsGetter.isDeleteEvent(cloudEvent.getType()));
    }

    @SuppressWarnings("unchecked")
    private ServiceOperation getLastServiceOperation(Map<String, Object> serviceInstanceEntity) {
        Map<String, Object> lastOperationAsMap = (Map<String, Object>) serviceInstanceEntity.get("last_operation");
        if (lastOperationAsMap == null) {
            return null;
        }
        ServiceOperation.Type type = ServiceOperation.Type.fromString((String) lastOperationAsMap.get("type"));
        ServiceOperation.State state = ServiceOperation.State.fromString((String) lastOperationAsMap.get("state"));
        return new ServiceOperation(type, (String) lastOperationAsMap.get("description"), state);
    }
}
