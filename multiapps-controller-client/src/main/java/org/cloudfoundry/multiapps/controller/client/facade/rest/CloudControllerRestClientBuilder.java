package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.net.URL;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.net.ssl.SSLException;
import javax.net.ssl.X509TrustManager;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.cloudfoundry.multiapps.controller.Constants;
import org.cloudfoundry.multiapps.controller.Messages;
import org.cloudfoundry.multiapps.controller.client.facade.oauth2.OAuthClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.resources.LoopResources;
import reactor.netty.tcp.SslProvider;

public final class CloudControllerRestClientBuilder {

    private CloudControllerRestClientBuilder() {
    }

    public static RestClient build(URL v3ApiUrl, OAuthClient oAuthClient, Map<String, String> requestTags,
                                   ClientConfigurationOptions configOptions) {
        return buildRestClient(buildHttpClient(configOptions), v3ApiUrl, oAuthClient, requestTags);
    }

    public static RestClient buildRestClient(HttpClient httpClient, URL baseUrl, OAuthClient oAuthClient,
                                             Map<String, String> requestTags) {
        return RestClient.builder()
                         .baseUrl(baseUrl.toString())
                         .requestFactory(new ReactorClientHttpRequestFactory(httpClient))
                         .messageConverters(List.of(new ByteArrayHttpMessageConverter(),
                                                    new StringHttpMessageConverter(),
                                                    new FormHttpMessageConverter(),
                                                    new MappingJackson2HttpMessageConverter(new ObjectMapper())))
                         .requestInterceptor((request, body, execution) -> {
                             String authorization = oAuthClient.getAuthorizationHeaderValue();

                             if (authorization != null) {
                                 request.getHeaders()
                                        .set(HttpHeaders.AUTHORIZATION, authorization);
                             }

                             requestTags.forEach((String key, String value) -> request.getHeaders()
                                                                                      .set(key, value));

                             return execution.execute(request, body);
                         })
                         .defaultStatusHandler(new CloudControllerResponseErrorHandler())
                         .build();
    }

    public static WebClient buildWebClient(HttpClient httpClient, URL baseUrl, OAuthClient oAuthClient,
                                           Map<String, String> requestTags) {
        return WebClient.builder()
                        .baseUrl(baseUrl.toString())
                        .clientConnector(new ReactorClientHttpConnector(httpClient))
                        .filter(authAndTagsFilter(oAuthClient, requestTags))
                        .build();
    }

    private static ExchangeFilterFunction authAndTagsFilter(OAuthClient oAuthClient, Map<String, String> requestTags) {
        return (request, next) -> {
            ClientRequest.Builder requestBuilder = ClientRequest.from(request);
            String authorizationValue = oAuthClient.getAuthorizationHeaderValue();

            if (authorizationValue != null) {
                requestBuilder.header(HttpHeaders.AUTHORIZATION, authorizationValue);
            }

            requestTags.forEach((String key, String value) -> {
                requestBuilder.header(key, value);
            });

            return next.exchange(requestBuilder.build());
        };
    }

    static HttpClient buildHttpClient(ClientConfigurationOptions configOptions) {
        int poolSize = configOptions.connectionPoolSize()
                                    .orElse(Constants.DEFAULT_CONNECTION_POOL_SIZE);
        ConnectionProvider connectionProvider = ConnectionProvider.builder(Constants.CONNECTION_POOL_NAME)
                                                                  .maxConnections(poolSize)
                                                                  .build();
        HttpClient httpClient = HttpClient.create(connectionProvider)
                                          .followRedirect(true)
                                          .proxyWithSystemProperties();
        int connectTimeoutMillis = (int) configOptions.connectionTimeout()
                                                      .orElse(Constants.DEFAULT_CONNECT_TIMEOUT)
                                                      .toMillis();
        httpClient = httpClient.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMillis);

        Optional<Integer> threadPoolSize = configOptions.threadPoolSize();
        if (threadPoolSize.isPresent()) {
            LoopResources loopResources = LoopResources.create(Constants.CONNECTION_POOL_NAME + Constants.LOOP_RESOURCES_SUFFIX,
                                                               threadPoolSize.get(), true);
            httpClient = httpClient.runOn(loopResources);
        }

        Optional<Duration> responseTimeout = configOptions.responseTimeout();
        if (responseTimeout.isPresent()) {
            httpClient = httpClient.responseTimeout(responseTimeout.get());
        }

        Optional<Duration> handshakeTimeout = configOptions.sslHandshakeTimeout();
        if (configOptions.trustSelfSignedCertificates()) {
            httpClient = httpClient.secure(sslSpecification -> {
                SslProvider.Builder sslBuilder = sslSpecification.sslContext(buildTrustAllSslContext());
                if (handshakeTimeout.isPresent()) {
                    sslBuilder.handshakeTimeout(handshakeTimeout.get());
                }
            });
        } else if (handshakeTimeout.isPresent()) {
            httpClient = httpClient.secure(sslSpecification -> {
                SslProvider.Builder sslBuilder = sslSpecification.sslContext(buildDefaultClientSslContext());
                sslBuilder.handshakeTimeout(handshakeTimeout.get());
            });
        } else {
            httpClient = httpClient.secure();
        }

        return httpClient;
    }

    private static SslContext buildDefaultClientSslContext() {
        try {
            return SslContextBuilder.forClient()
                                    .build();
        } catch (SSLException e) {
            throw new IllegalStateException(Messages.ERROR_OCCURRED_SETTING_UP_DEFAULT_SSL_CONTEXT, e);
        }
    }

    private static SslContext buildTrustAllSslContext() {
        try {
            return SslContextBuilder.forClient()
                                    .trustManager(getTrustAllCertificatesManager())
                                    .build();
        } catch (SSLException e) {
            throw new IllegalStateException(Messages.ERROR_OCCURRED_SETTING_UP_ALWAYS_APPROVING_SSL_CONTEXT, e);
        }
    }

    private static X509TrustManager getTrustAllCertificatesManager() {
        return new X509TrustManager() {

            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[] {};
            }

        };
    }

}
