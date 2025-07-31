package org.cloudfoundry.multiapps.controller.client.facade.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;

import javax.net.ssl.SSLException;
import javax.net.ssl.X509TrustManager;

import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import org.cloudfoundry.multiapps.controller.client.facade.oauth2.OAuthClient;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import reactor.netty.http.client.HttpClient;

/**
 * Some helper utilities for creating classes used for the REST support.
 *
 */
public class RestUtil {

    private static final int MAX_IN_MEMORY_SIZE = 1024 * 1024; // 1MB

    public OAuthClient createOAuthClientByControllerUrl(URL controllerUrl, boolean shouldTrustSelfSignedCertificates) {
        WebClient webClient = createWebClient(shouldTrustSelfSignedCertificates);
        URL authorizationUrl = getAuthorizationUrl(controllerUrl, webClient);
        return new OAuthClient(authorizationUrl, webClient);
    }

    private URL getAuthorizationUrl(URL controllerUrl, WebClient webClient) {
        AuthorizationEndpointGetter authorizationEndpointGetter = new AuthorizationEndpointGetter(webClient);
        return getAuthorizationUrl(authorizationEndpointGetter.getAuthorizationEndpoint(controllerUrl.toString()));
    }

    private URL getAuthorizationUrl(String authorizationEndpoint) {
        try {
            return new URL(authorizationEndpoint);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(MessageFormat.format("Error creating authorization endpoint URL for endpoint {0}.",
                                                                    authorizationEndpoint),
                                               e);
        }
    }

    public WebClient createWebClient(boolean trustSelfSignedCerts) {
        return WebClient.builder()
                        .exchangeStrategies(ExchangeStrategies.builder()
                                                              .codecs(configurer -> configurer.defaultCodecs()
                                                                                              .maxInMemorySize(MAX_IN_MEMORY_SIZE))
                                                              .build())
                        .clientConnector(buildClientConnector(trustSelfSignedCerts))
                        .build();
    }

    private ClientHttpConnector buildClientConnector(boolean trustSelfSignedCerts) {
        HttpClient httpClient = HttpClient.create()
                                          .followRedirect(true);
        if (trustSelfSignedCerts) {
            httpClient = httpClient.secure(sslContextSpec -> sslContextSpec.sslContext(buildSslContext()));
        } else {
            httpClient = httpClient.secure();
        }
        return new ReactorClientHttpConnector(httpClient);
    }

    private SslContext buildSslContext() {
        try {
            return SslContextBuilder.forClient()
                                    .trustManager(createDummyTrustManager())
                                    .build();
        } catch (SSLException e) {
            throw new RuntimeException("An error occurred setting up the SSLContext", e);
        }
    }

    private X509TrustManager createDummyTrustManager() {
        return new X509TrustManager() {

            @Override
            public void checkClientTrusted(X509Certificate[] xcs, String string) {
                // NOSONAR
            }

            @Override
            public void checkServerTrusted(X509Certificate[] xcs, String string) {
                // NOSONAR
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[] {};
            }

        };
    }
}
