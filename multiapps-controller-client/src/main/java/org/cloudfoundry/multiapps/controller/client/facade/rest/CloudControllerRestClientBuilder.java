package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.net.URL;
import java.net.http.HttpClient;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.cloudfoundry.multiapps.controller.client.facade.oauth2.OAuthClient;

/**
 * Builds the blocking Spring {@link RestClient} used by {@link CloudControllerRestClientV3Impl} to talk to the Cloud Controller v3
 * REST API directly, without the OSS cf-java-client.
 * <p>
 * The transport is the JDK {@link java.net.http.HttpClient} (via {@link JdkClientHttpRequestFactory}). This is deliberately chosen over
 * reactor-netty: it is already available through {@code spring-web} and the JDK, so it pulls <em>no</em> extra transitive dependencies
 * (no reactor-netty, no spring-webflux) into the deploy-service WAR — which is exactly the dependency-shedding this migration is about.
 * The JDK client provides connection reuse (HTTP/2 multiplexing + keep-alive) and connect/read timeouts; TLS trust is configurable for
 * self-signed landscapes. Every request carries a bearer token from {@link OAuthClient} (refreshed as needed), and non-2xx responses are
 * mapped to {@code CloudOperationException} by the existing {@link CloudControllerResponseErrorHandler}.
 */
public final class CloudControllerRestClientBuilder {

    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofMinutes(1);

    private CloudControllerRestClientBuilder() {
    }

    public static RestClient build(URL v3ApiUrl, OAuthClient oAuthClient, Map<String, String> requestTags, TransportOptions options) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(buildHttpClient(options));
        options.responseTimeout()
               .ifPresent(requestFactory::setReadTimeout);
        return RestClient.builder()
                         .baseUrl(v3ApiUrl.toString())
                         .requestFactory(requestFactory)
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

    private static HttpClient buildHttpClient(TransportOptions options) {
        HttpClient.Builder builder = HttpClient.newBuilder()
                                               .followRedirects(HttpClient.Redirect.NORMAL)
                                               .connectTimeout(options.connectTimeout()
                                                                      .orElse(DEFAULT_CONNECT_TIMEOUT));
        if (options.trustSelfSignedCertificates()) {
            builder.sslContext(buildTrustAllSslContext());
        }
        return builder.build();
    }

    private static SSLContext buildTrustAllSslContext() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[] { trustAllManager() }, new SecureRandom());
            return sslContext;
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
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
     * The transport-tuning surface carried over from the OSS-backed factory, so operators keep the same knobs. {@code connectionPoolSize}
     * is accepted for parity but the JDK client manages connection reuse internally; it is retained for a future backend swap.
     */
    public record TransportOptions(Optional<Duration> connectTimeout, Optional<Duration> responseTimeout,
                                   Optional<Integer> connectionPoolSize, boolean trustSelfSignedCertificates) {
    }

}
