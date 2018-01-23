package com.sap.cloud.lm.sl.cf.core.cf.clients;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.http.client.utils.URIBuilder;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudOrganization;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.util.CloudEntityResourceMapper;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.MapUtil;

public class CFOptimizedSpaceGetter extends SpaceGetter {

    private static final String DEFAULT_URL_ENCODING = "UTF-8";
    private static final String SPACE_NAME_QUERY_PARAMETER = "name:{space_name}";
    private static final String ORGANIZATION_GUID_QUERY_PARAMETER = "organization_guid:{organization_guid}";
    private static final String QUERY_PARAMETER = "q";
    private static final String INLINE_RELATIONS_DEPTH_DEFAULT_VALUE = "1";
    private static final String INLINE_RELATIONS_DEPTH = "inline-relations-depth";
    private static final String GET_SPACE_ENDPOINT = "/v2/spaces/{id}?inline-relations-depth=1";
    private static final String GET_SPACES_ENDPOINT = "/v2/spaces";

    private CloudEntityResourceMapper resourceMapper = new CloudEntityResourceMapper();

    public CFOptimizedSpaceGetter() {
        this(new RestTemplateFactory());
    }

    protected CFOptimizedSpaceGetter(RestTemplateFactory restTemplateFactory) {
        this.restTemplateFactory = restTemplateFactory;
    }

    @Override
    public CloudSpace findSpace(CloudFoundryOperations client, String orgName, String spaceName) {
        List<Map<String, Object>> parsedSpacesResponse = new CustomControllerClientErrorHandler()
            .handleErrorsOrReturnResult(() -> attemptToFindSpace(client, orgName, spaceName));
        validateParsedResponse(parsedSpacesResponse);
        return toCloudSpace(parsedSpacesResponse);
    }

    @Override
    public List<CloudSpace> findSpaces(CloudFoundryOperations client, String orgName) {
        List<Map<String, Object>> parsedSpacesResponse = new CustomControllerClientErrorHandler()
            .handleErrorsOrReturnResult(() -> attemptToFindSpace(client, orgName, null));
        return toCloudSpaces(parsedSpacesResponse);
    }

    public CloudSpace findSpace(CloudFoundryOperations client, String spaceId) {
        return new CustomControllerClientErrorHandler().handleErrorsOrReturnResult(() -> attemptToGetSpace(client, spaceId));
    }

    private List<Map<String, Object>> attemptToFindSpace(CloudFoundryOperations client, String orgName, String spaceName) {
        RestTemplate restTemplate = restTemplateFactory.getRestTemplate(client);
        String url = buildEndpoint(orgName, spaceName);

        String orgGuid = getOrgGuid(client, orgName);

        Map<String, Object> urlVariables = getAsUrlVariablesForFindSpaceRequest(orgGuid, spaceName);
        return executeFindSpaceRequest(restTemplate, client.getCloudControllerUrl().toString(), url, urlVariables);
    }

    private String getOrgGuid(CloudFoundryOperations client, String orgName) {
        if (orgName == null) {
            return null;
        }

        CloudOrganization org = client.getOrganization(orgName, false);
        return org != null ? org.getMeta().getGuid().toString() : null;
    }

    private String buildEndpoint(String orgName, String spaceName) {
        try {
            URIBuilder builder = new URIBuilder();
            builder.setPath(GET_SPACES_ENDPOINT);
            builder.addParameter(INLINE_RELATIONS_DEPTH, INLINE_RELATIONS_DEPTH_DEFAULT_VALUE);
            if (orgName != null) {
                builder.addParameter(QUERY_PARAMETER, ORGANIZATION_GUID_QUERY_PARAMETER);
            }
            if (spaceName != null) {
                builder.addParameter(QUERY_PARAMETER, SPACE_NAME_QUERY_PARAMETER);
            }
            return URLDecoder.decode(builder.toString(), DEFAULT_URL_ENCODING);
        } catch (UnsupportedEncodingException e) {
            throw new SLException(e);
        }
    }

    private String getUrlForEndpoint(CloudFoundryOperations client, String endpoint) {
        return client.getCloudControllerUrl() + endpoint;
    }

    private Map<String, Object> getAsUrlVariablesForFindSpaceRequest(String orgGuid, String spaceName) {
        Map<String, Object> result = new HashMap<>();
        if (spaceName != null) {
            result.put("space_name", spaceName);
        }
        if (orgGuid != null) {
            result.put("organization_guid", orgGuid);
        }
        return result;
    }

    private List<Map<String, Object>> executeFindSpaceRequest(RestTemplate restTemplate, String controllerUrl, String url,
        Map<String, Object> urlVariables) {
        return getAllResources(restTemplate, controllerUrl, url, urlVariables);
    }

    private Map<String, Object> parseResponse(String response) {
        return JsonUtil.convertJsonToMap(response);
    }

    @Override
    protected void validateResponse(Map<String, Object> response) {
        List<Map<String, Object>> resources = getResourcesFromResponse(response);
        Assert.notNull(resources, "The response of finding a space by org and space names should contain a 'resources' element");
    }

    private void validateParsedResponse(List<Map<String, Object>> parsedSpacesResponse) {
        Assert.isTrue(parsedSpacesResponse.size() <= 1,
            "The response of finding a space by org and space names should not have more than one resource element");
        if (!parsedSpacesResponse.isEmpty()) {
            Assert.isNull(parsedSpacesResponse.get(0).get("next_url"),
                "The response of finding a space by org and space names should contain just one page");
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getResourcesFromResponse(Map<String, Object> parsedResponse) {
        return (List<Map<String, Object>>) parsedResponse.get("resources");
    }

    private CloudSpace toCloudSpace(List<Map<String, Object>> resources) {
        if (resources.isEmpty()) {
            return null;
        }
        return toCloudSpace(resources.get(0));
    }

    private CloudSpace toCloudSpace(Map<String, Object> resource) {
        return resourceMapper.mapResource(resource, CloudSpace.class);
    }

    private List<CloudSpace> toCloudSpaces(List<Map<String, Object>> resources) {
        if (resources.isEmpty()) {
            return Collections.emptyList();
        }

        return resources.stream().map(resource -> toCloudSpace(resource)).collect(Collectors.toList());
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
