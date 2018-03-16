package com.sap.cloud.lm.sl.cf.core.cf.clients;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.springframework.web.client.RestTemplate;

import com.sap.cloud.lm.sl.common.util.CommonUtil;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

public abstract class CustomControllerClient {

    private RestTemplateFactory restTemplateFactory;

    protected CustomControllerClient(RestTemplateFactory restTemplateFactory) {
        this.restTemplateFactory = restTemplateFactory;
    }

    protected RestTemplate getRestTemplate(CloudFoundryOperations client) {
        return restTemplateFactory.getRestTemplate(client);
    }

    protected List<Map<String, Object>> getAllResources(RestTemplate restTemplate, String controllerUrl, String urlPath) {
        return getAllResources(restTemplate, controllerUrl, urlPath, Collections.emptyMap());
    }

    protected List<Map<String, Object>> getAllResources(RestTemplate restTemplate, String controllerUrl, String urlPath,
        Map<String, Object> urlVariables) {
        List<Map<String, Object>> allResources = new ArrayList<Map<String, Object>>();
        String nextUrl = urlPath;
        while (!CommonUtil.isNullOrEmpty(nextUrl)) {
            nextUrl = addPageOfResources(restTemplate, controllerUrl, nextUrl, allResources, urlVariables);
        }
        return allResources;
    }

    @SuppressWarnings("unchecked")
    protected String addPageOfResources(RestTemplate restTemplate, String controllerUrl, String nextUrl,
        List<Map<String, Object>> allResources, Map<String, Object> urlVariables) {
        String response = restTemplate.getForObject(getUrl(controllerUrl, nextUrl), String.class, urlVariables);
        Map<String, Object> responseMap = JsonUtil.convertJsonToMap(response);
        validateResponse(responseMap);
        List<Map<String, Object>> newResources = (List<Map<String, Object>>) responseMap.get("resources");
        if (newResources != null && newResources.size() > 0) {
            allResources.addAll(newResources);
        }
        return (String) responseMap.get("next_url");
    }

    protected void validateResponse(Map<String, Object> response) {
    }

    protected String getUrl(String controllerUrl, String path) {
        return controllerUrl + (path.startsWith("/") ? path : "/" + path);
    }

}
