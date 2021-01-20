package org.cloudfoundry.multiapps.controller.core.cf.clients;

import java.util.Map;

import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.springframework.web.reactive.function.client.WebClient;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;

public class AuthorizationEndpointGetter {

    private final WebClient webClient;

    public AuthorizationEndpointGetter(CloudControllerClient client) {
        this.webClient = new WebClientFactory().getWebClient(client);
    }

    @SuppressWarnings("unchecked")
    public String getAuthorizationEndpoint() {
        String response = webClient.get()
                                   .uri("/")
                                   .retrieve()
                                   .bodyToMono(String.class)
                                   .block();
        Map<String, Object> resource = JsonUtil.convertJsonToMap(response);
        Map<String, Object> links = (Map<String, Object>) resource.get("links");
        Map<String, Object> login = (Map<String, Object>) links.get("login");
        return (String) login.get("href");
    }

}
