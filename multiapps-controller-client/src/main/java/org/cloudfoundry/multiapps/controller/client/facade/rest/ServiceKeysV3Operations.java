package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.facade.domain.Metadata;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ServiceInstanceType;
import org.cloudfoundry.multiapps.controller.client.facade.Messages;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceInstance;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceKey;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ListResponse;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServiceBinding;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServiceKeyDetails;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServiceKeyMapper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;

/**
 * CF v3 <em>service key</em> operations for the cf-java-client replacement. A service key is a service credential binding of
 * {@code type=key}, so all endpoints target {@code /v3/service_credential_bindings}. Reproduces the HTTP shape and domain-mapping of the
 * OSS {@code CloudControllerRestClientImpl} service-key methods:
 * <ul>
 * <li>create &rarr; {@code POST /v3/service_credential_bindings} (type=key) &mdash; returns a job (202);</li>
 * <li>list &rarr; {@code GET /v3/service_credential_bindings?type=key&service_instance_guids={guid}[&names={name}]} (paginated);</li>
 * <li>credentials &rarr; {@code GET /v3/service_credential_bindings/{guid}/details}.</li>
 * </ul>
 *
 * <p>
 * The interface methods that accept a {@code serviceInstanceName} require the owning {@link CloudServiceInstance} (for its GUID and to
 * embed in the resulting {@link CloudServiceKey}). Mirroring the OSS impl &mdash; which first calls {@code getServiceInstance(name)}
 * &mdash; these operations take the already-resolved {@link CloudServiceInstance}; the integrator resolves it in
 * {@code CloudControllerRestClientV3Impl} before delegating.
 * </p>
 */
public class ServiceKeysV3Operations {

    // CF v3 caps per_page at 5000; use a large page to minimise round-trips (the pagination walker still handles multiple pages).
    private static final int DEFAULT_PAGE_SIZE = 5000;
    // Mirrors the OSS BINDING_OPERATIONS_TIMEOUT used when synchronously creating (and fetching) a service key.
    private static final Duration BINDING_OPERATIONS_TIMEOUT = Duration.ofMinutes(5);

    private final CloudControllerV3Client cc;
    private final CloudSpace target;

    public ServiceKeysV3Operations(CloudControllerV3Client cc, CloudSpace target) {
        this.cc = cc;
        this.target = target;
    }

    // --- create -----------------------------------------------------------------------------------------------------------------

    public CloudServiceKey createAndFetchServiceKey(CloudServiceKey keyModel, CloudServiceInstance serviceInstance) {
        ResponseEntity<Void> accepted = postServiceKey(keyModel.getName(), keyModel.getCredentials(), keyModel.getV3Metadata(),
                                                       serviceInstance);
        cc.followAsyncJob(accepted, BINDING_OPERATIONS_TIMEOUT);
        V3ServiceBinding created = getServiceKeyResourceByNameAndServiceInstanceGuid(keyModel.getName(), serviceInstance.getGuid());
        if (created == null) {
            return null;
        }
        Map<String, Object> credentials = getServiceKeyCredentials(created.guid());
        return V3ServiceKeyMapper.toCloudServiceKey(created, serviceInstance, credentials);
    }

    public Optional<String> createServiceKey(CloudServiceKey keyModel, CloudServiceInstance serviceInstance) {
        ResponseEntity<Void> accepted = postServiceKey(keyModel.getName(), keyModel.getCredentials(), keyModel.getV3Metadata(),
                                                       serviceInstance);
        return extractJobGuid(accepted);
    }

    public Optional<String> createServiceKey(CloudServiceInstance serviceInstance, String serviceKeyName, Map<String, Object> parameters) {
        ResponseEntity<Void> accepted = postServiceKey(serviceKeyName, parameters, null, serviceInstance);
        return extractJobGuid(accepted);
    }

    // --- read -------------------------------------------------------------------------------------------------------------------

    public CloudServiceKey getServiceKey(CloudServiceInstance serviceInstance, String serviceKeyName) {
        V3ServiceBinding key = getServiceKeyResourceByNameAndServiceInstanceGuid(serviceKeyName, serviceInstance.getGuid());
        if (key == null) {
            return null;
        }
        Map<String, Object> credentials = getServiceKeyCredentials(key.guid());
        return V3ServiceKeyMapper.toCloudServiceKey(key, serviceInstance, credentials);
    }

    public List<CloudServiceKey> getServiceKeys(CloudServiceInstance serviceInstance) {
        return listServiceKeyResources(serviceInstance.getGuid()).stream()
                                                                 .map(key -> V3ServiceKeyMapper.toCloudServiceKey(key, serviceInstance))
                                                                 .toList();
    }

