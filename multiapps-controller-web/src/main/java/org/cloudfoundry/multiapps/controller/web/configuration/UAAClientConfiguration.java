package org.cloudfoundry.multiapps.controller.web.configuration;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.client.uaa.UAAClient;
import org.cloudfoundry.multiapps.controller.client.uaa.UAAClientFactory;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.core.util.SSLUtil;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.inject.Inject;
import javax.net.ssl.SSLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Map;

@Configuration
public class UAAClientConfiguration {

    @Inject
    @Bean
    public UAAClient uaaClient(ApplicationConfiguration configuration) {
        if (configuration.shouldSkipSslValidation()) {
            SSLUtil.disableSSLValidation();
        }
        return new UAAClientFactory().createClient(
                readTokenEndpoint(configuration.getControllerUrl(),configuration.shouldSkipSslValidation()));
    }

    @SuppressWarnings("unchecked")
    private URL readTokenEndpoint(URL targetURL, Boolean shouldSkipSslValidation) {
        try {
            Map<String, Object> infoMap = getControllerInfo(targetURL, shouldSkipSslValidation);
            var links = (Map<String, Object>) infoMap.get("links");
            var uaa = (Map<String, Object>) links.get("uaa");
            Object endpoint = uaa.get("href");
            if (endpoint == null) {
                throw new IllegalStateException(MessageFormat.format("Response from {0} does not contain a valid token endpoint",
                        targetURL.toString()));
            }
            return new URL(endpoint.toString());
        } catch (Exception e) {
            throw new IllegalStateException("Could not read token endpoint " + targetURL, e);
        }
    }

    protected Map<String, Object> getControllerInfo(URL targetURL, Boolean shouldSkipSslValidation) throws SSLException {
        WebClient webClient = buildWebClientWith(shouldSkipSslValidation);
        String infoResponse = webClient.get()
                .uri(targetURL.toString())
                .retrieve()
                .bodyToMono(String.class)
                .block();
        if (infoResponse == null) {
            throw new IllegalStateException(MessageFormat.format("Invalid response returned from {0}", targetURL.toString()));
        }
        return JsonUtil.convertJsonToMap(infoResponse);
    }

    @NotNull
    private static WebClient buildWebClientWith(Boolean shouldSkipSslValidation) {
        if (!shouldSkipSslValidation) {
            return WebClient.create();
        }
        final SslContext sslContext;
        try {
            sslContext = SslContextBuilder
                    .forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build();
        } catch (SSLException e) {
            throw new IllegalStateException("Failed to create insecure SSL context", e);
        }
        HttpClient httpClient = HttpClient.create().secure(t -> t.sslContext(sslContext));
        return WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient)).build();
    }

}
