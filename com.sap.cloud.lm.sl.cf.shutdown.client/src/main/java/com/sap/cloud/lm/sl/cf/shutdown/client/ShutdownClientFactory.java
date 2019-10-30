package com.sap.cloud.lm.sl.cf.shutdown.client;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import org.apache.http.HttpHeaders;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultServiceUnavailableRetryStrategy;
import org.apache.http.impl.client.HttpClientBuilder;

import com.sap.cloud.lm.sl.cf.core.http.CsrfHttpClient;
import com.sap.cloud.lm.sl.cf.shutdown.client.configuration.ShutdownClientConfiguration;
import com.sap.cloud.lm.sl.common.util.MapUtil;

public class ShutdownClientFactory {

    private static final String CSRF_TOKEN_ENDPOINT = "/api/v1/csrf-token";
    private static final int RETRY_COUNT = 5;
    private static final int RETRY_INTERVAL_IN_MILLIS = 5000;

    public ShutdownClient createShutdownClient(ShutdownClientConfiguration configuration) {
        return new ShutdownClientImpl(configuration.getApplicationUrl(),
                                      defaultHttpHeaders -> createCsrfHttpClient(configuration, defaultHttpHeaders));

    }

    private CsrfHttpClient createCsrfHttpClient(ShutdownClientConfiguration configuration, Map<String, String> defaultHttpHeaders) {
        CloseableHttpClient httpClient = createHttpClient(configuration);
        String csrfTokenUrl = computeCsrfTokenUrl(configuration);
        Map<String, String> enrichedDefaultHttpHeaders = MapUtil.merge(computeHeaders(configuration), defaultHttpHeaders);
        return new CsrfHttpClient(httpClient, csrfTokenUrl, enrichedDefaultHttpHeaders);
    }

    private CloseableHttpClient createHttpClient(ShutdownClientConfiguration configuration) {
        return HttpClientBuilder.create()
                                .setServiceUnavailableRetryStrategy(createServiceUnavailableRetryStrategy())
                                .build();
    }

    private ServiceUnavailableRetryStrategy createServiceUnavailableRetryStrategy() {
        return new DefaultServiceUnavailableRetryStrategy(RETRY_COUNT, RETRY_INTERVAL_IN_MILLIS);
    }

    private String computeCsrfTokenUrl(ShutdownClientConfiguration configuration) {
        return configuration.getApplicationUrl() + CSRF_TOKEN_ENDPOINT;
    }

    private Map<String, String> computeHeaders(ShutdownClientConfiguration configuration) {
        String credentials = computeBasicAuthorizationCredentials(configuration);
        return MapUtil.asMap(HttpHeaders.AUTHORIZATION, String.format("Basic %s", encode(credentials)));
    }

    private String encode(String string) {
        return Base64.getEncoder()
                     .encodeToString(string.getBytes(StandardCharsets.UTF_8));
    }

    private String computeBasicAuthorizationCredentials(ShutdownClientConfiguration configuration) {
        return String.format("%s:%s", configuration.getUsername(), configuration.getPassword());
    }

}
