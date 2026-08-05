package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudEntity;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Metadata;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceInstance;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ListResponse;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServiceInstance;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServiceInstanceMapper;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServiceOffering;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServicePlan;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;

/**
 * CF v3 <em>service instance</em> operations of the in-house {@code CloudControllerRestClient}. Reproduces the HTTP shape, filtering,
 * space-scoping and domain-mapping of the OSS {@code CloudControllerRestClientImpl} service-instance methods on top of the shared
 * {@link CloudControllerV3Client} machinery.
 *
 * <p>
 * Endpoint map (see {@code docs/cf-java-client-migration/02-endpoint-inventory.md}):
 * </p>
 * <ul>
 * <li>{@code createServiceInstance} &rarr; {@code POST /v3/service_instances} (type=managed)</li>
 * <li>{@code createUserProvidedServiceInstance} &rarr; {@code POST /v3/service_instances} (type=user-provided)</li>
 * <li>{@code deleteServiceInstance} &rarr; {@code DELETE /v3/service_instances/{guid}}</li>
 * <li>{@code getServiceInstance} / {@code getRequiredServiceInstanceGuid} / {@code getServiceInstanceWithoutAuxiliaryContent} &rarr;
 * {@code GET /v3/service_instances?space_guids=…&names=…} (paginated)</li>
 * <li>{@code getServiceInstanceName} &rarr; {@code GET /v3/service_instances/{guid}}</li>
 * <li>{@code getServiceInstanceParameters} &rarr; {@code GET /v3/service_instances/{guid}/parameters}</li>
 * <li>{@code getUserProvidedServiceInstanceParameters} &rarr; {@code GET /v3/service_instances/{guid}/credentials}</li>
 * <li>{@code getServiceInstancesWithoutAuxiliaryContentByNames} &rarr; {@code GET /v3/service_instances?space_guids=…&names=…} (batched)</li>
 * <li>{@code getServiceInstancesByMetadataLabelSelector} (+ WithoutAuxiliaryContent) &rarr;
 * {@code GET /v3/service_instances?space_guids=…&label_selector=…} (paginated)</li>
 * <li>{@code updateServicePlan} / {@code updateServiceParameters} / {@code updateServiceTags} / {@code updateServiceSyslogDrainUrl} /
 * {@code updateServiceInstanceMetadata} &rarr; {@code PATCH /v3/service_instances/{guid}}</li>
 * </ul>
 *
 * <p>
 * Async note: for managed instances CF returns 202 for create/update/delete, but the OSS impl does <em>not</em> follow the job — it returns
 * as soon as the request is accepted. This helper preserves that behaviour (no {@code followAsyncJob} on these calls).
 * </p>
 */
public class ServiceInstancesV3Operations {

    // CF v3 caps per_page at 5000; use a large page to minimise round-trips (the pagination walker still handles multiple pages).
    private static final int PER_PAGE = 5000;
    // Mirrors the OSS MAX_CHAR_LENGTH_FOR_PARAMS_IN_REQUEST used to batch the "byNames" lookups.
    private static final int MAX_CHAR_LENGTH_FOR_PARAMS_IN_REQUEST = 4000;

    private static final ParameterizedTypeReference<V3ListResponse<V3ServiceInstance>> SERVICE_INSTANCE_LIST_TYPE = new ParameterizedTypeReference<>() {
    };

    private final CloudControllerV3Client cc;
    private final CloudSpace target;

    public ServiceInstancesV3Operations(CloudControllerV3Client cc, CloudSpace target) {
        this.cc = cc;
        this.target = target;
    }

    public void createServiceInstance(CloudServiceInstance serviceInstance) {
        assertSpaceProvided("create service instance");
        Assert.notNull(serviceInstance, "Service instance must not be null.");
        UUID servicePlanGuid = findPlanGuidForService(serviceInstance, serviceInstance.getPlan());

        Map<String, Object> body = new HashMap<>();
        body.put("type", "managed");
        body.put("name", serviceInstance.getName());
        putIfNotNull(body, "metadata", toMetadataMap(serviceInstance.getV3Metadata()));
        putIfNotNull(body, "tags", serviceInstance.getTags());
        putIfNotNull(body, "parameters", serviceInstance.getCredentials());
        body.put("relationships", Map.of("service_plan", toOneRelationship(servicePlanGuid.toString()), "space",
                                         toOneRelationship(getTargetSpaceGuid().toString())));

        cc.getRestClient()
          .post()
          .uri("/v3/service_instances")
          .body(body)
          .retrieve()
          .toBodilessEntity();
    }

