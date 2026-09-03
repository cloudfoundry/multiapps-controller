package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.controller.Constants;
import org.cloudfoundry.multiapps.controller.Messages;
import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudEntity;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceInstance;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Metadata;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ListResponse;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServiceInstance;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServiceInstanceMapper;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServiceOffering;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServicePlan;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;

public class ServiceInstancesV3Operations {

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
          .uri(CloudControllerV3Endpoints.SERVICE_INSTANCES)
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
          .uri(CloudControllerV3Endpoints.SERVICE_INSTANCES)
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
            throw new CloudOperationException(HttpStatus.NOT_FOUND, Messages.NOT_FOUND,
                                              MessageFormat.format(Messages.SERVICE_INSTANCE_0_NOT_FOUND, name));
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
        V3ServiceInstance serviceInstance = cc.get(CloudControllerV3Endpoints.SERVICE_INSTANCES + "/" + serviceInstanceGuid,
                                                   V3ServiceInstance.class);

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
        return getParameters(CloudControllerV3Endpoints.SERVICE_INSTANCES + "/" + guid + "/parameters");
    }

    public Map<String, Object> getUserProvidedServiceInstanceParameters(UUID guid) {
        return getParameters(CloudControllerV3Endpoints.SERVICE_INSTANCES + "/" + guid + "/credentials");
    }

    public List<CloudServiceInstance> getServiceInstancesWithoutAuxiliaryContentByNames(List<String> names) {
        List<CloudServiceInstance> allServiceInstances = new ArrayList<>();

        for (List<String> batch : toBatches(names, Constants.MAX_CHAR_LENGTH_FOR_PARAMS_IN_REQUEST)) {
            String uri = CloudControllerV3Endpoints.SERVICE_INSTANCES + CloudControllerV3Endpoints.QUERY_PER_PAGE
                + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE + CloudControllerV3Endpoints.AMPERSAND_SPACE_GUIDS + getTargetSpaceGuid()
                + CloudControllerV3Endpoints.AMPERSAND_NAMES + String.join(",", batch);

            cc.list(uri, SERVICE_INSTANCE_LIST_TYPE)
              .stream()
              .map(V3ServiceInstanceMapper::toCloudServiceInstanceWithoutAuxiliaryContent)
              .forEach(allServiceInstances::add);
        }

        return allServiceInstances;
    }

    public List<CloudServiceInstance> getServiceInstancesByMetadataLabelSelector(String labelSelector) {
        String uri = CloudControllerV3Endpoints.SERVICE_INSTANCES + CloudControllerV3Endpoints.QUERY_PER_PAGE
            + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE + CloudControllerV3Endpoints.AMPERSAND_SPACE_GUIDS + getTargetSpaceGuid()
            + CloudControllerV3Endpoints.AMPERSAND_LABEL_SELECTOR + labelSelector;

        return ReactiveFanOut.mapConcurrently(cc.list(uri, SERVICE_INSTANCE_LIST_TYPE), this::mapWithSupportingContent);
    }

    public List<CloudServiceInstance> getServiceInstancesWithoutAuxiliaryContentByMetadataLabelSelector(String labelSelector) {
        String uri = CloudControllerV3Endpoints.SERVICE_INSTANCES + CloudControllerV3Endpoints.QUERY_PER_PAGE
            + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE + CloudControllerV3Endpoints.AMPERSAND_SPACE_GUIDS + getTargetSpaceGuid()
            + CloudControllerV3Endpoints.AMPERSAND_LABEL_SELECTOR + labelSelector;

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
            throw new CloudOperationException(HttpStatus.NOT_FOUND, Messages.NOT_FOUND,
                                              MessageFormat.format(Messages.SERVICE_INSTANCE_0_NOT_FOUND, serviceInstanceName));
        }

        return serviceInstance;
    }

    private CloudServiceInstance findServiceInstanceByName(String name) {
        V3ServiceInstance resource = findServiceInstanceResourceByName(name);

        if (resource == null) {
            return null;
        }

        return mapWithSupportingContent(resource);
    }

    private CloudServiceInstance mapWithSupportingContent(V3ServiceInstance resource) {
        if (isUserProvided(resource)) {
            return V3ServiceInstanceMapper.toCloudServiceInstance(resource, null, null);
        }

        String servicePlanGuid = servicePlanGuidOf(resource);
        if (servicePlanGuid == null) {
            return V3ServiceInstanceMapper.toCloudServiceInstance(resource, null, null);
        }

        ServicePlanNames names = resolvePlanAndOfferingNames(servicePlanGuid, resource.name());
        return V3ServiceInstanceMapper.toCloudServiceInstance(resource, names.planName(), names.offeringName());
    }

    private V3ServiceInstance findServiceInstanceResourceByName(String name) {
        String uri = CloudControllerV3Endpoints.SERVICE_INSTANCES + CloudControllerV3Endpoints.QUERY_PER_PAGE
            + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE + CloudControllerV3Endpoints.AMPERSAND_SPACE_GUIDS + getTargetSpaceGuid()
            + CloudControllerV3Endpoints.AMPERSAND_NAMES + name;

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

    private ServicePlanNames resolvePlanAndOfferingNames(String servicePlanGuid, String serviceInstanceName) {
        V3ServicePlan plan = getServicePlanForNameResolution(servicePlanGuid, serviceInstanceName);

        if (plan == null) {
            return new ServicePlanNames(null, null);
        }

        String offeringName = null;
        String offeringGuid = offeringGuidOf(plan);

        if (offeringGuid != null) {
            V3ServiceOffering offering = getServiceOfferingForNameResolution(offeringGuid);
            offeringName = offering == null ? null : offering.name();
        }

        return new ServicePlanNames(plan.name(), offeringName);
    }

    private V3ServicePlan getServicePlanForNameResolution(String servicePlanGuid, String serviceInstanceName) {
        try {
            return cc.get(CloudControllerV3Endpoints.SERVICE_PLANS + "/" + servicePlanGuid, V3ServicePlan.class);
        } catch (CloudOperationException e) {
            if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
                throw new CloudOperationException(HttpStatus.FORBIDDEN, Messages.FORBIDDEN,
                                                  MessageFormat.format(
                                                      Messages.SERVICE_PLAN_WITH_GUID_0_NOT_AVAILABLE_FOR_SERVICE_INSTANCE_1,
                                                      servicePlanGuid, serviceInstanceName),
                                                  e);
            }

            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new CloudOperationException(HttpStatus.NOT_FOUND, Messages.NOT_FOUND,
                                                  MessageFormat.format(Messages.NO_SERVICE_PLAN_FOUND, servicePlanGuid,
                                                                       serviceInstanceName),
                                                  e);
            }

            throw e;
        }
    }

    private V3ServiceOffering getServiceOfferingForNameResolution(String offeringGuid) {
        try {
            return cc.get(CloudControllerV3Endpoints.SERVICE_OFFERINGS + "/" + offeringGuid, V3ServiceOffering.class);
        } catch (CloudOperationException e) {
            if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
                throw new CloudOperationException(HttpStatus.FORBIDDEN, Messages.FORBIDDEN,
                                                  MessageFormat.format(Messages.SERVICE_OFFERING_WITH_GUID_0_IS_NOT_AVAILABLE,
                                                                       offeringGuid),
                                                  e);
            }

            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new CloudOperationException(HttpStatus.NOT_FOUND, Messages.NOT_FOUND,
                                                  MessageFormat.format(Messages.SERVICE_OFFERING_WITH_GUID_0_NOT_FOUND, offeringGuid), e);
            }

            throw e;
        }
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

    private UUID findPlanGuidForService(CloudServiceInstance service, String planName) {
        List<String> offeringGuids = findServiceOfferingGuids(service.getLabel(), service.getBroker());

        for (String offeringGuid : offeringGuids) {
            String uri = CloudControllerV3Endpoints.SERVICE_PLANS + CloudControllerV3Endpoints.QUERY_PER_PAGE
                + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE + CloudControllerV3Endpoints.AMPERSAND_SERVICE_OFFERING_GUIDS + offeringGuid
                + CloudControllerV3Endpoints.AMPERSAND_NAMES + planName;

            V3ServicePlan plan = cc.list(uri, new ParameterizedTypeReference<V3ListResponse<V3ServicePlan>>() {
                                   })
                                   .stream()
                                   .findFirst()
                                   .orElse(null);

            if (plan != null) {
                return UUID.fromString(plan.guid());
            }
        }

        throw new CloudOperationException(HttpStatus.NOT_FOUND, Messages.NOT_FOUND,
                                          MessageFormat.format(Messages.SERVICE_PLAN_0_NOT_FOUND, planName));
    }

    private List<String> findServiceOfferingGuids(String label, String broker) {
        StringBuilder uri = new StringBuilder(
            CloudControllerV3Endpoints.SERVICE_OFFERINGS + CloudControllerV3Endpoints.QUERY_PER_PAGE).append(
                                                                                                         CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE)
                                                                                                     .append(
                                                                                                         CloudControllerV3Endpoints.AMPERSAND_SPACE_GUIDS)
                                                                                                     .append(
                                                                                                         getTargetSpaceGuid())
                                                                                                     .append(
                                                                                                         CloudControllerV3Endpoints.AMPERSAND_NAMES)
                                                                                                     .append(
                                                                                                         label);

        if (hasText(broker)) {
            uri.append(CloudControllerV3Endpoints.AMPERSAND_SERVICE_BROKER_NAMES)
               .append(broker);
        }

        return cc.list(uri.toString(), new ParameterizedTypeReference<V3ListResponse<V3ServiceOffering>>() {
                 })
                 .stream()
                 .map(V3ServiceOffering::guid)
                 .collect(Collectors.toList());
    }

    private void doDeleteServiceInstance(UUID serviceInstanceGuid) {
        cc.getRestClient()
          .delete()
          .uri(CloudControllerV3Endpoints.SERVICE_INSTANCE_BY_GUID, serviceInstanceGuid.toString())
          .retrieve()
          .toBodilessEntity();
    }

    private void patchServiceInstance(UUID guid, Map<String, Object> body) {
        cc.getRestClient()
          .patch()
          .uri(CloudControllerV3Endpoints.SERVICE_INSTANCE_BY_GUID, guid.toString())
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

        Map<String, Object> metadataMapResult = new HashMap<>();
        metadataMapResult.put("labels", metadata.getLabels() == null ? Map.of() : metadata.getLabels());
        metadataMapResult.put("annotations", metadata.getAnnotations() == null ? Map.of() : metadata.getAnnotations());

        return metadataMapResult;
    }

    private static void putIfNotNull(Map<String, Object> body, String key, Object value) {
        if (value != null) {
            body.put(key, value);
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

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
