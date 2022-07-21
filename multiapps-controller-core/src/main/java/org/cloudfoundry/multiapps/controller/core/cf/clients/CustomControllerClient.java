package org.cloudfoundry.multiapps.controller.core.cf.clients;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.springframework.web.reactive.function.client.WebClient;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;

public abstract class CustomControllerClient {

    protected final CloudControllerClient client;
    private final WebClient webClient;

    protected CustomControllerClient(CloudControllerClient client) {
        this.client = client;
        this.webClient = new WebClientFactory().getWebClient(client);
    }

    protected List<Map<String, Object>> getAllResources(String path) {
        List<Map<String, Object>> allResources = new ArrayList<>();
        String nextUrl = path;
        while (!StringUtils.isEmpty(nextUrl)) {
            nextUrl = addPageOfResources(nextUrl, allResources, null);
        }
        return allResources;
    }

    protected CloudResourcesWithIncluded getAllResourcesWithIncluded(String path) {
        List<Map<String, Object>> allResources = new ArrayList<>();
        Map<String, Object> includedResources = new HashMap<>();
        String nextUrl = path;
        while (!StringUtils.isEmpty(nextUrl)) {
            nextUrl = addPageOfResources(nextUrl, allResources, includedResources);
        }
        return CloudResourcesWithIncluded.of(allResources, includedResources);
    }

    private String addPageOfResources(String path, List<Map<String, Object>> allResources, Map<String, Object> includedResources) {
        String response = webClient.get()
                                   .uri(path)
                                   .retrieve()
                                   .bodyToMono(String.class)
                                   .block();
        Map<String, Object> responseMap = JsonUtil.convertJsonToMap(response);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> newResources = (List<Map<String, Object>>) responseMap.get("resources");

        if (!CollectionUtils.isEmpty(newResources)) {
            allResources.addAll(newResources);
        }
        if (responseMap.containsKey("included") && includedResources != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> included = (Map<String, Object>) responseMap.get("included");
            includedResources.putAll(included);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> pagination = (Map<String, Object>) responseMap.get("pagination");
        @SuppressWarnings("unchecked")
        Map<String, Object> next = (Map<String, Object>) pagination.get("next");
        return next == null ? null : URLDecoder.decode((String) next.get("href"), StandardCharsets.UTF_8);
    }
}
