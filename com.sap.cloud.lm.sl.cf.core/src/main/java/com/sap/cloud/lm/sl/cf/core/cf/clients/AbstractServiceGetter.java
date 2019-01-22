package com.sap.cloud.lm.sl.cf.core.cf.clients;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.springframework.util.Assert;

import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

public abstract class AbstractServiceGetter extends CustomControllerClient {

    private static final String V2_USER_PROVIDED_SERVICE_INSTANCES_RESOURCE_PATH = "/v2/user_provided_service_instances?";
    private static final String V2_SERVICE_INSTANCES_RESOURCE_PATH = "/v2/service_instances?";

    @Inject
    public AbstractServiceGetter(RestTemplateFactory restTemplateFactory) {
        super(restTemplateFactory);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getServiceInstanceEntity(CloudControllerClient client, String serviceName, String spaceId) {
        Map<String, Object> serviceInstance = new CustomControllerClientErrorHandler()
            .handleErrorsOrReturnResult(() -> attemptToGetServiceInstance(client, serviceName, spaceId));
        return serviceInstance != null ? (Map<String, Object>) serviceInstance.get(getEntityName()) : Collections.emptyMap();
    }

    public Map<String, Object> getServiceInstance(CloudControllerClient client, String serviceName, String spaceId) {
        return new CustomControllerClientErrorHandler()
            .handleErrorsOrReturnResult(() -> attemptToGetServiceInstance(client, serviceName, spaceId));
    }

    private Map<String, Object> attemptToGetServiceInstance(CloudControllerClient client, String serviceName, String spaceId) {
        Map<String, Object> queryParameters = buildQueryParameters(serviceName, spaceId);
        String serviceInstancesEndpoint = getUrl(client.getCloudControllerUrl()
            .toString(), getServiceInstanceURL(queryParameters.keySet()));

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
        if (CollectionUtils.isNotEmpty(cloudServiceInstanceResources)) {
            return cloudServiceInstanceResources.get(0);
        }
        return null;
    }

    private void validateServiceInstanceResponse(Map<String, Object> serviceInstancesResponse) {
        List<Map<String, Object>> resources = getResourcesFromResponse(serviceInstancesResponse);
        Assert.notNull(serviceInstancesResponse.containsKey(getResourcesName()), MessageFormat.format(Messages.ERROR_SERVICE_INSTANCE_RESPONSE_WITH_MISSING_FIELD, getResourcesName()));
        Assert.isTrue(resources == null || resources.size() <= 1, Messages.ERROR_SERVICE_INSTANCE_RESPONSE_WITH_MORE_THEN_ONE_RESULT);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getResourcesFromResponse(Map<String, Object> serviceInstancesResponse) {
        return (List<Map<String, Object>>) serviceInstancesResponse.get(getResourcesName());
    }

    protected String getUserProvidedServiceInstanceResourcePath() {
        return V2_USER_PROVIDED_SERVICE_INSTANCES_RESOURCE_PATH;
    }

    protected String getServiceInstanceResourcePath() {
        return V2_SERVICE_INSTANCES_RESOURCE_PATH;
    }

    protected abstract String getServiceInstanceURL(Set<String> fields);

    protected abstract String getResourcesName();

    protected abstract String getEntityName();
}
