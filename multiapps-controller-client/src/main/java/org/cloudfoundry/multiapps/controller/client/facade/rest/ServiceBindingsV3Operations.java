package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Metadata;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceBinding;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Application;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ListResponse;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServiceBinding;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServiceBindingMapper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;

/**
 * CF v3 <em>service credential bindings</em> (application bindings) operations of the in-house {@code CloudControllerRestClient}.
 * Reproduces the HTTP shape, filtering, async handling and domain mapping of the OSS {@code CloudControllerRestClientImpl} binding
 * methods on top of the shared {@link CloudControllerV3Client} machinery.
 *
 * <p>
 * Endpoint map (see {@code docs/cf-java-client-migration/02-endpoint-inventory.md}):
 * </p>
 * <ul>
 * <li>{@code bindServiceInstance} &rarr; {@code POST /v3/service_credential_bindings} (type=app, async job)</li>
 * <li>{@code unbindServiceInstance} / {@code deleteServiceBinding} &rarr; {@code DELETE /v3/service_credential_bindings/{guid}} (async
 * job)</li>
 * <li>{@code getServiceBinding} &rarr; {@code GET /v3/service_credential_bindings?guids={guid}} (single)</li>
 * <li>{@code getServiceAppBindings} &rarr; {@code GET /v3/service_credential_bindings?service_instance_guids=…&type=app} (paginated)</li>
 * <li>{@code getAppBindings} &rarr; {@code GET /v3/service_credential_bindings?app_guids=…} (paginated)</li>
 * <li>{@code getServiceBindingsForApplication} &rarr; {@code GET /v3/service_credential_bindings?app_guids=…&service_instance_guids=…}
 * (paginated)</li>
 * <li>{@code getServiceBindingParameters} &rarr; {@code GET /v3/service_credential_bindings/{guid}/parameters}</li>
 * <li>{@code updateServiceBindingMetadata} &rarr; {@code PATCH /v3/service_credential_bindings/{guid}}</li>
 * </ul>
 *
 * <p>
 * As in the OSS impl, the async create/delete operations do <em>not</em> block on the job: they return the job GUID (extracted from the
 * {@code Location} header of the 202 response) so the caller can poll it. This matches
 * {@code delegate.serviceBindingsV3().create(...).getJobId()} / {@code .delete(...).block()} which yield the job id without waiting.
 * </p>
 */
public class ServiceBindingsV3Operations {

