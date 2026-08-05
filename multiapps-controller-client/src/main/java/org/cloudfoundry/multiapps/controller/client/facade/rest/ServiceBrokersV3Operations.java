package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceBroker;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ServicePlanVisibility;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ListResponse;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServiceBroker;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServiceBrokerMapper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * CF v3 <em>service broker</em> operations of the in-house {@code CloudControllerRestClient}. Reproduces the HTTP shape, filtering,
 * async job handling and domain mapping of the OSS {@code CloudControllerRestClientImpl} service-broker methods on top of the shared
 * {@link CloudControllerV3Client} machinery.
 *
 * <p>
 * Endpoint map (see {@code docs/cf-java-client-migration/02-endpoint-inventory.md}):
 * </p>
 * <ul>
 * <li>{@code createServiceBroker} &rarr; {@code POST /v3/service_brokers} (async job; returns the job GUID)</li>
 * <li>{@code deleteServiceBroker} &rarr; {@code DELETE /v3/service_brokers/{guid}} (async job; returns the job GUID)</li>
 * <li>{@code getServiceBroker(name[, required])} &rarr; {@code GET /v3/service_brokers?names=<name>} (paginated, first match)</li>
 * <li>{@code getServiceBrokers} &rarr; {@code GET /v3/service_brokers} (paginated)</li>
 * <li>{@code updateServiceBroker} &rarr; {@code PATCH /v3/service_brokers/{guid}} (async job; returns the job GUID or {@code null})</li>
 * <li>{@code updateServicePlanVisibilityForBroker} &rarr; per plan {@code PATCH /v3/service_plans/{guid}/visibility}</li>
 * </ul>
 *
 * <p>
 * As in the OSS impl, {@code createServiceBroker}/{@code deleteServiceBroker}/{@code updateServiceBroker} return the async job GUID
 * <em>without</em> waiting for the job to complete — the caller polls the job separately.
 * </p>
 */
public class ServiceBrokersV3Operations {

    private static final int PER_PAGE = 5000;

    private static final ParameterizedTypeReference<V3ListResponse<V3ServiceBroker>> BROKER_PAGE = new ParameterizedTypeReference<>() {
    };
    private static final ParameterizedTypeReference<V3ListResponse<ServiceOfferingRef>> OFFERING_PAGE = new ParameterizedTypeReference<>() {
    };
    private static final ParameterizedTypeReference<V3ListResponse<ServicePlanRef>> PLAN_PAGE = new ParameterizedTypeReference<>() {
    };

    private final CloudControllerV3Client cc;
    private final CloudSpace target;

    public ServiceBrokersV3Operations(CloudControllerV3Client cc, CloudSpace target) {
        this.cc = cc;
        this.target = target;
    }

    public String createServiceBroker(CloudServiceBroker serviceBroker) {
        Assert.notNull(serviceBroker, "Service broker must not be null.");

        Map<String, Object> body = new HashMap<>();
        body.put("name", serviceBroker.getName());
        body.put("url", serviceBroker.getUrl());
        body.put("authentication", basicAuthentication(serviceBroker));
        if (serviceBroker.getSpaceGuid() != null) {
            body.put("relationships", Map.of("space", Map.of("data", Map.of("guid", serviceBroker.getSpaceGuid()))));
        }

        ResponseEntity<Void> response = cc.getRestClient()
                                          .post()
                                          .uri("/v3/service_brokers")
                                          .body(body)
                                          .retrieve()
                                          .toBodilessEntity();
        return extractJobGuid(response);
    }

    public String deleteServiceBroker(String name) {
        CloudServiceBroker broker = getServiceBroker(name);
        UUID guid = broker.getMetadata()
                          .getGuid();
        ResponseEntity<Void> response = cc.getRestClient()
                                          .delete()
                                          .uri("/v3/service_brokers/{guid}", guid.toString())
                                          .retrieve()
                                          .toBodilessEntity();
        return extractJobGuid(response);
    }

    public CloudServiceBroker getServiceBroker(String name) {
        return getServiceBroker(name, true);
    }

