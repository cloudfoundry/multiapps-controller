package org.cloudfoundry.multiapps.controller.core.cf.clients;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.springframework.web.reactive.function.client.WebClient;

public abstract class CustomControllerClient {

    protected final CloudControllerClient client;
    private final WebClient webClient;

    protected CustomControllerClient(CloudControllerClient client) {
        this.client = client;
        this.webClient = new WebClientFactory().getWebClient(client);
    }

    protected List<Map<String, Object>> getAllResources(String path, Object... urlVariables) {
        List<Map<String, Object>> allResources = new ArrayList<>();
        String nextUrl = path;
        while (!StringUtils.isEmpty(nextUrl)) {
            nextUrl = addPageOfResources(nextUrl, allResources, urlVariables);
        }
        return allResources;
    }

    private String addPageOfResources(String path, List<Map<String, Object>> allResources, Object... urlVariables) {
        String response = webClient.get()
                                   .uri(path, urlVariables)
                                   .retrieve()
                                   .bodyToMono(String.class)
                                   .block();
        Map<String, Object> responseMap = JsonUtil.convertJsonToMap(response);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> newResources = (List<Map<String, Object>>) responseMap.get("resources");

        if (!CollectionUtils.isEmpty(newResources)) {
            allResources.addAll(newResources);
        }

        String nextUrl = (String) responseMap.get("next_url");
        return nextUrl == null ? null : URLDecoder.decode(nextUrl, StandardCharsets.UTF_8);
    }
}
