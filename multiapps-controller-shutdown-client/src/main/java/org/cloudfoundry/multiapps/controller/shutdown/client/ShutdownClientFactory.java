package org.cloudfoundry.multiapps.controller.shutdown.client;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import org.apache.hc.client5.http.HttpRequestRetryStrategy;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.cloudfoundry.multiapps.common.util.MapUtil;
import org.cloudfoundry.multiapps.controller.core.http.CsrfHttpClient;
import org.cloudfoundry.multiapps.controller.shutdown.client.configuration.ShutdownClientConfiguration;

public class ShutdownClientFactory {

    private static final String CSRF_TOKEN_ENDPOINT = "/api/v1/csrf-token";
    private static final int RETRY_COUNT = 5;
    private static final int RETRY_INTERVAL_IN_MILLIS = 5000;
    private static final Timeout CONNECT_TIMEOUT = Timeout.ofMinutes(2);
    private static final Timeout SOCKET_TIMEOUT = Timeout.ofMinutes(5);
    private static final Timeout RESPONSE_TIMEOUT = Timeout.ofMinutes(10);

    public ShutdownClient createShutdownClient(ShutdownClientConfiguration configuration) {
        return new ShutdownClientImpl(configuration.getApplicationUrl(),
                                      defaultHttpHeaders -> createCsrfHttpClient(configuration, defaultHttpHeaders));
    }

    private CsrfHttpClient createCsrfHttpClient(ShutdownClientConfiguration configuration, Map<String, String> defaultHttpHeaders) {
        CloseableHttpClient httpClient = createHttpClient();
        String csrfTokenUrl = computeCsrfTokenUrl(configuration);
        Map<String, String> enrichedDefaultHttpHeaders = MapUtil.merge(computeHeaders(configuration), defaultHttpHeaders);
        return new CsrfHttpClient(httpClient, csrfTokenUrl, enrichedDefaultHttpHeaders);
    }

    private CloseableHttpClient createHttpClient() {
        return HttpClientBuilder.create()
                                .setRetryStrategy(createRetryStrategy())
                                .setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
                                                                                               .setDefaultSocketConfig(
                                                                                                   SocketConfig.custom()
                                                                                                               .setSoTimeout(
                                                                                                                   SOCKET_TIMEOUT)
                                                                                                               .build())
                                                                                               .setDefaultConnectionConfig(
                                                                                                   ConnectionConfig.custom()
                                                                                                                   .setConnectTimeout(
                                                                                                                       CONNECT_TIMEOUT)
                                                                                                                   .setSocketTimeout(
                                                                                                                       SOCKET_TIMEOUT)
                                                                                                                   .build())
                                                                                               .build())
                                .setDefaultRequestConfig(RequestConfig.custom()
                                                                      .setResponseTimeout(RESPONSE_TIMEOUT)
                                                                      .build())
                                .build();
    }

    private HttpRequestRetryStrategy createRetryStrategy() {
        return new DefaultHttpRequestRetryStrategy(RETRY_COUNT, TimeValue.ofMilliseconds(RETRY_INTERVAL_IN_MILLIS));
    }

    private String computeCsrfTokenUrl(ShutdownClientConfiguration configuration) {
        return configuration.getApplicationUrl() + CSRF_TOKEN_ENDPOINT;
    }

    private Map<String, String> computeHeaders(ShutdownClientConfiguration configuration) {
        String credentials = computeBasicAuthorizationCredentials(configuration);
        return Map.of(HttpHeaders.AUTHORIZATION, String.format("Basic %s", encode(credentials)));
    }

    private String encode(String string) {
        return Base64.getEncoder()
                     .encodeToString(string.getBytes(StandardCharsets.UTF_8));
    }

    private String computeBasicAuthorizationCredentials(ShutdownClientConfiguration configuration) {
        return String.format("%s:%s", configuration.getUsername(), configuration.getPassword());
    }

}
