package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.net.URL;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.net.ssl.X509TrustManager;

import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContextBuilder;
import org.cloudfoundry.multiapps.controller.client.facade.oauth2.OAuthClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.resources.LoopResources;

/**
 * Builds the blocking Spring {@link RestClient} used by {@link CloudControllerRestClientV3Impl} to talk to the Cloud Controller v3
 * REST API directly, without the OSS cf-java-client.
 * <p>
 * The transport is <b>Reactor-Netty</b> (via Spring's {@link ReactorClientHttpRequestFactory}, which lives in {@code spring-web} — NOT
 * {@code spring-webflux}). This is the same HTTP engine the deploy-service ran on under the OSS client, so its proven behavior
 * (connection pooling, TLS, timeouts, proxy handling on the target landscapes) is preserved; only the cf-java-client library on top of it
 * was removed. The tuning knobs the OSS {@code DefaultConnectionContext} honored are restored here: pool size (via {@link ConnectionProvider}),
 * connect timeout, and response timeout. The caller still consumes everything synchronously through {@link RestClient} — Reactor-Netty is
 * used purely as the transport, exactly as before.
 * <p>
 * Every request carries a bearer token from {@link OAuthClient} (refreshed as needed), and non-2xx responses are mapped to
 * {@code CloudOperationException} by the existing {@link CloudControllerResponseErrorHandler}.
 */
public final class CloudControllerRestClientBuilder {

    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofMinutes(1);
    private static final int DEFAULT_CONNECTION_POOL_SIZE = 192;
    private static final String CONNECTION_POOL_NAME = "cf-controller-client";

    private CloudControllerRestClientBuilder() {
    }

    public static RestClient build(URL v3ApiUrl, OAuthClient oAuthClient, Map<String, String> requestTags, TransportOptions options) {
        return buildRestClient(buildHttpClient(options), v3ApiUrl, oAuthClient, requestTags);
    }

