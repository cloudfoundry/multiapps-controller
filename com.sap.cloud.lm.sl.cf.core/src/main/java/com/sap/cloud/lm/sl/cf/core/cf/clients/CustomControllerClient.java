package com.sap.cloud.lm.sl.cf.core.cf.clients;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.springframework.web.client.RestTemplate;

import com.sap.cloud.lm.sl.common.util.CommonUtil;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

public abstract class CustomControllerClient {

    @Inject
    private RestTemplateFactory restTemplateFactory;

    protected RestTemplate getRestTemplate(CloudFoundryOperations client) {
        return restTemplateFactory.getRestTemplate(client);
    }

    protected List<Map<String, Object>> getAllResources(RestTemplate restTemplate, String controllerUrl, String urlPath) {
        List<Map<String, Object>> allResources = new ArrayList<Map<String, Object>>();
        String nextUrl = urlPath;
        while (!CommonUtil.isNullOrEmpty(nextUrl)) {
            nextUrl = addPageOfResources(restTemplate, controllerUrl, nextUrl, allResources);
        }
        return allResources;
    }

    @SuppressWarnings("unchecked")
    protected String addPageOfResources(RestTemplate restTemplate, String controllerUrl, String nextUrl,
        List<Map<String, Object>> allResources) {
        String response = restTemplate.getForObject(getUrl(controllerUrl, nextUrl), String.class);
        Map<String, Object> responseMap = JsonUtil.convertJsonToMap(response);
        List<Map<String, Object>> newResources = (List<Map<String, Object>>) responseMap.get("resources");
        if (newResources != null && newResources.size() > 0) {
            allResources.addAll(newResources);
        }
        return (String) responseMap.get("next_url");
    }

    protected String getUrl(String controllerUrl, String path) {
        return controllerUrl + (path.startsWith("/") ? path : "/" + path);
    }

}
