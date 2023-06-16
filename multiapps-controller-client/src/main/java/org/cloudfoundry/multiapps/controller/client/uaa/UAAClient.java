package org.cloudfoundry.multiapps.controller.client.uaa;

import java.net.URL;
import java.text.MessageFormat;
import java.util.Map;

import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.springframework.web.reactive.function.client.WebClient;

public class UAAClient {

    private static final String TOKEN_KEY_ENDPOINT = "/token_key";

    protected final URL uaaUrl;
    protected final WebClient webClient;

    public UAAClient(URL uaaUrl, WebClient webClient) {
        this.uaaUrl = uaaUrl;
        this.webClient = webClient;
    }

    public Map<String, Object> readTokenKey() {
        String tokenKeyURL = uaaUrl.toString() + TOKEN_KEY_ENDPOINT;
        String tokenKeyResponse = webClient.get()
                                           .uri(tokenKeyURL)
                                           .retrieve()
                                           .bodyToMono(String.class)
                                           .block();
        if (tokenKeyResponse == null) {
            throw new IllegalStateException(MessageFormat.format("Invalid response returned from {0}", tokenKeyURL));
        }
        return JsonUtil.convertJsonToMap(tokenKeyResponse);
    }

    public URL getUaaUrl() {
        return this.uaaUrl;
    }

}
