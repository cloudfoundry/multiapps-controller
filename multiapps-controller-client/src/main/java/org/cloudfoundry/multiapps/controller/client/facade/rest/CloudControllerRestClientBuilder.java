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
import org.springframework.http.client.ClientHttpRequestInterceptor;
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

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private CloudControllerRestClientBuilder() {
    }

    public static RestClient buildRestClient(HttpClient httpClient, URL baseUrl, OAuthClient oAuthClient,
                                             Map<String, String> requestTags) {
        return RestClient.builder()
                         .baseUrl(baseUrl.toString())
                         .requestFactory(new ReactorClientHttpRequestFactory(httpClient))
                         .messageConverters(List.of(new ByteArrayHttpMessageConverter(),
                                                    new StringHttpMessageConverter(),
                                                    new FormHttpMessageConverter(),
                                                    new MappingJackson2HttpMessageConverter(OBJECT_MAPPER)))
                         .requestInterceptor(restClientAuthAndTagsInterceptor(oAuthClient, requestTags))
                         .defaultStatusHandler(new CloudControllerResponseErrorHandler())
                         .build();
    }

    private static ClientHttpRequestInterceptor restClientAuthAndTagsInterceptor(OAuthClient oAuthClient, Map<String, String> requestTags) {
        return (request, body, execution) -> {
            String authorization = oAuthClient.getAuthorizationHeaderValue();

            if (authorization != null) {
                request.getHeaders()
                       .set(HttpHeaders.AUTHORIZATION, authorization);
            }

            requestTags.forEach((String key, String value) -> request.getHeaders()
                                                                     .set(key, value));

            return execution.execute(request, body);
        };
    }

    public static WebClient buildWebClient(HttpClient httpClient, URL baseUrl, OAuthClient oAuthClient,
                                           Map<String, String> requestTags) {
        return WebClient.builder()
                        .baseUrl(baseUrl.toString())
                        .clientConnector(new ReactorClientHttpConnector(httpClient))
                        .filter(webClientAuthAndTagsFilter(oAuthClient, requestTags))
                        .build();
    }

    private static ExchangeFilterFunction webClientAuthAndTagsFilter(OAuthClient oAuthClient, Map<String, String> requestTags) {
        return (request, next) -> {
            ClientRequest.Builder requestBuilder = ClientRequest.from(request);
            String authorizationValue = oAuthClient.getAuthorizationHeaderValue();

            if (authorizationValue != null) {
                requestBuilder.header(HttpHeaders.AUTHORIZATION, authorizationValue);
            }

            requestTags.forEach(requestBuilder::header);

            return next.exchange(requestBuilder.build());
        };
    }

    public static HttpClient buildHttpClient(ClientConfigurationOptions configOptions) {
        HttpClient httpClient = createBaseHttpClient(configOptions);
        httpClient = configureConnectionTimeout(httpClient, configOptions);
        httpClient = configureThreadPool(httpClient, configOptions);
        httpClient = configureResponseTimeout(httpClient, configOptions);
        httpClient = configureSsl(httpClient, configOptions);

        return httpClient;
    }

    private static HttpClient createBaseHttpClient(ClientConfigurationOptions configOptions) {
        int poolSize = configOptions.connectionPoolSize()
                                    .orElse(Constants.DEFAULT_CONNECTION_POOL_SIZE);

        ConnectionProvider connectionProvider = ConnectionProvider.builder(Constants.CONNECTION_POOL_NAME)
                                                                  .maxConnections(poolSize)
                                                                  .build();

        return HttpClient.create(connectionProvider)
                         .followRedirect(true)
                         .proxyWithSystemProperties();
    }

    private static HttpClient configureConnectionTimeout(HttpClient httpClient, ClientConfigurationOptions configOptions) {
        int connectTimeoutMillis = (int) configOptions.connectionTimeout()
                                                      .orElse(Constants.DEFAULT_CONNECT_TIMEOUT)
                                                      .toMillis();

        return httpClient.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMillis);
    }

    private static HttpClient configureThreadPool(HttpClient httpClient, ClientConfigurationOptions configOptions) {
        Optional<Integer> threadPoolSize = configOptions.threadPoolSize();
        if (threadPoolSize.isEmpty()) {
            return httpClient;
        }

        LoopResources loopResources = LoopResources.create(Constants.CONNECTION_POOL_NAME + Constants.LOOP_RESOURCES_SUFFIX,
                                                           threadPoolSize.get(), true);

        return httpClient.runOn(loopResources);
    }

    private static HttpClient configureResponseTimeout(HttpClient httpClient, ClientConfigurationOptions configOptions) {
        Optional<Duration> responseTimeout = configOptions.responseTimeout();
        if (responseTimeout.isEmpty()) {
            return httpClient;
        }

        return httpClient.responseTimeout(responseTimeout.get());
    }

    private static HttpClient configureSsl(HttpClient httpClient, ClientConfigurationOptions configOptions) {
        Optional<Duration> handshakeTimeout = configOptions.sslHandshakeTimeout();
        boolean trustSelfSignedCertificates = configOptions.trustSelfSignedCertificates();

        if (!trustSelfSignedCertificates && handshakeTimeout.isEmpty()) {
            return httpClient.secure();
        }

        SslContext sslContext = trustSelfSignedCertificates ? buildTrustAllSslContext() : buildDefaultClientSslContext();
        return httpClient.secure(sslSpecification -> {
            SslProvider.Builder sslBuilder = sslSpecification.sslContext(sslContext);
            handshakeTimeout.ifPresent(timeout -> sslBuilder.handshakeTimeout(timeout));
        });
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