    public void createUserProvidedServiceInstance(CloudServiceInstance serviceInstance) {
        assertSpaceProvided("create service instance");
        Assert.notNull(serviceInstance, "Service instance must not be null.");
        String syslogDrainUrl = hasText(serviceInstance.getSyslogDrainUrl()) ? serviceInstance.getSyslogDrainUrl() : "";

        Map<String, Object> body = new HashMap<>();
        body.put("type", "user-provided");
        body.put("name", serviceInstance.getName());
        putIfNotNull(body, "metadata", toMetadataMap(serviceInstance.getV3Metadata()));
        putIfNotNull(body, "credentials", serviceInstance.getCredentials());
        body.put("syslog_drain_url", syslogDrainUrl);
        putIfNotNull(body, "tags", serviceInstance.getTags());
        body.put("relationships", Map.of("space", toOneRelationship(getTargetSpaceGuid().toString())));

        cc.getRestClient()
          .post()
          .uri("/v3/service_instances")
          .body(body)
          .retrieve()
          .toBodilessEntity();
    }

    public void deleteServiceInstance(String serviceInstanceName) {
        CloudServiceInstance serviceInstance = getServiceInstanceWithoutAuxiliaryContent(serviceInstanceName);
        doDeleteServiceInstance(serviceInstance.getGuid());
    }

    public void deleteServiceInstance(CloudServiceInstance serviceInstance) {
        doDeleteServiceInstance(serviceInstance.getGuid());
    }

