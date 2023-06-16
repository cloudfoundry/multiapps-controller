package org.cloudfoundry.multiapps.controller.web.configuration;

import java.net.URL;
import java.text.MessageFormat;
import java.util.Map;

import javax.inject.Inject;

import com.sap.cloudfoundry.client.facade.util.RestUtil;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.client.uaa.UAAClient;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.core.util.SSLUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class UAAClientConfiguration {

    @Inject
    @Bean
    public UAAClient uaaClient(ApplicationConfiguration configuration) {
        if (configuration.shouldSkipSslValidation()) {
            SSLUtil.disableSSLValidation();
        }
        return new UAAClient(readTokenEndpoint(configuration.getControllerUrl()),
                             new RestUtil().createWebClient(false));
    }

    @SuppressWarnings("unchecked")
    private URL readTokenEndpoint(URL targetURL) {
        try {
            Map<String, Object> infoMap = getControllerInfo(targetURL);
            var links = (Map<String, Object>) infoMap.get("links");
            var uaa = (Map<String, Object>) links.get("uaa");
            Object endpoint = uaa.get("href");
            if (endpoint == null) {
                throw new IllegalStateException(MessageFormat.format("Response from {0} does not contain a valid token endpoint",
                                                                     targetURL.toString()));
            }
            return new URL(endpoint.toString());
        } catch (Exception e) {
            throw new IllegalStateException("Could not read token endpoint", e);
        }
    }

    protected Map<String, Object> getControllerInfo(URL targetURL) {
        String infoResponse = WebClient.create()
                                       .get()
                                       .uri(targetURL.toString())
                                       .retrieve()
                                       .bodyToMono(String.class)
                                       .block();
        if (infoResponse == null) {
            throw new IllegalStateException(MessageFormat.format("Invalid response returned from {0}", targetURL.toString()));
        }
        return JsonUtil.convertJsonToMap(infoResponse);
    }

}