    // CF v3 caps per_page at 5000; use a large page to minimise round-trips (the pagination walker still handles multiple pages).
    private static final int PER_PAGE = 5000;

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
                                          .uri("/v3/service_credential_bindings")
                                          .body(body)
                                          .retrieve()
                                          .toBodilessEntity();
        return extractJobGuid(response);
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
        String uri = "/v3/service_credential_bindings?guids=" + serviceBindingGuid + "&per_page=" + PER_PAGE;
        return cc.list(uri, BINDING_LIST_TYPE)
                 .stream()
                 .findFirst()
                 .map(V3ServiceBindingMapper::toCloudServiceBinding)
                 .orElse(null);
    }

    public List<CloudServiceBinding> getServiceAppBindings(UUID serviceInstanceGuid) {
        String uri = "/v3/service_credential_bindings?service_instance_guids=" + serviceInstanceGuid + "&type=app&per_page=" + PER_PAGE;
        return listBindings(uri);
    }

    public List<CloudServiceBinding> getAppBindings(UUID applicationGuid) {
        String uri = "/v3/service_credential_bindings?app_guids=" + applicationGuid + "&per_page=" + PER_PAGE;
        return listBindings(uri);
    }

    public List<CloudServiceBinding> getServiceBindingsForApplication(UUID applicationId, UUID serviceInstanceGuid) {
        String uri = "/v3/service_credential_bindings?app_guids=" + applicationId + "&service_instance_guids=" + serviceInstanceGuid
            + "&per_page=" + PER_PAGE;
        return listBindings(uri);
    }

    public Map<String, Object> getServiceBindingParameters(UUID guid) {
        @SuppressWarnings("unchecked")
        Map<String, Object> parameters = cc.get("/v3/service_credential_bindings/" + guid + "/parameters", Map.class);
        return parameters;
    }

    public void updateServiceBindingMetadata(UUID guid, Metadata metadata) {
        Map<String, Object> metadataBody = new HashMap<>();
        metadataBody.put("labels", metadata.getLabels());
        metadataBody.put("annotations", metadata.getAnnotations());
        cc.getRestClient()
          .patch()
          .uri("/v3/service_credential_bindings/{guid}", guid)
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

    // Mirrors the OSS doUnbindServiceInstance: resolve the app<->service bindings and 404 when none exist, then delete each.
    private List<String> doUnbindServiceInstance(UUID applicationGuid, UUID serviceInstanceGuid) {
        List<UUID> serviceBindingGuids = getServiceBindingGuids(applicationGuid, serviceInstanceGuid);
        if (serviceBindingGuids.isEmpty()) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found",
                                              "Service binding between service with GUID " + serviceInstanceGuid
                                                  + " and application with GUID " + applicationGuid + " not found.");
        }
        return doDeleteServiceBindings(serviceBindingGuids);
    }

    private List<UUID> getServiceBindingGuids(UUID applicationGuid, UUID serviceInstanceGuid) {
        String uri = "/v3/service_credential_bindings?app_guids=" + applicationGuid + "&service_instance_guids=" + serviceInstanceGuid
            + "&per_page=" + PER_PAGE;
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
                                          .uri("/v3/service_credential_bindings/{guid}", guid)
                                          .retrieve()
                                          .toBodilessEntity();
        return extractJobGuid(response);
    }

    private void throwOnErrors(List<CloudOperationException> errors) {
        if (errors.isEmpty()) {
            return;
        }
        CloudOperationException first = errors.get(0);
        errors.subList(1, errors.size())
              .forEach(first::addSuppressed);
        throw first;
    }

    // Extract the job GUID from the Location header of a 202-Accepted response (path ends in /v3/jobs/{guid}); empty if synchronous.
    private Optional<String> extractJobGuid(ResponseEntity<Void> response) {
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

    // Mirrors the OSS getRequiredApplicationGuid: look up the app in the target space and 404 if it is absent.
    private UUID getRequiredApplicationGuid(String applicationName) {
        StringBuilder query = new StringBuilder("/v3/apps?per_page=" + PER_PAGE);
        if (target != null && target.getGuid() != null) {
            query.append("&space_guids=")
                 .append(target.getGuid());
        }
        query.append("&names=")
             .append(applicationName);
        List<V3Application> apps = cc.list(query.toString(), APPLICATION_LIST_TYPE);
        if (apps.isEmpty()) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Application " + applicationName + " not found.");
        }
        return UUID.fromString(apps.get(0)
                                   .guid());
    }

    // Mirrors the OSS getRequiredServiceInstanceGuid: look up the service instance in the target space and 404 if it is absent.
    private UUID getRequiredServiceInstanceGuid(String name) {
        StringBuilder query = new StringBuilder("/v3/service_instances?per_page=" + PER_PAGE);
        if (target != null && target.getGuid() != null) {
            query.append("&space_guids=")
                 .append(target.getGuid());
        }
        query.append("&names=")
             .append(name);
        List<V3ServiceInstanceRef> instances = cc.list(query.toString(),
                                                       new ParameterizedTypeReference<V3ListResponse<V3ServiceInstanceRef>>() {
                                                       });
        if (instances.isEmpty()) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Service instance " + name + " not found.");
        }
        return UUID.fromString(instances.get(0)
                                        .guid());
    }

    // Minimal wire model to resolve a service instance GUID by name without depending on the (separately-owned) ServiceInstances group.
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    private record V3ServiceInstanceRef(@com.fasterxml.jackson.annotation.JsonProperty("guid") String guid) {
    }

}
