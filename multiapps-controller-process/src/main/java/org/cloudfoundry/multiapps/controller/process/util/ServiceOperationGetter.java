package org.cloudfoundry.multiapps.controller.process.util;

import java.util.List;
import java.util.UUID;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudEvent;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceInstance;
import com.sap.cloudfoundry.client.facade.domain.ServiceOperation;

@Named
public class ServiceOperationGetter {

    private static final String USER_PROVIDED_SERVICE_EVENT_TYPE_DELETE = "audit.user_provided_service_instance.delete";
    private static final String SERVICE_EVENT_TYPE_DELETE = "audit.service_instance.delete";

    public ServiceOperation getLastServiceOperation(CloudControllerClient client, CloudServiceInstanceExtended service) {
        CloudServiceInstance serviceInstance = client.getServiceInstance(service.getName(), false);
        if (serviceInstance == null) {
            return getLastDeleteServiceOperation(client, service);
        }
        return serviceInstance.getLastOperation();
    }

    private ServiceOperation getLastDeleteServiceOperation(CloudControllerClient client, CloudServiceInstanceExtended service) {
        if (service.getMetadata() == null) {
            return null;
        }
        boolean isServiceDeleted = isServiceDeleted(client, service.getMetadata()
                                                                   .getGuid());
        ServiceOperation.State operationState = isServiceDeleted ? ServiceOperation.State.SUCCEEDED : ServiceOperation.State.IN_PROGRESS;
        return new ServiceOperation(ServiceOperation.Type.DELETE, ServiceOperation.Type.DELETE.name(), operationState);
    }

    private boolean isServiceDeleted(CloudControllerClient client, UUID uuid) {
        List<CloudEvent> serviceEvent = client.getEventsByActee(uuid);
        return serviceEvent.stream()
                           .anyMatch(cloudEvent -> isDeleteEvent(cloudEvent.getType()));
    }

    private boolean isDeleteEvent(String eventType) {
        return SERVICE_EVENT_TYPE_DELETE.equalsIgnoreCase(eventType) || USER_PROVIDED_SERVICE_EVENT_TYPE_DELETE.equalsIgnoreCase(eventType);
    }
}
