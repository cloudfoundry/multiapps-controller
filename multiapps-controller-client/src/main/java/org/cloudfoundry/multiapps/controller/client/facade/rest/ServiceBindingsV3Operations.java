package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.cloudfoundry.multiapps.controller.Messages;
import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceBinding;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Metadata;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Application;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ListResponse;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServiceBinding;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServiceBindingMapper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;

public class ServiceBindingsV3Operations {

    private static final ParameterizedTypeReference<V3ListResponse<V3ServiceBinding>> BINDING_LIST_TYPE = new ParameterizedTypeReference<>() {
    };
    private static final ParameterizedTypeReference<V3ListResponse<V3Application>> APPLICATION_LIST_TYPE = new ParameterizedTypeReference<>() {
    };

    private final CloudControllerV3Client cc;
    private final CloudSpace target;

    public ServiceBindingsV3Operations(CloudControllerV3Client cc, CloudSpace target) {
        this.cc = cc;
        this.target = target;
    }

    public Optional<String> bindServiceInstance(String bindingName, String applicationName, String serviceInstanceName) {
        return bindServiceInstance(bindingName, applicationName, serviceInstanceName, null);
    }

    public Optional<String> bindServiceInstance(String bindingName, String applicationName, String serviceInstanceName,
                                                Map<String, Object> parameters) {
        UUID applicationGuid = getRequiredApplicationGuid(applicationName);
        UUID serviceInstanceGuid = getRequiredServiceInstanceGuid(serviceInstanceName);

        Map<String, Object> body = new HashMap<>();
        body.put("name", bindingName);
        body.put("type", "app");
        body.put("relationships", Map.of("app", toOneRelationship(applicationGuid), "service_instance",
                                         toOneRelationship(serviceInstanceGuid)));

        if (!CollectionUtils.isEmpty(parameters)) {
            body.put("parameters", parameters);
        }

        ResponseEntity<Void> response = cc.getRestClient()
                                          .post()
                                          .uri(CloudControllerV3Endpoints.SERVICE_CREDENTIAL_BINDINGS)
                                          .body(body)
                                          .retrieve()
                                          .toBodilessEntity();

        return extractJobGuidFromResponseHeader(response);
    }

    public List<String> unbindServiceInstance(String applicationName, String serviceInstanceName) {
        UUID applicationGuid = getRequiredApplicationGuid(applicationName);
        UUID serviceInstanceGuid = getRequiredServiceInstanceGuid(serviceInstanceName);

        return doUnbindServiceInstance(applicationGuid, serviceInstanceGuid);
    }

    public List<String> unbindServiceInstance(UUID applicationGuid, UUID serviceInstanceGuid) {
        return doUnbindServiceInstance(applicationGuid, serviceInstanceGuid);
    }

    public Optional<String> deleteServiceBinding(UUID bindingGuid) {
        return deleteSingleServiceBinding(bindingGuid);
    }

    public CloudServiceBinding getServiceBinding(UUID serviceBindingGuid) {
        String uri = CloudControllerV3Endpoints.SERVICE_CREDENTIAL_BINDINGS + CloudControllerV3Endpoints.QUERY_GUIDS + serviceBindingGuid
            + CloudControllerV3Endpoints.AMPERSAND_PER_PAGE + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE;

        return cc.list(uri, BINDING_LIST_TYPE)
                 .stream()
                 .findFirst()
                 .map(V3ServiceBindingMapper::toCloudServiceBinding)
                 .orElse(null);
    }

    public List<CloudServiceBinding> getServiceAppBindings(UUID serviceInstanceGuid) {
        String uri = CloudControllerV3Endpoints.SERVICE_CREDENTIAL_BINDINGS + CloudControllerV3Endpoints.QUERY_SERVICE_INSTANCE_GUIDS
            + serviceInstanceGuid + CloudControllerV3Endpoints.AMPERSAND_TYPE + "app" + CloudControllerV3Endpoints.AMPERSAND_PER_PAGE
            + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE;

        return listBindings(uri);
    }

    public List<CloudServiceBinding> getAppBindings(UUID applicationGuid) {
        String uri = CloudControllerV3Endpoints.SERVICE_CREDENTIAL_BINDINGS + CloudControllerV3Endpoints.QUERY_APP_GUIDS + applicationGuid
            + CloudControllerV3Endpoints.AMPERSAND_PER_PAGE + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE;

        return listBindings(uri);
    }

    public List<CloudServiceBinding> getServiceBindingsForApplication(UUID applicationId, UUID serviceInstanceGuid) {
        String uri = CloudControllerV3Endpoints.SERVICE_CREDENTIAL_BINDINGS + CloudControllerV3Endpoints.QUERY_APP_GUIDS + applicationId
            + CloudControllerV3Endpoints.AMPERSAND_SERVICE_INSTANCE_GUIDS + serviceInstanceGuid
            + CloudControllerV3Endpoints.AMPERSAND_PER_PAGE
            + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE;

        return listBindings(uri);
    }

    public Map<String, Object> getServiceBindingParameters(UUID guid) {
        @SuppressWarnings("unchecked")
        Map<String, Object> parameters = cc.get(CloudControllerV3Endpoints.SERVICE_CREDENTIAL_BINDINGS + "/" + guid + "/parameters",
                                                Map.class);

        return parameters;
    }