    public UUID getRequiredServiceInstanceGuid(String name) {
        V3ServiceInstance resource = findServiceInstanceResourceByName(name);
        if (resource == null) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Service instance " + name + " not found.");
        }
        return UUID.fromString(resource.guid());
    }

    public CloudServiceInstance getServiceInstance(String serviceInstanceName) {
        return getServiceInstance(serviceInstanceName, true);
    }

    public CloudServiceInstance getServiceInstance(String serviceInstanceName, boolean required) {
        CloudServiceInstance serviceInstance = findServiceInstanceByName(serviceInstanceName);
        return getServiceInstanceIfRequired(serviceInstanceName, serviceInstance, required);
    }

    public String getServiceInstanceName(UUID serviceInstanceGuid) {
        V3ServiceInstance serviceInstance = cc.get("/v3/service_instances/" + serviceInstanceGuid, V3ServiceInstance.class);
        return serviceInstance == null ? null : serviceInstance.name();
    }

    public CloudServiceInstance getServiceInstanceWithoutAuxiliaryContent(String serviceInstanceName) {
        return getServiceInstanceWithoutAuxiliaryContent(serviceInstanceName, true);
    }

    public CloudServiceInstance getServiceInstanceWithoutAuxiliaryContent(String serviceInstanceName, boolean required) {
        V3ServiceInstance resource = findServiceInstanceResourceByName(serviceInstanceName);
        CloudServiceInstance serviceInstance = resource == null ? null : V3ServiceInstanceMapper.toCloudServiceInstance(resource, null,
                                                                                                                        null);
        return getServiceInstanceIfRequired(serviceInstanceName, serviceInstance, required);
    }

    public Map<String, Object> getServiceInstanceParameters(UUID guid) {
        return getParameters("/v3/service_instances/" + guid + "/parameters");
    }

    public Map<String, Object> getUserProvidedServiceInstanceParameters(UUID guid) {
        return getParameters("/v3/service_instances/" + guid + "/credentials");
    }

    public List<CloudServiceInstance> getServiceInstancesWithoutAuxiliaryContentByNames(List<String> names) {
        List<CloudServiceInstance> allServiceInstances = new ArrayList<>();
        for (List<String> batch : toBatches(names, MAX_CHAR_LENGTH_FOR_PARAMS_IN_REQUEST)) {
            String uri = "/v3/service_instances?per_page=" + PER_PAGE + "&space_guids=" + getTargetSpaceGuid() + "&names="
                + String.join(",", batch);
            cc.list(uri, SERVICE_INSTANCE_LIST_TYPE)
              .stream()
              .map(V3ServiceInstanceMapper::toCloudServiceInstanceWithoutAuxiliaryContent)
              .forEach(allServiceInstances::add);
        }
        return allServiceInstances;
    }

    public List<CloudServiceInstance> getServiceInstancesByMetadataLabelSelector(String labelSelector) {
        String uri = "/v3/service_instances?per_page=" + PER_PAGE + "&space_guids=" + getTargetSpaceGuid() + "&label_selector="
            + labelSelector;
        return cc.list(uri, SERVICE_INSTANCE_LIST_TYPE)
                 .stream()
                 .map(this::mapWithAuxiliaryContent)
                 .collect(Collectors.toList());
    }

    public List<CloudServiceInstance> getServiceInstancesWithoutAuxiliaryContentByMetadataLabelSelector(String labelSelector) {
        String uri = "/v3/service_instances?per_page=" + PER_PAGE + "&space_guids=" + getTargetSpaceGuid() + "&label_selector="
            + labelSelector;
        return cc.list(uri, SERVICE_INSTANCE_LIST_TYPE)
                 .stream()
                 .map(resource -> V3ServiceInstanceMapper.toCloudServiceInstance(resource, null, null))
                 .collect(Collectors.toList());
    }

    public void updateServicePlan(String serviceName, String planName) {
        CloudServiceInstance service = getServiceInstance(serviceName);
        if (service.isUserProvided()) {
            return;
        }
        UUID planGuid = findPlanGuidForService(service, planName);
        patchServiceInstance(service.getGuid(), Map.of("relationships", Map.of("service_plan", toOneRelationship(planGuid.toString()))));
    }

    public void updateServiceParameters(String serviceName, Map<String, Object> parameters) {
        CloudServiceInstance service = getServiceInstanceWithoutAuxiliaryContent(serviceName);
        String key = service.isUserProvided() ? "credentials" : "parameters";
        patchServiceInstance(service.getGuid(), Map.of(key, parameters));
    }

    public void updateServiceTags(String serviceName, List<String> tags) {
        UUID serviceInstanceGuid = getRequiredServiceInstanceGuid(serviceName);
        patchServiceInstance(serviceInstanceGuid, Map.of("tags", tags));
    }

    public void updateServiceSyslogDrainUrl(String serviceName, String syslogDrainUrl) {
        CloudServiceInstance service = getServiceInstanceWithoutAuxiliaryContent(serviceName);
        if (!service.isUserProvided()) {
            return;
        }
        String updatedSyslogDrain = hasText(syslogDrainUrl) ? syslogDrainUrl : "";
        patchServiceInstance(service.getGuid(), Map.of("syslog_drain_url", updatedSyslogDrain));
    }

    public void updateServiceInstanceMetadata(UUID guid, Metadata metadata) {
        Map<String, Object> metadataMap = toMetadataMap(metadata);
        patchServiceInstance(guid, Map.of("metadata", metadataMap == null ? Map.of() : metadataMap));
    }

    private CloudServiceInstance getServiceInstanceIfRequired(String serviceInstanceName, CloudServiceInstance serviceInstance,
                                                              boolean required) {
        if (serviceInstance == null && required) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Service instance " + serviceInstanceName + " not found.");
        }
        return serviceInstance;
    }

    // Mirrors the OSS findServiceInstanceByName -> zipWithAuxiliaryServiceInstanceContent: for managed instances the plan and offering
    // names are resolved and included; user-provided instances are mapped as-is.
    private CloudServiceInstance findServiceInstanceByName(String name) {
        V3ServiceInstance resource = findServiceInstanceResourceByName(name);
        if (resource == null) {
            return null;
        }
        return mapWithAuxiliaryContent(resource);
    }

    private CloudServiceInstance mapWithAuxiliaryContent(V3ServiceInstance resource) {
        if (isUserProvided(resource)) {
            return V3ServiceInstanceMapper.toCloudServiceInstance(resource, null, null);
        }
        String servicePlanGuid = servicePlanGuidOf(resource);
        if (servicePlanGuid == null) {
            return V3ServiceInstanceMapper.toCloudServiceInstance(resource, null, null);
        }
        ServicePlanNames names = resolvePlanAndOfferingNames(servicePlanGuid);
        return V3ServiceInstanceMapper.toCloudServiceInstance(resource, names.planName(), names.offeringName());
    }

    private V3ServiceInstance findServiceInstanceResourceByName(String name) {
        String uri = "/v3/service_instances?per_page=" + PER_PAGE + "&space_guids=" + getTargetSpaceGuid() + "&names=" + name;
        return cc.list(uri, SERVICE_INSTANCE_LIST_TYPE)
                 .stream()
                 .findFirst()
                 .orElse(null);
    }

    private boolean isUserProvided(V3ServiceInstance resource) {
        return "user-provided".equals(resource.type());
    }

    private String servicePlanGuidOf(V3ServiceInstance resource) {
        if (resource.relationships() == null || resource.relationships()
                                                        .servicePlan() == null
            || resource.relationships()
                       .servicePlan()
                       .data() == null) {
            return null;
        }
        return resource.relationships()
                       .servicePlan()
                       .data()
                       .guid();
    }

    // Mirrors the OSS getServicePlanResource + getServiceOffering zip: resolve the plan's own name and its offering's name.
    private ServicePlanNames resolvePlanAndOfferingNames(String servicePlanGuid) {
        V3ServicePlan plan = cc.get("/v3/service_plans/" + servicePlanGuid, V3ServicePlan.class);
        if (plan == null) {
            return new ServicePlanNames(null, null);
        }
        String offeringName = null;
        String offeringGuid = offeringGuidOf(plan);
        if (offeringGuid != null) {
            V3ServiceOffering offering = cc.get("/v3/service_offerings/" + offeringGuid, V3ServiceOffering.class);
            offeringName = offering == null ? null : offering.name();
        }
        return new ServicePlanNames(plan.name(), offeringName);
    }

    private String offeringGuidOf(V3ServicePlan plan) {
        if (plan.relationships() == null || plan.relationships()
                                                .serviceOffering() == null
            || plan.relationships()
                   .serviceOffering()
                   .data() == null) {
            return null;
        }
        return plan.relationships()
                   .serviceOffering()
                   .data()
                   .guid();
    }

    // Mirrors the OSS findPlanForService: list the offerings for the service's label (optionally scoped by broker), then find the plan by
    // name; a missing plan throws CloudOperationException(NOT_FOUND).
    private UUID findPlanGuidForService(CloudServiceInstance service, String planName) {
        List<String> offeringGuids = findServiceOfferingGuids(service.getLabel(), service.getBroker());
        for (String offeringGuid : offeringGuids) {
            String uri = "/v3/service_plans?per_page=" + PER_PAGE + "&service_offering_guids=" + offeringGuid + "&names=" + planName;
            V3ServicePlan plan = cc.list(uri, new ParameterizedTypeReference<V3ListResponse<V3ServicePlan>>() {
            })
                                   .stream()
                                   .findFirst()
                                   .orElse(null);
            if (plan != null) {
                return UUID.fromString(plan.guid());
            }
        }
        throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Service plan " + planName + " not found.");
    }

    private List<String> findServiceOfferingGuids(String label, String broker) {
        StringBuilder uri = new StringBuilder("/v3/service_offerings?per_page=").append(PER_PAGE)
                                                                                .append("&space_guids=")
                                                                                .append(getTargetSpaceGuid())
                                                                                .append("&names=")
                                                                                .append(label);
        if (hasText(broker)) {
            uri.append("&service_broker_names=")
               .append(broker);
        }
        return cc.list(uri.toString(), new ParameterizedTypeReference<V3ListResponse<V3ServiceOffering>>() {
        })
                 .stream()
                 .map(V3ServiceOffering::guid)
                 .collect(Collectors.toList());
    }

    private void doDeleteServiceInstance(UUID serviceInstanceGuid) {
        // OSS parity: the delete is issued and the response consumed, but the async job is NOT followed.
        cc.getRestClient()
          .delete()
          .uri("/v3/service_instances/{guid}", serviceInstanceGuid.toString())
          .retrieve()
          .toBodilessEntity();
    }

    private void patchServiceInstance(UUID guid, Map<String, Object> body) {
        // OSS parity: the update is issued and the response consumed, but the async job is NOT followed.
        cc.getRestClient()
          .patch()
          .uri("/v3/service_instances/{guid}", guid.toString())
          .body(body)
          .retrieve()
          .toBodilessEntity();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getParameters(String uri) {
        return cc.get(uri, Map.class);
    }

    private Map<String, Object> toOneRelationship(String guid) {
        return Map.of("data", Map.of("guid", guid));
    }

    private Map<String, Object> toMetadataMap(Metadata metadata) {
        if (metadata == null) {
            return null;
        }
        Map<String, Object> map = new HashMap<>();
        map.put("labels", metadata.getLabels() == null ? Map.of() : metadata.getLabels());
        map.put("annotations", metadata.getAnnotations() == null ? Map.of() : metadata.getAnnotations());
        return map;
    }

    private static void putIfNotNull(Map<String, Object> body, String key, Object value) {
        if (value != null) {
            body.put(key, value);
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    // Mirrors the OSS toBatches: split a large collection into batches bounded by the total character length of the elements.
    private <T> List<List<T>> toBatches(Collection<T> largeList, int maxCharLength) {
        if (largeList.isEmpty()) {
            return List.of();
        }
        List<List<T>> batches = new ArrayList<>();
        int currentBatchLength = 0;
        int currentBatchIndex = 0;
        batches.add(new ArrayList<>());
        for (T element : largeList) {
            int elementLength = element.toString()
                                       .length();
            if (elementLength + currentBatchLength >= maxCharLength) {
                batches.add(new ArrayList<>());
                currentBatchIndex++;
                currentBatchLength = 0;
            }
            batches.get(currentBatchIndex)
                   .add(element);
            currentBatchLength += elementLength;
        }
        return batches;
    }

    private UUID getTargetSpaceGuid() {
        return getGuid(target);
    }

    private UUID getGuid(CloudEntity entity) {
        if (entity == null || entity.getMetadata() == null) {
            return null;
        }
        return entity.getMetadata()
                     .getGuid();
    }

    private void assertSpaceProvided(String operation) {
        Assert.notNull(target, "Unable to " + operation + " without specifying organization and space to use.");
    }

    private record ServicePlanNames(String planName, String offeringName) {
    }

}
