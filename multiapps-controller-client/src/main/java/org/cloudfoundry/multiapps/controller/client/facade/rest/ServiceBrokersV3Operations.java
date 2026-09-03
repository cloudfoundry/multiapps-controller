package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.net.URI;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.cloudfoundry.multiapps.controller.Messages;
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

public class ServiceBrokersV3Operations {

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
                                          .uri(CloudControllerV3Endpoints.SERVICE_BROKERS)
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
                                          .uri(CloudControllerV3Endpoints.SERVICE_BROKER_BY_GUID, guid.toString())
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
            throw new CloudOperationException(HttpStatus.NOT_FOUND, Messages.NOT_FOUND,
                                              MessageFormat.format(Messages.SERVICE_BROKER_0_NOT_FOUND, name));
        }

        return serviceBroker;
    }

    public List<CloudServiceBroker> getServiceBrokers() {
        return cc.list(CloudControllerV3Endpoints.SERVICE_BROKERS + CloudControllerV3Endpoints.QUERY_PER_PAGE
                           + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE, BROKER_PAGE)
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
                                          .uri(CloudControllerV3Endpoints.SERVICE_BROKER_BY_GUID, brokerGuid.toString())
                                          .body(body)
                                          .retrieve()
                                          .toBodilessEntity();

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
        return cc.list(
                     CloudControllerV3Endpoints.SERVICE_BROKERS + CloudControllerV3Endpoints.QUERY_NAMES + name
                         + CloudControllerV3Endpoints.AMPERSAND_PER_PAGE
                         + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE, BROKER_PAGE)
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
        String uri = CloudControllerV3Endpoints.SERVICE_OFFERINGS + CloudControllerV3Endpoints.QUERY_SERVICE_BROKER_GUIDS + brokerGuid
            + CloudControllerV3Endpoints.AMPERSAND_SPACE_GUIDS + getTargetSpaceGuid() + CloudControllerV3Endpoints.AMPERSAND_PER_PAGE
            + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE;

        return cc.list(uri, OFFERING_PAGE)
                 .stream()
                 .map(ServiceOfferingRef::guid)
                 .map(UUID::fromString)
                 .toList();
    }

    private List<UUID> findServicePlanGuidsByOfferingGuid(UUID serviceOfferingGuid) {
        String uri =
            CloudControllerV3Endpoints.SERVICE_PLANS + CloudControllerV3Endpoints.QUERY_SERVICE_OFFERING_GUIDS + serviceOfferingGuid
                + CloudControllerV3Endpoints.AMPERSAND_PER_PAGE + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE;

        return cc.list(uri, PLAN_PAGE)
                 .stream()
                 .map(ServicePlanRef::guid)
                 .map(UUID::fromString)
                 .toList();
    }

    private void updateServicePlanVisibility(UUID servicePlanGuid, ServicePlanVisibility visibility) {
        cc.getRestClient()
          .patch()
          .uri(CloudControllerV3Endpoints.SERVICE_PLAN_VISIBILITY, servicePlanGuid.toString())
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

    private static String extractJobGuid(ResponseEntity<Void> response) {
        URI location = response.getHeaders()
                               .getLocation();

        if (location == null) {
            return null;
        }

        String value = location.toString();
        int index = value.lastIndexOf("/v3/jobs/");

        if (index < 0) {
            return value.substring(value.lastIndexOf('/') + 1);
        }

        return value.substring(index + "/v3/jobs/".length());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ServiceOfferingRef(@JsonProperty("guid") String guid) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ServicePlanRef(@JsonProperty("guid") String guid) {
    }

}