    public void updateServiceBindingMetadata(UUID guid, Metadata metadata) {
        Map<String, Object> metadataBody = new HashMap<>();
        metadataBody.put("labels", metadata.getLabels());
        metadataBody.put("annotations", metadata.getAnnotations());

        cc.getRestClient()
          .patch()
          .uri(CloudControllerV3Endpoints.SERVICE_CREDENTIAL_BINDING_BY_GUID, guid)
          .body(Map.of("metadata", metadataBody))
          .retrieve()
          .toBodilessEntity();
    }

    private List<CloudServiceBinding> listBindings(String uri) {
        return cc.list(uri, BINDING_LIST_TYPE)
                 .stream()
                 .map(V3ServiceBindingMapper::toCloudServiceBinding)
                 .toList();
    }

    private List<String> doUnbindServiceInstance(UUID applicationGuid, UUID serviceInstanceGuid) {
        List<UUID> serviceBindingGuids = getServiceBindingGuids(applicationGuid, serviceInstanceGuid);

        if (serviceBindingGuids.isEmpty()) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, Messages.NOT_FOUND, MessageFormat.format(
                Messages.SERVICE_BINDING_BETWEEN_SERVICE_WITH_GUID_0_AND_APPLICATION_WITH_GUID_1_NOT_FOUND, serviceInstanceGuid,
                applicationGuid));
        }

        return doDeleteServiceBindings(serviceBindingGuids);
    }

    private List<UUID> getServiceBindingGuids(UUID applicationGuid, UUID serviceInstanceGuid) {
        String uri = CloudControllerV3Endpoints.SERVICE_CREDENTIAL_BINDINGS + CloudControllerV3Endpoints.QUERY_APP_GUIDS + applicationGuid
            + CloudControllerV3Endpoints.AMPERSAND_SERVICE_INSTANCE_GUIDS + serviceInstanceGuid
            + CloudControllerV3Endpoints.AMPERSAND_PER_PAGE
            + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE;

        return cc.list(uri, BINDING_LIST_TYPE)
                 .stream()
                 .map(binding -> UUID.fromString(binding.guid()))
                 .toList();
    }

    private List<String> doDeleteServiceBindings(List<UUID> guids) {
        List<String> jobIds = new ArrayList<>();
        List<CloudOperationException> errors = new ArrayList<>();

        for (UUID guid : guids) {
            try {
                deleteSingleServiceBinding(guid).ifPresent(jobIds::add);
            } catch (CloudOperationException e) {
                errors.add(e);
            }
        }

        throwOnErrors(errors);
        return jobIds;
    }

    private Optional<String> deleteSingleServiceBinding(UUID guid) {
        ResponseEntity<Void> response = cc.getRestClient()
                                          .delete()
                                          .uri(CloudControllerV3Endpoints.SERVICE_CREDENTIAL_BINDING_BY_GUID, guid)
                                          .retrieve()
                                          .toBodilessEntity();

        return extractJobGuidFromResponseHeader(response);
    }

    private void throwOnErrors(List<CloudOperationException> errors) {
        if (errors.isEmpty()) {
            return;
        }

        CloudOperationException firstException = errors.getFirst();
        errors.subList(1, errors.size())
              .forEach(firstException::addSuppressed);

        throw firstException;
    }

    private Optional<String> extractJobGuidFromResponseHeader(ResponseEntity<Void> response) {
        URI location = response.getHeaders()
                               .getLocation();

        if (location == null) {
            return Optional.empty();
        }

        String path = location.getPath();
        return Optional.of(path.substring(path.lastIndexOf('/') + 1));
    }

    private Map<String, Object> toOneRelationship(UUID guid) {
        return Map.of("data", Map.of("guid", guid.toString()));
    }

    private UUID getRequiredApplicationGuid(String applicationName) {
        StringBuilder query = new StringBuilder(
            CloudControllerV3Endpoints.APPS + CloudControllerV3Endpoints.QUERY_PER_PAGE + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE);

        if (target != null && target.getGuid() != null) {
            query.append(CloudControllerV3Endpoints.AMPERSAND_SPACE_GUIDS)
                 .append(target.getGuid());
        }

        query.append(CloudControllerV3Endpoints.AMPERSAND_NAMES)
             .append(applicationName);

        List<V3Application> apps = cc.list(query.toString(), APPLICATION_LIST_TYPE);

        if (apps.isEmpty()) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, Messages.NOT_FOUND,
                                              MessageFormat.format(Messages.APPLICATION_0_NOT_FOUND, applicationName));
        }

        return UUID.fromString(apps.getFirst()
                                   .guid());
    }

    private UUID getRequiredServiceInstanceGuid(String name) {
        StringBuilder query = new StringBuilder(CloudControllerV3Endpoints.SERVICE_INSTANCES + CloudControllerV3Endpoints.QUERY_PER_PAGE
                                                    + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE);

        if (target != null && target.getGuid() != null) {
            query.append(CloudControllerV3Endpoints.AMPERSAND_SPACE_GUIDS)
                 .append(target.getGuid());
        }

        query.append(CloudControllerV3Endpoints.AMPERSAND_NAMES)
             .append(name);
        List<V3ServiceInstanceRef> instances = cc.list(query.toString(),
                                                       new ParameterizedTypeReference<V3ListResponse<V3ServiceInstanceRef>>() {
                                                       });

        if (instances.isEmpty()) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, Messages.NOT_FOUND,
                                              MessageFormat.format(Messages.SERVICE_INSTANCE_0_NOT_FOUND, name));
        }

        return UUID.fromString(instances.getFirst()
                                        .guid());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record V3ServiceInstanceRef(@JsonProperty("guid") String guid) {
    }

}
