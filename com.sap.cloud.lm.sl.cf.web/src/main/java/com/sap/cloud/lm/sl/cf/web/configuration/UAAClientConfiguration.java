package com.sap.cloud.lm.sl.cf.web.configuration;

import java.net.URL;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.web.client.RestTemplate;

import com.sap.cloud.lm.sl.cf.client.uaa.UAAClient;
import com.sap.cloud.lm.sl.cf.client.uaa.UAAClientFactory;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

@Configuration
public class UAAClientConfiguration {

    @Bean
    @Profile("cf")
    public UAAClient uaaClient() {
        return new UAAClientFactory().createClient(readTokenEndpoint(com.sap.cloud.lm.sl.cf.core.util.Configuration.getInstance()
            .getTargetURL()));
    }

    private URL readTokenEndpoint(URL targetURL) {
        try {
            String infoURL = targetURL.toString() + "/v2/info";
            ResponseEntity<String> infoResponse = new RestTemplate().getForEntity(infoURL, String.class);
            if (infoResponse == null) {
                throw new InternalAuthenticationServiceException("Invalid response returned from /v2/info");
            }
            Map<String, Object> infoMap = JsonUtil.convertJsonToMap(infoResponse.getBody());
            Object endpoint = infoMap.get("token_endpoint");
            if (endpoint == null) {
                endpoint = infoMap.get("authorizationEndpoint");
            }
            if (endpoint == null) {
                throw new InternalAuthenticationServiceException("Response from /v2/info does not contain a valid token endpoint");
            }
            return new URL(endpoint.toString());
        } catch (Exception e) {
            throw new InternalAuthenticationServiceException("Could not read token endpoint", e);
        }
    }

}
