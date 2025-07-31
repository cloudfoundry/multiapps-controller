package org.cloudfoundry.multiapps.controller.client.facade.util;

import java.util.Map;

import org.springframework.web.reactive.function.client.WebClient;

public class AuthorizationEndpointGetter {

    private final WebClient webClient;

    public AuthorizationEndpointGetter(WebClient webClient) {
        this.webClient = webClient;
    }

    public String getAuthorizationEndpoint() {
        return getAuthorizationEndpoint("");
    }

    public String getAuthorizationEndpoint(String controllerUrl) {
        String response = webClient.get()
                                   .uri(controllerUrl + "/")
                                   .retrieve()
                                   .bodyToMono(String.class)
                                   .block();
        return getLoginHref(response);
    }

    @SuppressWarnings("unchecked")
    private String getLoginHref(String response) {
        Map<String, Object> resource = JsonUtil.convertJsonToMap(response);
        Map<String, Object> links = (Map<String, Object>) resource.get("links");
        Map<String, Object> login = (Map<String, Object>) links.get("login");
        return (String) login.get("href");
    }

}