    /**
     * Build a blocking {@link RestClient} on the given (already-configured) Reactor-Netty {@link HttpClient}. Sharing one
     * {@code HttpClient} between the RestClient and the {@link #buildWebClient WebClient} means both ride the same connection pool,
     * timeouts, TLS and proxy settings.
     */
    public static RestClient buildRestClient(HttpClient httpClient, URL baseUrl, OAuthClient oAuthClient,
                                             Map<String, String> requestTags) {
        return RestClient.builder()
                         .baseUrl(baseUrl.toString())
                         .requestFactory(new ReactorClientHttpRequestFactory(httpClient))
                         // Pass a fully-formed converter list via the List overload — deliberately NOT the Consumer overload.
                         // messageConverters(Consumer) first calls initMessageConverters() to build Spring's DEFAULT list and only then
                         // hands it to the consumer; that default init eagerly constructs AllEncompassingFormHttpMessageConverter ->
                         // MappingJackson2YamlHttpMessageConverter -> Jackson2ObjectMapperBuilder.yaml(), which loads a YAMLFactory that is
                         // incompatible with this codebase's jackson-core (mixed Jackson 2/3 classpath) and throws VerifyError at load time
                         // — so clearing the list afterwards is too late. The List overload skips initMessageConverters() entirely.
                         // Converters: byte[] + String + JSON (from an ObjectMapper we control) for the v3 JSON APIs, plus a plain
                         // FormHttpMessageConverter for multipart/form-data (the POST /v3/packages/{guid}/upload app-bits upload). We use
                         // FormHttpMessageConverter, NOT AllEncompassingFormHttpMessageConverter, precisely because the latter is what
                         // triggers the YAML VerifyError above.
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
                             requestTags.forEach((key, value) -> request.getHeaders()
                                                                        .set(key, value));
                             return execution.execute(request, body);
                         })
                         .defaultStatusHandler(new CloudControllerResponseErrorHandler())
                         .build();
    }

    /**
     * Build a reactive {@link WebClient} on the same Reactor-Netty {@link HttpClient} as the RestClient, applying the identical
     * per-request bearer auth (from {@link OAuthClient}, refreshed as needed) and request-tag headers. This is used only for the
     * intra-operation concurrency spots (concurrent pagination) — the same mechanism the OSS client used — while the rest of the client
     * stays on the blocking RestClient. Response error handling for these calls is done by the callers (they map WebClient errors the
     * same way {@link CloudControllerResponseErrorHandler} does for the RestClient).
     */
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
            ClientRequest.Builder builder = ClientRequest.from(request);
            String authorization = oAuthClient.getAuthorizationHeaderValue();
            if (authorization != null) {
                builder.header(HttpHeaders.AUTHORIZATION, authorization);
            }
            requestTags.forEach(builder::header);
            return next.exchange(builder.build());
        };
    }

    static HttpClient buildHttpClient(TransportOptions options) {
        int poolSize = options.connectionPoolSize()
                              .orElse(DEFAULT_CONNECTION_POOL_SIZE);
        ConnectionProvider connectionProvider = ConnectionProvider.builder(CONNECTION_POOL_NAME)
                                                                  .maxConnections(poolSize)
                                                                  .build();
        HttpClient httpClient = HttpClient.create(connectionProvider)
                                          .followRedirect(true)
                                          // Honor JVM standard proxy settings (-Dhttp(s).proxyHost/proxyPort/nonProxyHosts), matching the
                                          // OSS DefaultConnectionContext's proxy support. No-op when no proxy is configured, so it is safe
                                          // on direct-connect landscapes and required on proxied ones.
                                          .proxyWithSystemProperties();
        int connectTimeoutMillis = (int) options.connectTimeout()
                                                 .orElse(DEFAULT_CONNECT_TIMEOUT)
                                                 .toMillis();
        httpClient = httpClient.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMillis);
        // Size the I/O event-loop pool from threadPoolSize, mirroring the OSS DefaultConnectionContext.threadPoolSize (which created a
        // reactor-netty LoopResources the client ran on). When unset, reactor-netty uses its shared default LoopResources.
        Optional<Integer> threadPoolSize = options.threadPoolSize();
        if (threadPoolSize.isPresent()) {
            LoopResources loopResources = LoopResources.create(CONNECTION_POOL_NAME + "-loop", threadPoolSize.get(), true);
            httpClient = httpClient.runOn(loopResources);
        }
        Optional<Duration> responseTimeout = options.responseTimeout();
        if (responseTimeout.isPresent()) {
            httpClient = httpClient.responseTimeout(responseTimeout.get());
        }
        Optional<Duration> handshakeTimeout = options.sslHandshakeTimeout();
        if (options.trustSelfSignedCertificates()) {
            httpClient = httpClient.secure(spec -> {
                var sslBuilder = spec.sslContext(buildTrustAllSslContext());
                handshakeTimeout.ifPresent(sslBuilder::handshakeTimeout);
            });
        } else if (handshakeTimeout.isPresent()) {
            httpClient = httpClient.secure(spec -> {
                var sslBuilder = spec.sslContext(defaultClientSslContext());
                sslBuilder.handshakeTimeout(handshakeTimeout.get());
            });
        } else {
            httpClient = httpClient.secure();
        }
        return httpClient;
    }

    private static io.netty.handler.ssl.SslContext defaultClientSslContext() {
        try {
            return SslContextBuilder.forClient()
                                    .build();
        } catch (javax.net.ssl.SSLException e) {
            throw new IllegalStateException("An error occurred setting up the default SSLContext", e);
        }
    }

    private static io.netty.handler.ssl.SslContext buildTrustAllSslContext() {
        try {
            return SslContextBuilder.forClient()
                                    .trustManager(trustAllManager())
                                    .build();
        } catch (javax.net.ssl.SSLException e) {
            throw new IllegalStateException("An error occurred setting up the SSLContext", e);
        }
    }

    private static X509TrustManager trustAllManager() {
        return new X509TrustManager() {

            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
                // NOSONAR - self-signed certificate trust is opt-in via TransportOptions
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
                // NOSONAR - self-signed certificate trust is opt-in via TransportOptions
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[] {};
            }

        };
    }

    /**
     * The transport-tuning surface, honored by the Reactor-Netty transport: {@code connectTimeout} and {@code responseTimeout} on the
     * client, {@code sslHandshakeTimeout} on the TLS layer, {@code connectionPoolSize} on the {@link ConnectionProvider}, and
     * {@code threadPoolSize} on the I/O event-loop ({@link LoopResources}). Mirrors the knobs the OSS {@code DefaultConnectionContext}
     * exposed.
     */
    public record TransportOptions(Optional<Duration> connectTimeout, Optional<Duration> responseTimeout,
                                   Optional<Duration> sslHandshakeTimeout, Optional<Integer> connectionPoolSize,
                                   Optional<Integer> threadPoolSize, boolean trustSelfSignedCertificates) {
    }

}
