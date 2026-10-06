package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.Constants;
import org.cloudfoundry.multiapps.controller.Messages;
import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceInstance;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Metadata;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Fields;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3GuidReference;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ListResponse;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServiceInstance;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServiceInstanceMapper;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServiceOffering;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServicePlan;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class ServiceInstancesV3Operations {

    private static final ParameterizedTypeReference<V3ListResponse<V3ServiceInstance>> SERVICE_INSTANCE_LIST_TYPE = new ParameterizedTypeReference<>() {
    };

    private static final ParameterizedTypeReference<V3ListResponse<V3GuidReference>> GUID_REFERENCE_PAGE = new ParameterizedTypeReference<>() {
    };

    private final CloudControllerV3Client client;

    private final CloudSpace targetSpace;

    public ServiceInstancesV3Operations(CloudControllerV3Client client, CloudSpace targetSpace) {
        this.client = client;
        this.targetSpace = targetSpace;
    }

    public void createServiceInstance(CloudServiceInstance serviceInstance) {
        assertSpaceProvided("create service instance");
        Assert.notNull(serviceInstance, "Service instance must not be null.");
        UUID servicePlanGuid = findPlanGuidForService(serviceInstance, serviceInstance.getPlan());

        Map<String, Object> body = OperationsUtil.buildCreateServiceInstanceBody(serviceInstance, servicePlanGuid, targetSpace);

        client.post(CloudControllerV3Endpoints.SERVICE_INSTANCES, body);
    }

    public void createUserProvidedServiceInstance(CloudServiceInstance serviceInstance) {
        assertSpaceProvided("create service instance");
        Assert.notNull(serviceInstance, "Service instance must not be null.");
        String syslogDrainUrl = StringUtils.hasText(serviceInstance.getSyslogDrainUrl()) ? serviceInstance.getSyslogDrainUrl() : "";

        Map<String, Object> body = OperationsUtil.buildCreateUserProvidedServiceInstance(serviceInstance, syslogDrainUrl, targetSpace);

        client.post(CloudControllerV3Endpoints.SERVICE_INSTANCES, body);
    }

    public void deleteServiceInstance(String serviceInstanceName) {
        CloudServiceInstance serviceInstance = getServiceInstanceWithoutAuxiliaryContent(serviceInstanceName);
        deleteServiceInstance(serviceInstance.getGuid());
    }

    public void deleteServiceInstance(CloudServiceInstance serviceInstance) {
        deleteServiceInstance(serviceInstance.getGuid());
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
        V3ServiceInstance serviceInstance = client.get(CloudControllerV3Endpoints.SERVICE_INSTANCES + "/" + serviceInstanceGuid,
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
        Map<String, Object> parameters = client.get(CloudControllerV3Endpoints.SERVICE_INSTANCES + "/" + guid + "/parameters", Map.class);

        if (parameters == null) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, Messages.NOT_FOUND,
                                              MessageFormat.format(Messages.PARAMETERS_OF_SERVICE_INSTANCE_WITH_GUID_0_NOT_FOUND,
                                                                   guid));
        }

        return parameters;
    }

    public Map<String, Object> getUserProvidedServiceInstanceParameters(UUID guid) {
        Map<String, Object> credentials = client.get(CloudControllerV3Endpoints.SERVICE_INSTANCES + "/" + guid + "/credentials", Map.class);

        if (credentials == null) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, Messages.NOT_FOUND,
                                              MessageFormat.format(Messages.CREDENTIALS_OF_SERVICE_INSTANCE_WITH_GUID_0_NOT_FOUND,
                                                                   guid));
        }

        return credentials;
    }

    public List<CloudServiceInstance> getServiceInstancesWithoutAuxiliaryContentByNames(List<String> names) {
        List<CloudServiceInstance> allServiceInstances = new ArrayList<>();

        for (List<String> batch : OperationsUtil.toBatches(names, Constants.MAX_CHAR_LENGTH_FOR_PARAMS_IN_REQUEST)) {
            String uri = CloudControllerV3Endpoints.SERVICE_INSTANCES + CloudControllerV3Endpoints.QUERY_PER_PAGE
                + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE + CloudControllerV3Endpoints.AMPERSAND_SPACE_GUIDS
                + OperationsUtil.getGuid(targetSpace)
                + CloudControllerV3Endpoints.AMPERSAND_NAMES + String.join(",", batch);

            client.list(uri, SERVICE_INSTANCE_LIST_TYPE)
                  .stream()
                  .map(V3ServiceInstanceMapper::toCloudServiceInstanceWithoutAuxiliaryContent)
                  .forEach(allServiceInstances::add);
        }

        return allServiceInstances;
    }

    public List<CloudServiceInstance> getServiceInstancesByMetadataLabelSelector(String labelSelector) {
        String uri = CloudControllerV3Endpoints.SERVICE_INSTANCES + CloudControllerV3Endpoints.QUERY_PER_PAGE
            + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE + CloudControllerV3Endpoints.AMPERSAND_SPACE_GUIDS
            + OperationsUtil.getGuid(targetSpace)
            + CloudControllerV3Endpoints.AMPERSAND_LABEL_SELECTOR + labelSelector;

        return ReactiveFanOut.mapConcurrently(client.list(uri, SERVICE_INSTANCE_LIST_TYPE), this::mapWithSupportingContent);
    }

    public List<CloudServiceInstance> getServiceInstancesWithoutAuxiliaryContentByMetadataLabelSelector(String labelSelector) {
        String uri = CloudControllerV3Endpoints.SERVICE_INSTANCES + CloudControllerV3Endpoints.QUERY_PER_PAGE
            + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE + CloudControllerV3Endpoints.AMPERSAND_SPACE_GUIDS
            + OperationsUtil.getGuid(targetSpace)
            + CloudControllerV3Endpoints.AMPERSAND_LABEL_SELECTOR + labelSelector;

        return client.list(uri, SERVICE_INSTANCE_LIST_TYPE)
                     .stream()
                     .map(resource -> V3ServiceInstanceMapper.toCloudServiceInstance(resource, null, null))
                     .toList();
    }

    public void updateServicePlan(String serviceName, String planName) {
        CloudServiceInstance service = getServiceInstance(serviceName);

        if (service.isUserProvided()) {
            return;
        }

        UUID planGuid = findPlanGuidForService(service, planName);
        patchServiceInstance(service.getGuid(),
                             Map.of(V3Fields.RELATIONSHIPS,
                                    Map.of(V3Fields.SERVICE_PLAN, OperationsUtil.toOneRelationship(planGuid.toString()))));
    }

    public void updateServiceParameters(String serviceName, Map<String, Object> parameters) {
        CloudServiceInstance service = getServiceInstanceWithoutAuxiliaryContent(serviceName);
        String key = service.isUserProvided() ? V3Fields.CREDENTIALS : V3Fields.PARAMETERS;
        patchServiceInstance(service.getGuid(), Map.of(key, parameters));
    }

    public void updateServiceTags(String serviceName, List<String> tags) {
        UUID serviceInstanceGuid = getRequiredServiceInstanceGuid(serviceName);
        patchServiceInstance(serviceInstanceGuid, Map.of(V3Fields.TAGS, tags));
    }

    public void updateServiceSyslogDrainUrl(String serviceName, String syslogDrainUrl) {
        CloudServiceInstance service = getServiceInstanceWithoutAuxiliaryContent(serviceName);

        if (!service.isUserProvided()) {
            return;
        }

        String updatedSyslogDrain = StringUtils.hasText(syslogDrainUrl) ? syslogDrainUrl : "";
        patchServiceInstance(service.getGuid(), Map.of(V3Fields.SYSLOG_DRAIN_URL, updatedSyslogDrain));
    }

    public void updateServiceInstanceMetadata(UUID guid, Metadata metadata) {
        Map<String, Object> metadataMap = OperationsUtil.toMetadataMap(metadata);
        patchServiceInstance(guid, Map.of(V3Fields.METADATA, metadataMap == null ? Map.of() : metadataMap));
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
        if (OperationsUtil.isUserProvided(resource)) {
            return V3ServiceInstanceMapper.toCloudServiceInstance(resource, null, null);
        }

        String servicePlanGuid = OperationsUtil.servicePlanGuidOf(resource);
        if (servicePlanGuid == null) {
            return V3ServiceInstanceMapper.toCloudServiceInstance(resource, null, null);
        }

        ServicePlanNames names = resolvePlanAndOfferingNames(servicePlanGuid, resource.name());
        return V3ServiceInstanceMapper.toCloudServiceInstance(resource, names.planName(), names.offeringName());
    }

    private V3ServiceInstance findServiceInstanceResourceByName(String name) {
        String uri = CloudControllerV3Endpoints.SERVICE_INSTANCES + CloudControllerV3Endpoints.QUERY_PER_PAGE
            + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE + CloudControllerV3Endpoints.AMPERSAND_SPACE_GUIDS
            + OperationsUtil.getGuid(targetSpace)
            + CloudControllerV3Endpoints.AMPERSAND_NAMES + name;

        return client.list(uri, SERVICE_INSTANCE_LIST_TYPE)
                     .stream()
                     .findFirst()
                     .orElse(null);
    }

    private ServicePlanNames resolvePlanAndOfferingNames(String servicePlanGuid, String serviceInstanceName) {
        V3ServicePlan plan = getServicePlanForServiceInstance(servicePlanGuid, serviceInstanceName);

        if (plan == null) {
            return new ServicePlanNames(null, null);
        }

        String offeringName = null;
        String offeringGuid = OperationsUtil.getServiceOfferingGuidFromServicePlan(plan);

        if (offeringGuid != null) {
            V3ServiceOffering offering = getServiceOfferingForNameResolution(offeringGuid);
            offeringName = offering == null ? null : offering.name();
        }

        return new ServicePlanNames(plan.name(), offeringName);
    }

    private V3ServicePlan getServicePlanForServiceInstance(String servicePlanGuid, String serviceInstanceName) {
        try {
            return client.get(CloudControllerV3Endpoints.SERVICE_PLANS + "/" + servicePlanGuid, V3ServicePlan.class);
        } catch (CloudOperationException e) {
            throw OperationsUtil.determineNameResolutionError(e,
                                                              MessageFormat.format(
                                                                  Messages.SERVICE_PLAN_WITH_GUID_0_NOT_AVAILABLE_FOR_SERVICE_INSTANCE_1,
                                                                  servicePlanGuid, serviceInstanceName),
                                                              MessageFormat.format(Messages.NO_SERVICE_PLAN_FOUND, servicePlanGuid,
                                                                                   serviceInstanceName));
        }
    }

    private V3ServiceOffering getServiceOfferingForNameResolution(String offeringGuid) {
        try {
            return client.get(CloudControllerV3Endpoints.SERVICE_OFFERINGS + "/" + offeringGuid, V3ServiceOffering.class);
        } catch (CloudOperationException e) {
            throw OperationsUtil.determineNameResolutionError(e,
                                                              MessageFormat.format(Messages.SERVICE_OFFERING_WITH_GUID_0_IS_NOT_AVAILABLE,
                                                                                   offeringGuid),
                                                              MessageFormat.format(Messages.SERVICE_OFFERING_WITH_GUID_0_NOT_FOUND,
                                                                                   offeringGuid));
        }
    }

    private UUID findPlanGuidForService(CloudServiceInstance service, String planName) {
        List<String> offeringGuids = findServiceOfferingGuids(service.getLabel(), service.getBroker());

        for (String offeringGuid : offeringGuids) {
            String uri = CloudControllerV3Endpoints.SERVICE_PLANS + CloudControllerV3Endpoints.QUERY_PER_PAGE
                + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE + CloudControllerV3Endpoints.AMPERSAND_SERVICE_OFFERING_GUIDS + offeringGuid
                + CloudControllerV3Endpoints.AMPERSAND_NAMES + planName;

            V3GuidReference plan = client.list(uri, GUID_REFERENCE_PAGE)
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
                                                                                                         OperationsUtil.getGuid(
                                                                                                             targetSpace))
                                                                                                     .append(
                                                                                                         CloudControllerV3Endpoints.AMPERSAND_NAMES)
                                                                                                     .append(
                                                                                                         label);

        if (StringUtils.hasText(broker)) {
            uri.append(CloudControllerV3Endpoints.AMPERSAND_SERVICE_BROKER_NAMES)
               .append(broker);
        }

        return client.list(uri.toString(), GUID_REFERENCE_PAGE)
                     .stream()
                     .map(V3GuidReference::guid)
                     .toList();
    }

    private void deleteServiceInstance(UUID serviceInstanceGuid) {
        client.delete(CloudControllerV3Endpoints.SERVICE_INSTANCE_BY_GUID, serviceInstanceGuid.toString());
    }

    private void patchServiceInstance(UUID guid, Map<String, Object> body) {
        client.patch(CloudControllerV3Endpoints.SERVICE_INSTANCE_BY_GUID, body, guid.toString());
    }

    private void assertSpaceProvided(String operation) {
        Assert.notNull(targetSpace, "Unable to " + operation + " without specifying organization and space to use.");
    }

    private record ServicePlanNames(String planName, String offeringName) {
    }

}
