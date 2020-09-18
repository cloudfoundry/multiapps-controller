package org.cloudfoundry.multiapps.controller.core.cf.clients;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.springframework.web.reactive.function.client.WebClient;

public abstract class CustomControllerClient {

    private final WebClientFactory webClientFactory;

    protected CustomControllerClient(WebClientFactory webClientFactory) {
        this.webClientFactory = webClientFactory;
    }

    protected WebClient getWebClient(CloudControllerClient client) {
        return webClientFactory.getWebClient(client);
    }

    protected List<Map<String, Object>> getAllResources(WebClient webClient, String controllerUrl, String urlPath) {
        return getAllResources(webClient, controllerUrl, urlPath, Collections.emptyMap());
    }

    protected List<Map<String, Object>> getAllResources(WebClient webClient, String controllerUrl, String urlPath,
                                                        Map<String, Object> urlVariables) {
        List<Map<String, Object>> allResources = new ArrayList<>();
        String nextUrl = urlPath;
        while (!StringUtils.isEmpty(nextUrl)) {
            nextUrl = addPageOfResources(webClient, controllerUrl, nextUrl, allResources, urlVariables);
        }
        return allResources;
    }

    @SuppressWarnings("unchecked")
    protected String addPageOfResources(WebClient webClient, String controllerUrl, String path, List<Map<String, Object>> allResources,
                                        Map<String, Object> urlVariables) {
        String response = webClient.get()
                                   .uri(getUrl(controllerUrl, path), urlVariables)
                                   .retrieve()
                                   .bodyToMono(String.class)
                                   .block();
        Map<String, Object> responseMap = JsonUtil.convertJsonToMap(response);
        validateResponse(responseMap);
        List<Map<String, Object>> newResources = (List<Map<String, Object>>) responseMap.get("resources");
        if (!CollectionUtils.isEmpty(newResources)) {
            allResources.addAll(newResources);
        }
        String nextUrl = (String) responseMap.get("next_url");
        return nextUrl == null ? null : decode(nextUrl);
    }

    private String decode(String url) {
        try {
            return URLDecoder.decode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    protected void validateResponse(Map<String, Object> response) {
    }

    protected String getUrl(String controllerUrl, String path) {
        return controllerUrl + (path.startsWith("/") ? path : "/" + path);
    }

}
