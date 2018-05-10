package com.sap.cloud.lm.sl.cf.client.uaa;

import java.net.URL;
import java.text.MessageFormat;
import java.util.Map;

import org.cloudfoundry.client.lib.util.JsonUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class UAAClient {

    private static final String TOKEN_KEY_ENDPOINT = "/token_key";

    protected URL uaaUrl;
    protected RestTemplate restTemplate;

    protected UAAClient(URL uaaUrl, RestTemplate restTemplate) {
        this.uaaUrl = uaaUrl;
        this.restTemplate = restTemplate;
    }

    public Map<String, Object> readTokenKey() {
        String tokenKeyURL = uaaUrl.toString() + TOKEN_KEY_ENDPOINT;
        ResponseEntity<String> tokenKeyResponse = restTemplate.getForEntity(tokenKeyURL, String.class);
        if (!tokenKeyResponse.hasBody()) {
            throw new IllegalStateException(
                MessageFormat.format("Invalid response returned from /token_key: {0}", tokenKeyResponse.getBody()));
        }

        return JsonUtil.convertJsonToMap(tokenKeyResponse.getBody());
    }

}
