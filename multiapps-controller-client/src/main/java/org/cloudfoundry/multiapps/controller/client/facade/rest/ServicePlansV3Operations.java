package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.cloudfoundry.multiapps.controller.Messages;
import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ServicePlanVisibility;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ListResponse;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServicePlan;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;

public class ServicePlansV3Operations {

    private static final ParameterizedTypeReference<V3ListResponse<V3ServiceBrokerRef>> BROKER_LIST_TYPE = new ParameterizedTypeReference<>() {
    };
    private static final ParameterizedTypeReference<V3ListResponse<V3ServiceOfferingRef>> OFFERING_LIST_TYPE = new ParameterizedTypeReference<>() {
    };
    private static final ParameterizedTypeReference<V3ListResponse<V3ServicePlan>> PLAN_LIST_TYPE = new ParameterizedTypeReference<>() {
    };

    private final CloudControllerV3Client cc;
    private final CloudSpace target;

    public ServicePlansV3Operations(CloudControllerV3Client cc, CloudSpace target) {
        this.cc = cc;
        this.target = target;
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

        return cc.list(uri, BROKER_LIST_TYPE)
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
                                                         .reduce((a, b) -> a + "," + b)
                                                         .orElse("");

        String uri = CloudControllerV3Endpoints.SERVICE_PLANS + CloudControllerV3Endpoints.QUERY_SERVICE_OFFERING_GUIDS
            + offeringGuidsFilter + CloudControllerV3Endpoints.AMPERSAND_PER_PAGE + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE;

        return cc.list(uri, PLAN_LIST_TYPE)
                 .stream()
                 .map(plan -> UUID.fromString(plan.guid()))
                 .toList();
    }

    private List<UUID> findServiceOfferingGuidsByBrokerGuid(UUID brokerGuid) {
        StringBuilder query = new StringBuilder(CloudControllerV3Endpoints.SERVICE_OFFERINGS
                                                    + CloudControllerV3Endpoints.QUERY_SERVICE_BROKER_GUIDS + brokerGuid
                                                    + CloudControllerV3Endpoints.AMPERSAND_PER_PAGE
                                                    + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE);

        if (target != null && target.getGuid() != null) {
            query.append(CloudControllerV3Endpoints.AMPERSAND_SPACE_GUIDS)
                 .append(target.getGuid());
        }

        return cc.list(query.toString(), OFFERING_LIST_TYPE)
                 .stream()
                 .map(offering -> UUID.fromString(offering.guid()))
                 .toList();
    }

    private void updateServicePlanVisibility(UUID servicePlanGuid, ServicePlanVisibility visibility) {
        cc.getRestClient()
          .patch()
          .uri(CloudControllerV3Endpoints.SERVICE_PLAN_VISIBILITY, servicePlanGuid)
          .body(Map.of("type", visibility.toString()))
          .retrieve()
          .toBodilessEntity();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record V3ServiceBrokerRef(@JsonProperty("guid") String guid) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record V3ServiceOfferingRef(@JsonProperty("guid") String guid) {
    }

}
