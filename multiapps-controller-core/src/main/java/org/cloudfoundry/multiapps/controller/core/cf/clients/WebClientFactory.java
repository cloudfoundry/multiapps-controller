package org.cloudfoundry.multiapps.controller.core.cf.clients;

import com.sap.cloudfoundry.client.facade.CloudCredentials;
import com.sap.cloudfoundry.client.facade.util.RestUtil;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.cloudfoundry.multiapps.controller.core.cf.OAuthClientFactory;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.inject.Inject;
import javax.inject.Named;
import javax.net.ssl.SSLException;

@Named
public class WebClientFactory {

    @Inject
    private ApplicationConfiguration configuration;
    @Inject
    private OAuthClientFactory oAuthClientFactory;

    public WebClient getWebClient(CloudCredentials credentials) {
        WebClient.Builder webClientBuilder = new RestUtil().createWebClient(false)
                .mutate()
                .baseUrl(configuration.getControllerUrl()
                        .toString());
        webClientBuilder.defaultHeaders(httpHeaders -> httpHeaders.setBearerAuth(computeAuthorizationToken(credentials)));

        //do not skip ssl
        if (!configuration.shouldSkipSslValidation()) {
            return webClientBuilder.build();
        }

        // skip ssl
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
        return WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();

    }

    private String computeAuthorizationToken(CloudCredentials credentials) {
        var oAuthClient = oAuthClientFactory.createOAuthClient();
        oAuthClient.init(credentials);
        return oAuthClient.getToken()
                .getOAuth2AccessToken()
                .getTokenValue();
    }

}
