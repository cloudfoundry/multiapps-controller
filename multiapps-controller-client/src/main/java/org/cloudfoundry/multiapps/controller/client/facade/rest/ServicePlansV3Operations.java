package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.Messages;
import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ServicePlanVisibility;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Fields;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3GuidReference;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ListResponse;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3QueryParameters;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;

public class ServicePlansV3Operations {

    private static final ParameterizedTypeReference<V3ListResponse<V3GuidReference>> GUID_REFERENCE_PAGE = new ParameterizedTypeReference<>() {
    };

    private final CloudControllerV3Client client;

    private final CloudSpace targetSpace;

    public ServicePlansV3Operations(CloudControllerV3Client client, CloudSpace targetSpace) {
        this.client = client;
        this.targetSpace = targetSpace;
    }

    public void updateServicePlanVisibilityForBroker(String name, ServicePlanVisibility visibility) {
        UUID brokerGuid = getRequiredServiceBrokerGuid(name);
        List<UUID> servicePlanGuids = findServicePlanGuidsByBrokerGuid(brokerGuid);

        for (UUID servicePlanGuid : servicePlanGuids) {
            updateServicePlanVisibility(servicePlanGuid, visibility);
        }
    }

    private UUID getRequiredServiceBrokerGuid(String name) {
        String uri = V3Uri.path(CloudControllerV3Endpoints.SERVICE_BROKERS)
                          .query(V3QueryParameters.NAMES, name)
                          .perPage(CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE)
                          .build();

        return client.list(uri, GUID_REFERENCE_PAGE)
                     .stream()
                     .findFirst()
                     .map(broker -> UUID.fromString(broker.guid()))
                     .orElseThrow(() -> new CloudOperationException(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.getReasonPhrase(),
                                                                    MessageFormat.format(Messages.SERVICE_BROKER_0_NOT_FOUND, name)));
    }

    private List<UUID> findServicePlanGuidsByBrokerGuid(UUID brokerGuid) {
        List<UUID> offeredServicesGuids = findServiceOfferingGuidsByBrokerGuid(brokerGuid);
        if (offeredServicesGuids.isEmpty()) {
            return List.of();
        }

        String uri = V3Uri.path(CloudControllerV3Endpoints.SERVICE_PLANS)
                          .query(V3QueryParameters.SERVICE_OFFERING_GUIDS, offeredServicesGuids)
                          .perPage(CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE)
                          .build();

        return client.list(uri, GUID_REFERENCE_PAGE)
                     .stream()
                     .map(plan -> UUID.fromString(plan.guid()))
                     .toList();
    }

    private List<UUID> findServiceOfferingGuidsByBrokerGuid(UUID brokerGuid) {
        String uri = V3Uri.path(CloudControllerV3Endpoints.SERVICE_OFFERINGS)
                          .query(V3QueryParameters.SERVICE_BROKER_GUIDS, brokerGuid)
                          .perPage(CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE)
                          .query(V3QueryParameters.SPACE_GUIDS, OperationsUtil.getGuid(targetSpace))
                          .build();

        return client.list(uri, GUID_REFERENCE_PAGE)
                     .stream()
                     .map(offering -> UUID.fromString(offering.guid()))
                     .toList();
    }

    private void updateServicePlanVisibility(UUID servicePlanGuid, ServicePlanVisibility visibility) {
        client.patch(CloudControllerV3Endpoints.SERVICE_PLAN_VISIBILITY, Map.of(V3Fields.TYPE, visibility.toString()), servicePlanGuid);
    }

}
