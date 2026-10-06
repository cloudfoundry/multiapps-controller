package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.controller.Messages;
import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ServicePlanVisibility;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Fields;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3GuidReference;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ListResponse;
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
        String uri = CloudControllerV3Endpoints.SERVICE_BROKERS + CloudControllerV3Endpoints.QUERY_NAMES + name
            + CloudControllerV3Endpoints.AMPERSAND_PER_PAGE + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE;

        return client.list(uri, GUID_REFERENCE_PAGE)
                     .stream()
                     .findFirst()
                     .map(broker -> UUID.fromString(broker.guid()))
                     .orElseThrow(() -> new CloudOperationException(HttpStatus.NOT_FOUND, Messages.NOT_FOUND,
                                                                    MessageFormat.format(Messages.SERVICE_BROKER_0_NOT_FOUND, name)));
    }

    private List<UUID> findServicePlanGuidsByBrokerGuid(UUID brokerGuid) {
        List<UUID> offeredServicesGuids = findServiceOfferingGuidsByBrokerGuid(brokerGuid);
        if (offeredServicesGuids.isEmpty()) {
            return List.of();
        }

        String offeringGuidsFilter = offeredServicesGuids.stream()
                                                         .map(UUID::toString)
                                                         .collect(Collectors.joining(","));

        String uri = CloudControllerV3Endpoints.SERVICE_PLANS + CloudControllerV3Endpoints.QUERY_SERVICE_OFFERING_GUIDS
            + offeringGuidsFilter + CloudControllerV3Endpoints.AMPERSAND_PER_PAGE + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE;

        return client.list(uri, GUID_REFERENCE_PAGE)
                     .stream()
                     .map(plan -> UUID.fromString(plan.guid()))
                     .toList();
    }

    private List<UUID> findServiceOfferingGuidsByBrokerGuid(UUID brokerGuid) {
        StringBuilder query = new StringBuilder(CloudControllerV3Endpoints.SERVICE_OFFERINGS
                                                    + CloudControllerV3Endpoints.QUERY_SERVICE_BROKER_GUIDS + brokerGuid
                                                    + CloudControllerV3Endpoints.AMPERSAND_PER_PAGE
                                                    + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE);

        if (targetSpace != null && targetSpace.getGuid() != null) {
            query.append(CloudControllerV3Endpoints.AMPERSAND_SPACE_GUIDS)
                 .append(targetSpace.getGuid());
        }

        return client.list(query.toString(), GUID_REFERENCE_PAGE)
                     .stream()
                     .map(offering -> UUID.fromString(offering.guid()))
                     .toList();
    }

    private void updateServicePlanVisibility(UUID servicePlanGuid, ServicePlanVisibility visibility) {
        client.patch(CloudControllerV3Endpoints.SERVICE_PLAN_VISIBILITY, Map.of(V3Fields.TYPE, visibility.toString()), servicePlanGuid);
    }

}
