package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.Constants;
import org.cloudfoundry.multiapps.controller.client.facade.Messages;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceInstance;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceKey;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Metadata;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ServiceInstanceType;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ListResponse;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServiceBinding;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServiceKeyDetails;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServiceKeyMapper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;

public class ServiceKeysV3Operations {

    private final CloudControllerV3Client cc;

    public ServiceKeysV3Operations(CloudControllerV3Client cc) {
        this.cc = cc;
    }

    public CloudServiceKey createAndFetchServiceKey(CloudServiceKey keyModel, CloudServiceInstance serviceInstance) {
        ResponseEntity<Void> acceptedResponse = postServiceKey(keyModel.getName(), keyModel.getCredentials(), keyModel.getV3Metadata(),
                                                               serviceInstance);

        cc.followAsyncJob(acceptedResponse, Constants.BINDING_OPERATIONS_TIMEOUT);

        V3ServiceBinding createdServiceKey = getServiceKeyResourceByNameAndServiceInstanceGuid(keyModel.getName(),
                                                                                               serviceInstance.getGuid());
        if (createdServiceKey == null) {
            return null;
        }

        Map<String, Object> credentials = getServiceKeyCredentials(createdServiceKey.guid());
        return V3ServiceKeyMapper.toCloudServiceKey(createdServiceKey, serviceInstance, credentials);
    }

    public Optional<String> createServiceKey(CloudServiceKey keyModel, CloudServiceInstance serviceInstance) {
        ResponseEntity<Void> acceptedResponse = postServiceKey(keyModel.getName(), keyModel.getCredentials(), keyModel.getV3Metadata(),
                                                               serviceInstance);

        return extractJobGuid(acceptedResponse);
    }

    public Optional<String> createServiceKey(CloudServiceInstance serviceInstance, String serviceKeyName, Map<String, Object> parameters) {
        ResponseEntity<Void> acceptedResponse = postServiceKey(serviceKeyName, parameters, null, serviceInstance);

        return extractJobGuid(acceptedResponse);
    }

    public CloudServiceKey getServiceKey(CloudServiceInstance serviceInstance, String serviceKeyName) {
        V3ServiceBinding serviceKey = getServiceKeyResourceByNameAndServiceInstanceGuid(serviceKeyName, serviceInstance.getGuid());
        if (serviceKey == null) {
            return null;
        }

        Map<String, Object> credentials = getServiceKeyCredentials(serviceKey.guid());
        return V3ServiceKeyMapper.toCloudServiceKey(serviceKey, serviceInstance, credentials);
    }

    public List<CloudServiceKey> getServiceKeys(CloudServiceInstance serviceInstance) {
        return listServiceKeyResources(serviceInstance.getGuid()).stream()
                                                                 .map(key -> V3ServiceKeyMapper.toCloudServiceKey(key, serviceInstance))
                                                                 .toList();
    }

    public List<CloudServiceKey> getServiceKeysWithCredentials(CloudServiceInstance serviceInstance) {
        return ReactiveFanOut.mapConcurrently(listServiceKeyResources(serviceInstance.getGuid()),
                                              key -> V3ServiceKeyMapper.toCloudServiceKey(key, serviceInstance,
                                                                                          getServiceKeyCredentials(key.guid())));
    }

    private ResponseEntity<Void> postServiceKey(String name, Map<String, Object> parameters, Metadata metadata,
                                                CloudServiceInstance serviceInstance) {
        if (serviceInstance.getType() != ServiceInstanceType.MANAGED) {
            throw new IllegalArgumentException(
                String.format(Messages.CANT_CREATE_SERVICE_KEY_FOR_USER_PROVIDED_SERVICE, serviceInstance.getName()));
        }

        return cc.getRestClient()
                 .post()
                 .uri(CloudControllerV3Endpoints.SERVICE_CREDENTIAL_BINDINGS)
                 .body(buildCreateServiceKeyBody(name, parameters, metadata, serviceInstance.getGuid()))
                 .retrieve()
                 .toBodilessEntity();
    }

    private Map<String, Object> buildCreateServiceKeyBody(String name, Map<String, Object> parameters, Metadata metadata,
                                                          UUID serviceInstanceGuid) {
        Map<String, Object> resultBody = new HashMap<>();
        resultBody.put("type", "key");
        resultBody.put("name", name);
        resultBody.put("relationships", Map.of("service_instance", Map.of("data", Map.of("guid", serviceInstanceGuid.toString()))));

        if (parameters != null && !parameters.isEmpty()) {
            resultBody.put("parameters", parameters);
        }

        Map<String, Object> metadataBody = buildMetadataBody(metadata);
        if (metadataBody != null) {
            resultBody.put("metadata", metadataBody);
        }

        return resultBody;
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
        String query = CloudControllerV3Endpoints.SERVICE_CREDENTIAL_BINDINGS + CloudControllerV3Endpoints.QUERY_PER_PAGE
            + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE + CloudControllerV3Endpoints.AMPERSAND_TYPE + "key"
            + CloudControllerV3Endpoints.AMPERSAND_SERVICE_INSTANCE_GUIDS + serviceInstanceGuid + CloudControllerV3Endpoints.AMPERSAND_NAMES
            + name;

        List<V3ServiceBinding> keys = cc.list(query, new ParameterizedTypeReference<V3ListResponse<V3ServiceBinding>>() {
        });

        return keys.isEmpty() ? null : keys.getFirst();
    }

    private List<V3ServiceBinding> listServiceKeyResources(UUID serviceInstanceGuid) {
        String query = CloudControllerV3Endpoints.SERVICE_CREDENTIAL_BINDINGS + CloudControllerV3Endpoints.QUERY_PER_PAGE
            + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE + CloudControllerV3Endpoints.AMPERSAND_TYPE + "key"
            + CloudControllerV3Endpoints.AMPERSAND_SERVICE_INSTANCE_GUIDS + serviceInstanceGuid;

        return cc.list(query, new ParameterizedTypeReference<V3ListResponse<V3ServiceBinding>>() {
        });
    }

    private Map<String, Object> getServiceKeyCredentials(String keyGuid) {
        Optional<V3ServiceKeyDetails> details = cc.getOptional(CloudControllerV3Endpoints.SERVICE_CREDENTIAL_BINDINGS + "/" + keyGuid
                                                                   + "/details",
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
        int index = href.lastIndexOf("/v3/jobs/");
        String jobGuid = index < 0 ? href.substring(href.lastIndexOf('/') + 1) : href.substring(index + "/v3/jobs/".length());

        return Optional.of(jobGuid);
    }

}
