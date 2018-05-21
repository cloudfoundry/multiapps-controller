package com.sap.cloud.lm.sl.cf.web.configuration;

import java.net.URL;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.sap.cloud.lm.sl.cf.client.uaa.UAAClient;
import com.sap.cloud.lm.sl.cf.client.uaa.UAAClientFactory;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

@Configuration
public class UAAClientConfiguration {

    @Inject
    @Bean
    @Profile("cf")
    public UAAClient uaaClient(ApplicationConfiguration configuration) {
        return new UAAClientFactory().createClient(readTokenEndpoint(configuration.getTargetURL()));
    }

    private URL readTokenEndpoint(URL targetURL) {
        try {
            String infoURL = targetURL.toString() + "/v2/info";
            ResponseEntity<String> infoResponse = new RestTemplate().getForEntity(infoURL, String.class);
            if (infoResponse == null) {
                throw new IllegalStateException("Invalid response returned from /v2/info");
            }
            Map<String, Object> infoMap = JsonUtil.convertJsonToMap(infoResponse.getBody());
            Object endpoint = infoMap.get("token_endpoint");
            if (endpoint == null) {
                endpoint = infoMap.get("authorizationEndpoint");
            }
            if (endpoint == null) {
                throw new IllegalStateException("Response from /v2/info does not contain a valid token endpoint");
            }
            return new URL(endpoint.toString());
        } catch (Exception e) {
            throw new IllegalStateException("Could not read token endpoint", e);
        }
    }

}