    public List<CloudServiceKey> getServiceKeysWithCredentials(CloudServiceInstance serviceInstance) {
        // Each key needs a separate GET /details for its credentials. The OSS client fetched these concurrently (reactive flatMap); do the
        // same here so a service with many keys doesn't serialize N credential round-trips.
        return ReactiveFanOut.mapConcurrently(listServiceKeyResources(serviceInstance.getGuid()),
                                              key -> V3ServiceKeyMapper.toCloudServiceKey(key, serviceInstance,
                                                                                          getServiceKeyCredentials(key.guid())));
    }

    // --- helpers ----------------------------------------------------------------------------------------------------------------

    private ResponseEntity<Void> postServiceKey(String name, Map<String, Object> parameters, Metadata metadata,
                                                CloudServiceInstance serviceInstance) {
        if (serviceInstance.getType() != ServiceInstanceType.MANAGED) {
            throw new IllegalArgumentException(
                String.format(Messages.CANT_CREATE_SERVICE_KEY_FOR_USER_PROVIDED_SERVICE, serviceInstance.getName()));
        }
        return cc.getRestClient()
                 .post()
                 .uri("/v3/service_credential_bindings")
                 .body(buildCreateServiceKeyBody(name, parameters, metadata, serviceInstance.getGuid()))
                 .retrieve()
                 .toBodilessEntity();
    }

    private Map<String, Object> buildCreateServiceKeyBody(String name, Map<String, Object> parameters, Metadata metadata,
                                                          UUID serviceInstanceGuid) {
        Map<String, Object> body = new HashMap<>();
        body.put("type", "key");
        body.put("name", name);
        body.put("relationships", Map.of("service_instance", Map.of("data", Map.of("guid", serviceInstanceGuid.toString()))));
        if (parameters != null && !parameters.isEmpty()) {
            body.put("parameters", parameters);
        }
        Map<String, Object> metadataBody = buildMetadataBody(metadata);
        if (metadataBody != null) {
            body.put("metadata", metadataBody);
        }
        return body;
    }

    private Map<String, Object> buildMetadataBody(Metadata metadata) {
        if (metadata == null) {
            return null;
        }
        Map<String, Object> metadataBody = new HashMap<>();
        if (metadata.getLabels() != null) {
            metadataBody.put("labels", metadata.getLabels());
        }
        if (metadata.getAnnotations() != null) {
            metadataBody.put("annotations", metadata.getAnnotations());
        }
        return metadataBody.isEmpty() ? null : metadataBody;
    }

    private V3ServiceBinding getServiceKeyResourceByNameAndServiceInstanceGuid(String name, UUID serviceInstanceGuid) {
        String query = "/v3/service_credential_bindings?per_page=" + DEFAULT_PAGE_SIZE + "&type=key&service_instance_guids="
            + serviceInstanceGuid + "&names=" + name;
        List<V3ServiceBinding> keys = cc.list(query, new ParameterizedTypeReference<V3ListResponse<V3ServiceBinding>>() {
        });
        return keys.isEmpty() ? null : keys.get(0);
    }

    private List<V3ServiceBinding> listServiceKeyResources(UUID serviceInstanceGuid) {
        String query = "/v3/service_credential_bindings?per_page=" + DEFAULT_PAGE_SIZE + "&type=key&service_instance_guids="
            + serviceInstanceGuid;
        return cc.list(query, new ParameterizedTypeReference<V3ListResponse<V3ServiceBinding>>() {
        });
    }

    // CF v3 returns 404 when fetching the details of a service key whose creation failed; treat that as empty credentials, matching the OSS impl.
    private Map<String, Object> getServiceKeyCredentials(String keyGuid) {
        Optional<V3ServiceKeyDetails> details = cc.getOptional("/v3/service_credential_bindings/" + keyGuid + "/details",
                                                               V3ServiceKeyDetails.class);
        return details.map(V3ServiceKeyDetails::credentials)
                      .orElse(Collections.emptyMap());
    }

    private Optional<String> extractJobGuid(ResponseEntity<Void> accepted) {
        URI location = accepted.getHeaders()
                               .getLocation();
        if (location == null) {
            return Optional.empty();
        }
        String href = location.toString();
        int idx = href.lastIndexOf("/v3/jobs/");
        String jobGuid = idx < 0 ? href.substring(href.lastIndexOf('/') + 1) : href.substring(idx + "/v3/jobs/".length());
        return Optional.of(jobGuid);
    }

}
