package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ServicePlanVisibility;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ListResponse;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServicePlan;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;

/**
 * CF v3 <em>service plans</em> operations of the in-house {@code CloudControllerRestClient}. Reproduces the HTTP shape, filtering and
 * broker-scoped fan-out of the OSS {@code CloudControllerRestClientImpl#updateServicePlanVisibilityForBroker} on top of the shared
 * {@link CloudControllerV3Client} machinery.
 *
 * <p>
 * Endpoint map (see {@code docs/cf-java-client-migration/02-endpoint-inventory.md}):
 * </p>
 * <ul>
 * <li>resolve broker by name &rarr; {@code GET /v3/service_brokers?names=<name>} (404 when absent, as the broker is required);</li>
 * <li>resolve the broker's offerings &rarr; {@code GET /v3/service_offerings?service_broker_guids=<guid>&space_guids=<target>};</li>
 * <li>resolve each offering's plans &rarr; {@code GET /v3/service_plans?service_offering_guids=<guids>};</li>
 * <li>update visibility &rarr; {@code PATCH /v3/service_plans/{guid}/visibility} with body {@code { "type": "<visibility>" }}.</li>
 * </ul>
 *
 * <p>
 * The {@code visibility} body value is the lowercase CF visibility token ({@code public|admin|organization}); this matches the OSS impl,
 * which passes {@code Visibility.from(visibility.toString())} where {@link ServicePlanVisibility#toString()} lower-cases the enum name.
 * </p>
 */
public class ServicePlansV3Operations {

    // CF v3 caps per_page at 5000; use a large page to minimise round-trips (the pagination walker still handles multiple pages).
    private static final int PER_PAGE = 5000;

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

    // Mirrors the OSS getServiceBroker(name, true): find the broker by name in the (global) broker list and 404 if it is absent.
    private UUID getRequiredServiceBrokerGuid(String name) {
        String uri = "/v3/service_brokers?names=" + name + "&per_page=" + PER_PAGE;
        return cc.list(uri, BROKER_LIST_TYPE)
                 .stream()
                 .findFirst()
                 .map(broker -> UUID.fromString(broker.guid()))
                 .orElseThrow(() -> new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found",
                                                                "Service broker " + name + " not found."));
    }

    // Mirrors the OSS findServicePlansByBrokerGuid: list the broker's offerings (scoped to the target space), then each offering's plans.
    private List<UUID> findServicePlanGuidsByBrokerGuid(UUID brokerGuid) {
        List<UUID> offeringGuids = findServiceOfferingGuidsByBrokerGuid(brokerGuid);
        if (offeringGuids.isEmpty()) {
            return List.of();
        }
        String offeringGuidsFilter = offeringGuids.stream()
                                                  .map(UUID::toString)
                                                  .reduce((a, b) -> a + "," + b)
                                                  .orElse("");
        String uri = "/v3/service_plans?service_offering_guids=" + offeringGuidsFilter + "&per_page=" + PER_PAGE;
        return cc.list(uri, PLAN_LIST_TYPE)
                 .stream()
                 .map(plan -> UUID.fromString(plan.guid()))
                 .toList();
    }

    private List<UUID> findServiceOfferingGuidsByBrokerGuid(UUID brokerGuid) {
        StringBuilder query = new StringBuilder("/v3/service_offerings?service_broker_guids=" + brokerGuid + "&per_page=" + PER_PAGE);
        if (target != null && target.getGuid() != null) {
            query.append("&space_guids=")
                 .append(target.getGuid());
        }
        return cc.list(query.toString(), OFFERING_LIST_TYPE)
                 .stream()
                 .map(offering -> UUID.fromString(offering.guid()))
                 .toList();
    }

    // Mirrors the OSS updateServicePlanVisibility(UUID, ServicePlanVisibility): PATCH the plan's visibility with the lowercase CF token.
    private void updateServicePlanVisibility(UUID servicePlanGuid, ServicePlanVisibility visibility) {
        cc.getRestClient()
          .patch()
          .uri("/v3/service_plans/{guid}/visibility", servicePlanGuid)
          .body(Map.of("type", visibility.toString()))
          .retrieve()
          .toBodilessEntity();
    }

    // Minimal wire models to resolve GUIDs by name/relationship without depending on the (separately-owned) broker/offering groups.
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    private record V3ServiceBrokerRef(@com.fasterxml.jackson.annotation.JsonProperty("guid") String guid) {
    }

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    private record V3ServiceOfferingRef(@com.fasterxml.jackson.annotation.JsonProperty("guid") String guid) {
    }

}
