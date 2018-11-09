package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudEntity.Meta;
import org.cloudfoundry.client.lib.domain.CloudEvent;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.clients.EventsGetter;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperation;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperationState;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperationType;

public class PollServiceDeleteOperationsExecution extends PollServiceOperationsExecution implements AsyncExecution {

    private EventsGetter eventsGetter;

    public PollServiceDeleteOperationsExecution(EventsGetter eventsGetter) {
        this.eventsGetter = eventsGetter;
    }

    @Override
    protected List<CloudServiceExtended> computeServicesToPoll(ExecutionWrapper execution,
        Map<String, ServiceOperationType> triggeredServiceOperations) {
        List<String> servicesToDeleteNames = StepsUtil.getServicesToDelete(execution.getContext());
        List<CloudServiceExtended> servicesToDelete = getServicesData(servicesToDeleteNames, execution);
        return getServicesWithTriggeredOperations(servicesToDelete, triggeredServiceOperations);
    }

    private List<CloudServiceExtended> getServicesData(List<String> servicesToDeleteNames, ExecutionWrapper execution) {
        Map<String, String> servicesGuids = StepsUtil.getServicesGuids(execution.getContext());
        return servicesToDeleteNames.stream()
            .map(name -> new CloudServiceExtended(new Meta(UUID.fromString(servicesGuids.get(name)), null, null), name))
            .collect(Collectors.toList());
    }

    @Override
    protected ServiceOperation getLastServiceOperation(ExecutionWrapper execution, CloudControllerClient client,
        CloudServiceExtended service) {
        return getLastDeleteServiceOperation(execution, service);
    }

    private ServiceOperation getLastDeleteServiceOperation(ExecutionWrapper execution, CloudServiceExtended service) {
        if (service.getMeta() == null) {
            return null;
        }
        boolean isServiceDeleted = isServiceDeleted(execution, service.getMeta()
            .getGuid());
        ServiceOperationState operationState = isServiceDeleted ? ServiceOperationState.SUCCEEDED : ServiceOperationState.IN_PROGRESS;
        return new ServiceOperation(ServiceOperationType.DELETE, ServiceOperationType.DELETE.name(), operationState);
    }

    private boolean isServiceDeleted(ExecutionWrapper execution, UUID uuid) {
        CloudEvent serviceEvent = eventsGetter.getLastEvent(uuid, execution.getControllerClient());
        return serviceEvent != null && eventsGetter.isDeleteEvent(serviceEvent.getType());
    }
}