    public CloudServiceBroker getServiceBroker(String name, boolean required) {
        CloudServiceBroker serviceBroker = findServiceBrokerByName(name);
        if (serviceBroker == null && required) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Service broker " + name + " not found.");
        }
        return serviceBroker;
    }

    public List<CloudServiceBroker> getServiceBrokers() {
        return cc.list("/v3/service_brokers?per_page=" + PER_PAGE, BROKER_PAGE)
                 .stream()
                 .map(V3ServiceBrokerMapper::toCloudServiceBroker)
                 .toList();
    }

    public String updateServiceBroker(CloudServiceBroker serviceBroker) {
        Assert.notNull(serviceBroker, "Service broker must not be null.");

        CloudServiceBroker existingBroker = getServiceBroker(serviceBroker.getName());
        UUID brokerGuid = existingBroker.getMetadata()
                                        .getGuid();

        Map<String, Object> body = Map.of("name", serviceBroker.getName(), "url", serviceBroker.getUrl(), "authentication",
                                          basicAuthentication(serviceBroker));

        ResponseEntity<Void> response = cc.getRestClient()
                                          .patch()
                                          .uri("/v3/service_brokers/{guid}", brokerGuid.toString())
                                          .body(body)
                                          .retrieve()
                                          .toBodilessEntity();
        // A synchronous update returns 200 with no Location header (no job); an async update returns 202 with a job GUID.
        return extractJobGuid(response);
    }

    public void updateServicePlanVisibilityForBroker(String name, ServicePlanVisibility visibility) {
        CloudServiceBroker broker = getServiceBroker(name);
        UUID brokerGuid = broker.getMetadata()
                                .getGuid();
        for (UUID servicePlanGuid : findServicePlanGuidsByBrokerGuid(brokerGuid)) {
            updateServicePlanVisibility(servicePlanGuid, visibility);
        }
    }

    private CloudServiceBroker findServiceBrokerByName(String name) {
        return cc.list("/v3/service_brokers?names=" + name + "&per_page=" + PER_PAGE, BROKER_PAGE)
                 .stream()
                 .findFirst()
                 .map(V3ServiceBrokerMapper::toCloudServiceBroker)
                 .orElse(null);
    }

    private List<UUID> findServicePlanGuidsByBrokerGuid(UUID brokerGuid) {
        return findServiceOfferingGuidsByBrokerGuid(brokerGuid).stream()
                                                               .flatMap(offeringGuid -> findServicePlanGuidsByOfferingGuid(
                                                                   offeringGuid).stream())
                                                               .toList();
    }

    private List<UUID> findServiceOfferingGuidsByBrokerGuid(UUID brokerGuid) {
        String uri = "/v3/service_offerings?service_broker_guids=" + brokerGuid + "&space_guids=" + getTargetSpaceGuid() + "&per_page="
            + PER_PAGE;
        return cc.list(uri, OFFERING_PAGE)
                 .stream()
                 .map(ServiceOfferingRef::guid)
                 .map(UUID::fromString)
                 .toList();
    }

    private List<UUID> findServicePlanGuidsByOfferingGuid(UUID serviceOfferingGuid) {
        String uri = "/v3/service_plans?service_offering_guids=" + serviceOfferingGuid + "&per_page=" + PER_PAGE;
        return cc.list(uri, PLAN_PAGE)
                 .stream()
                 .map(ServicePlanRef::guid)
                 .map(UUID::fromString)
                 .toList();
    }

    private void updateServicePlanVisibility(UUID servicePlanGuid, ServicePlanVisibility visibility) {
        // ServicePlanVisibility.toString() yields the lowercase CF visibility type (public/admin/organization),
        // matching the OSS Visibility.from(visibility.toString()) mapping.
        cc.getRestClient()
          .patch()
          .uri("/v3/service_plans/{guid}/visibility", servicePlanGuid.toString())
          .body(Map.of("type", visibility.toString()))
          .retrieve()
          .toBodilessEntity();
    }

    private static Map<String, Object> basicAuthentication(CloudServiceBroker serviceBroker) {
        return Map.of("type", "basic", "credentials",
                      Map.of("username", nullToEmpty(serviceBroker.getUsername()), "password", nullToEmpty(serviceBroker.getPassword())));
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String getTargetSpaceGuid() {
        return target.getMetadata()
                     .getGuid()
                     .toString();
    }

    // A 202-Accepted async response carries the job in its Location header (/v3/jobs/{guid}); a synchronous 200 has none.
    private static String extractJobGuid(ResponseEntity<Void> response) {
        URI location = response.getHeaders()
                               .getLocation();
        if (location == null) {
            return null;
        }
        String value = location.toString();
        int idx = value.lastIndexOf("/v3/jobs/");
        if (idx < 0) {
            return value.substring(value.lastIndexOf('/') + 1);
        }
        return value.substring(idx + "/v3/jobs/".length());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ServiceOfferingRef(@JsonProperty("guid") String guid) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ServicePlanRef(@JsonProperty("guid") String guid) {
    }

}
