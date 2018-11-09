package com.sap.cloud.lm.sl.cf.core.cf.clients;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.springframework.util.Assert;

import com.sap.cloud.lm.sl.common.util.JsonUtil;

public abstract class AbstractServiceGetter extends CustomControllerClient {

    @Inject
    public AbstractServiceGetter(RestTemplateFactory restTemplateFactory) {
        super(restTemplateFactory);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getServiceInstanceEntity(CloudControllerClient client, String serviceName, String spaceId) {
        Map<String, Object> serviceInstance = new CustomControllerClientErrorHandler()
            .handleErrorsOrReturnResult(() -> attemptToGetServiceInstance(client, serviceName, spaceId));
        return serviceInstance != null ? (Map<String, Object>) serviceInstance.get("entity") : Collections.emptyMap();
    }

    public Map<String, Object> getServiceInstance(CloudControllerClient client, String serviceName, String spaceId) {
        return new CustomControllerClientErrorHandler()
            .handleErrorsOrReturnResult(() -> attemptToGetServiceInstance(client, serviceName, spaceId));
    }

    private Map<String, Object> attemptToGetServiceInstance(CloudControllerClient client, String serviceName, String spaceId) {
        String serviceInstancesEndpoint = getUrl(client.getCloudControllerUrl()
            .toString(), getServiceInstanceURL());
        Map<String, Object> queryParameters = buildQueryParameters(serviceName, spaceId);

        return getCloudServiceInstance(client, serviceInstancesEndpoint, queryParameters);
    }

    private Map<String, Object> buildQueryParameters(String serviceName, String spaceId) {
        Map<String, Object> queryVariables = new HashMap<>();
        queryVariables.put("name", serviceName);
        queryVariables.put("space_guid", spaceId);
        return queryVariables;
    }

    private Map<String, Object> getCloudServiceInstance(CloudControllerClient client, String serviceInstancesEndpoint,
        Map<String, Object> queryParameters) {
        String response = getRestTemplate(client).getForObject(serviceInstancesEndpoint, String.class, queryParameters);
        Map<String, Object> serviceInstancesResponse = parseResponse(response);
        return getCloudServiceInstance(serviceInstancesResponse);
    }

    private Map<String, Object> parseResponse(String response) {
        return JsonUtil.convertJsonToMap(response);
    }

    private Map<String, Object> getCloudServiceInstance(Map<String, Object> serviceInstancesResponse) {
        validateServiceInstanceResponse(serviceInstancesResponse);
        List<Map<String, Object>> cloudServiceInstanceResources = getResourcesFromResponse(serviceInstancesResponse);
        if (!cloudServiceInstanceResources.isEmpty()) {
            return cloudServiceInstanceResources.get(0);
        }
        return null;
    }

    private void validateServiceInstanceResponse(Map<String, Object> serviceInstancesResponse) {
        List<Map<String, Object>> resources = getResourcesFromResponse(serviceInstancesResponse);
        Assert.notNull(resources, "The response of finding a service instance should contain a 'resources' element");
        Assert.isTrue(resources.size() <= 1, "The response of finding a service instance should not have more than one resource element");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getResourcesFromResponse(Map<String, Object> serviceInstancesResponse) {
        return (List<Map<String, Object>>) serviceInstancesResponse.get("resources");
    }

    public abstract String getServiceInstanceURL();
}
