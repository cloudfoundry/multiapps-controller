package com.sap.cloud.lm.sl.cf.core.cf.clients;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.springframework.web.client.RestTemplate;

import com.sap.cloud.lm.sl.common.util.JsonUtil;

public class CFOptimizedEventGetter {

    private static final String FIND_EVENT_BY_TYPE_AND_TIMESTAMP_ENDPOINT = "/v2/events?inline-relations-depth=1&results-per-page=100&q=type:{type}&q=timestamp>{timestamp}";
    private RestTemplateFactory restTemplateFactory;
    private CloudFoundryClient cfClient;

    public CFOptimizedEventGetter(CloudFoundryClient cfClient) {
        this.cfClient = cfClient;
        this.restTemplateFactory = new RestTemplateFactory();
    }

    public List<String> findEvents(String type, String timestamp) {
        return new CustomControllerClientErrorHandler().handleErrorsOrReturnResult(() -> findEventsInternal(type, timestamp));
    }

    private List<String> findEventsInternal(String type, String timestamp) {
        String url = getUrlForEndpoint(cfClient, FIND_EVENT_BY_TYPE_AND_TIMESTAMP_ENDPOINT);
        Map<String, Object> urlVariables = getAsUrlVariablesForFindEventsRequest(type, timestamp);
        return executeFindEventRequest(url, urlVariables);
    }

    private String getUrlForEndpoint(CloudFoundryOperations client, String endpoint) {
        return client.getCloudControllerUrl() + endpoint;
    }

    private Map<String, Object> getAsUrlVariablesForFindEventsRequest(String type, String timestamp) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", type);
        result.put("timestamp", timestamp);
        return result;
    }

    private List<String> executeFindEventRequest(String url, Map<String, Object> urlVariables) {
        String response = getRestTemplate().getForObject(url, String.class, urlVariables);
        Map<String, Object> parsedResponse = parseResponse(response);
        return getSpaceIds(parsedResponse);
    }

    private List<String> getSpaceIds(Map<String, Object> parsedResponse) {
        List<String> result = new ArrayList<>();
        List<Map<String, Object>> resources = getResourcesFromResponse(parsedResponse);
        for (Map<String, Object> cloudEvent : resources) {
            String spaceId = getSpaceIdValue(cloudEvent);
            result.add(spaceId);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private String getSpaceIdValue(Map<String, Object> cloudEvent) {
        Map<String, Object> entity = (Map<String, Object>) cloudEvent.get("entity");
        String spaceId = (String) entity.get("space_guid");
        return  spaceId;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getResourcesFromResponse(Map<String, Object> parsedResponse) {
        List<Map<String, Object>> allResources = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> newResources = (List<Map<String, Object>>) parsedResponse.get("resources");
        if (newResources != null && newResources.size() > 0) {
            allResources.addAll(newResources);
        }
        try {
            addAllRemainingResources(parsedResponse, allResources);
        } catch (UnsupportedEncodingException e) {
            return allResources;
        }
        return allResources;
    }

    private void addAllRemainingResources(Map<String, Object> responseMap, List<Map<String, Object>> allResources)
        throws UnsupportedEncodingException {
        String nextUrl = (String) responseMap.get("next_url");
        while (nextUrl != null && nextUrl.length() > 0) {
            nextUrl = addPageOfResources(nextUrl, allResources);
        }
    }

    @SuppressWarnings("unchecked")
    private String addPageOfResources(String nextUrl, List<Map<String, Object>> allResources) throws UnsupportedEncodingException {
        String decodedURL = URLDecoder.decode(getUrl(nextUrl), "UTF-8");
        String resp = getRestTemplate().getForObject(decodedURL, String.class);
        Map<String, Object> respMap = JsonUtil.convertJsonToMap(resp);
        List<Map<String, Object>> newResources = (List<Map<String, Object>>) respMap.get("resources");
        if (newResources != null && newResources.size() > 0) {
            allResources.addAll(newResources);
        }
        return (String) respMap.get("next_url");
    }

    protected String getUrl(String path) {
        return cfClient.getCloudControllerUrl().toString() + (path.startsWith("/") ? path : "/" + path);
    }

    private Map<String, Object> parseResponse(String response) {
        return JsonUtil.convertJsonToMap(response);
    }

    private RestTemplate getRestTemplate() {
        return restTemplateFactory.getRestTemplate(cfClient);
    }
}
