package org.cloudfoundry.multiapps.controller.web.configuration;

import java.net.URL;
import java.text.MessageFormat;
import java.util.Map;

import javax.inject.Inject;

import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.client.uaa.UAAClient;
import org.cloudfoundry.multiapps.controller.client.uaa.UAAClientFactory;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.core.util.SSLUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@Configuration
public class UAAClientConfiguration {

    private static final String CONTROLLER_INFO_ENDPOINT = "/v2/info";

    @Inject
    @Bean
    public UAAClient uaaClient(ApplicationConfiguration configuration) {
        if (configuration.shouldSkipSslValidation()) {
            SSLUtil.disableSSLValidation();
        }
        return new UAAClientFactory().createClient(readTokenEndpoint(configuration.getControllerUrl()));
    }

    private URL readTokenEndpoint(URL targetURL) {
        try {
            Map<String, Object> infoMap = getControllerInfo(targetURL);
            Object endpoint = infoMap.get("token_endpoint");
            if (endpoint == null) {
                endpoint = infoMap.get("authorizationEndpoint");
            }
            if (endpoint == null) {
                throw new IllegalStateException(MessageFormat.format("Response from {0} does not contain a valid token endpoint",
                                                                     CONTROLLER_INFO_ENDPOINT));
            }
            return new URL(endpoint.toString());
        } catch (Exception e) {
            throw new IllegalStateException("Could not read token endpoint", e);
        }
    }

    protected Map<String, Object> getControllerInfo(URL targetURL) {
        String infoURL = targetURL.toString() + CONTROLLER_INFO_ENDPOINT;
        ResponseEntity<String> infoResponse = new RestTemplate().getForEntity(infoURL, String.class);
        if (infoResponse.getBody() == null) {
            throw new IllegalStateException(MessageFormat.format("Invalid response returned from {0}", CONTROLLER_INFO_ENDPOINT));
        }
        return JsonUtil.convertJsonToMap(infoResponse.getBody());
    }

}
