package com.sap.cloud.lm.sl.cf.core.cf.clients;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudOrganization;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.util.CloudEntityResourceMapper;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.MapUtil;

public class CFOptimizedSpaceGetter extends SpaceGetter {

    private static final String FIND_SPACE_ENDPOINT = "/v2/spaces?inline-relations-depth=1&q=name:{name}&q=organization_guid:{organization_guid}";
    private static final String GET_SPACE_ENDPOINT = "/v2/spaces/{id}?inline-relations-depth=1";

    private RestTemplateFactory restTemplateFactory;
    private CloudEntityResourceMapper resourceMapper = new CloudEntityResourceMapper();

    public CFOptimizedSpaceGetter() {
        this(new RestTemplateFactory());
    }

    protected CFOptimizedSpaceGetter(RestTemplateFactory restTemplateFactory) {
        this.restTemplateFactory = restTemplateFactory;
    }

    @Override
    public CloudSpace findSpace(CloudFoundryOperations client, String orgName, String spaceName) {
        return new CustomControllerClientErrorHandler().handleErrorsOrReturnResult(() -> attemptToFindSpace(client, orgName, spaceName));
    }

    private CloudSpace attemptToFindSpace(CloudFoundryOperations client, String orgName, String spaceName) {
        RestTemplate restTemplate = restTemplateFactory.getRestTemplate(client);
        String url = getUrlForEndpoint(client, FIND_SPACE_ENDPOINT);

        CloudOrganization org = client.getOrganization(orgName, false);
        if (org == null) {
            return null;
        }
        String orgGuid = org.getMeta().getGuid().toString();
        Map<String, Object> urlVariables = getAsUrlVariablesForFindSpaceRequest(orgGuid, spaceName);
        return executeFindSpaceRequest(restTemplate, url, urlVariables);
    }

    private String getUrlForEndpoint(CloudFoundryOperations client, String endpoint) {
        return client.getCloudControllerUrl() + endpoint;
    }

    private Map<String, Object> getAsUrlVariablesForFindSpaceRequest(String orgGuid, String spaceName) {
        Map<String, Object> result = new HashMap<>();
        result.put("name", spaceName);
        result.put("organization_guid", orgGuid);
        return result;
    }

    private CloudSpace executeFindSpaceRequest(RestTemplate restTemplate, String url, Map<String, Object> urlVariables) {
        String response = restTemplate.getForObject(url, String.class, urlVariables);
        Map<String, Object> parsedResponse = parseResponse(response);
        validateResponse(parsedResponse);
        return toCloudSpace(parsedResponse);
    }

    private Map<String, Object> parseResponse(String response) {
        return JsonUtil.convertJsonToMap(response);
    }

    private void validateResponse(Map<String, Object> parsedResponse) {
        Assert.isNull(parsedResponse.get("next_url"),
            "The response of finding a space by org and space names should contain just one page");
        List<Map<String, Object>> resources = getResourcesFromResponse(parsedResponse);
        Assert.notNull(resources, "The response of finding a space by org and space names should contain a 'resources' element");
        Assert.isTrue(resources.size() <= 1,
            "The response of finding a space by org and space names should not have more than one resource element");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getResourcesFromResponse(Map<String, Object> parsedResponse) {
        return (List<Map<String, Object>>) parsedResponse.get("resources");
    }

    private CloudSpace toCloudSpace(Map<String, Object> parsedResponse) {
        List<Map<String, Object>> resources = getResourcesFromResponse(parsedResponse);
        if (!resources.isEmpty()) {
            return resourceMapper.mapResource(resources.get(0), CloudSpace.class);
        }
        return null;
    }

    @Override
    public CloudSpace getSpace(CloudFoundryOperations client, String spaceId) {
        return new CustomControllerClientErrorHandler().handleErrorsOrReturnResult(() -> attemptToGetSpace(client, spaceId));
    }

    private CloudSpace attemptToGetSpace(CloudFoundryOperations client, String spaceId) {
        RestTemplate restTemplate = restTemplateFactory.getRestTemplate(client);
        String url = getUrlForEndpoint(client, GET_SPACE_ENDPOINT);
        Map<String, Object> urlVariables = MapUtil.asMap("id", spaceId);
        return executeGetSpaceRequest(restTemplate, url, urlVariables);
    }

    private CloudSpace executeGetSpaceRequest(RestTemplate restTemplate, String url, Map<String, Object> urlVariables) {
        String response = restTemplate.getForObject(url, String.class, urlVariables);
        Map<String, Object> parsedResponse = parseResponse(response);
        return resourceMapper.mapResource(parsedResponse, CloudSpace.class);
    }

}
