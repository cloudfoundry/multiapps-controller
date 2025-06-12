package org.cloudfoundry.multiapps.controller.core.http;

import java.net.ProxySelector;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import org.apache.hc.client5.http.HttpRequestRetryStrategy;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.StandardCookieSpec;
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.impl.routing.SystemDefaultRoutePlanner;
import org.apache.hc.client5.http.routing.HttpRoutePlanner;
import org.apache.hc.client5.http.ssl.TlsSocketStrategy;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;

public class HttpClientFactory {

    private static final Timeout DEFAULT_CONNECT_TIMEOUT = Timeout.ofMinutes(2);
    private static final Timeout DEFAULT_SOCKET_TIMEOUT = Timeout.ofMinutes(3);
    private static final Timeout DEFAULT_RESPONSE_TIMEOUT = Timeout.ofMinutes(10);

    private final TlsSocketStrategy tlsSocketStrategy;
    private Timeout connectTimeout = DEFAULT_CONNECT_TIMEOUT;
    private Timeout socketTimeout = DEFAULT_SOCKET_TIMEOUT;
    private Timeout responseTimeout = DEFAULT_RESPONSE_TIMEOUT;

    public HttpClientFactory(TlsSocketStrategy tlsSocketStrategy) {
        this.tlsSocketStrategy = tlsSocketStrategy;
    }

    public HttpClientFactory withConnectTimeout(Timeout connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    public HttpClientFactory withSocketTimeout(Timeout socketTimeout) {
        this.socketTimeout = socketTimeout;
        return this;
    }

    public HttpClientFactory withResponseTimeout(Timeout responseTimeout) {
        this.responseTimeout = responseTimeout;
        return this;
    }

    public CloseableHttpClient createBasicAuthHttpClient(String username, String password) {
        String authorizationHeaderValue = "Basic " + encodeCredentials(username, password);
        return createHttpClientBuilder().setDefaultHeaders(Collections.singletonList(createAuthorizationHeader(authorizationHeaderValue)))
                                        .build();
    }

    public CloseableHttpClient createOAuthHttpClient(String token) {
        String authorizationHeaderValue = "Bearer " + token;
        return createHttpClientBuilder().setDefaultHeaders(Collections.singletonList(createAuthorizationHeader(authorizationHeaderValue)))
                                        .build();
    }

    public CloseableHttpClient createNoAuthHttpClient() {
        return createHttpClientBuilder().build();
    }

    private String encodeCredentials(String username, String password) {
        Base64.Encoder encoder = Base64.getEncoder();
        return encoder.encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    private HttpClientBuilder createHttpClientBuilder() {
        return HttpClientBuilder.create()
                                .setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
                                                                                               .setDefaultSocketConfig(
                                                                                                   SocketConfig.custom()
                                                                                                               .setSoTimeout(
                                                                                                                   socketTimeout
                                                                                                               )
                                                                                                               .build())
                                                                                               .setTlsSocketStrategy(tlsSocketStrategy)
                                                                                               .setDefaultConnectionConfig(
                                                                                                   ConnectionConfig.custom()
                                                                                                                   .setConnectTimeout(
                                                                                                                       connectTimeout)
                                                                                                                   .setSocketTimeout(
                                                                                                                       socketTimeout)
                                                                                                                   .build())
                                                                                               .build())
                                .setRoutePlanner(createRoutePlanner())
                                .setDefaultRequestConfig(createDefaultRequestConfig())
                                .setRetryStrategy(createRetryStrategy());
    }

    private HttpRoutePlanner createRoutePlanner() {
        // If http.nonProxyHosts is not set, then "localhost" is used as a default value, which prevents setting a proxy on localhost.
        if (System.getProperty("http.nonProxyHosts") == null) {
            System.setProperty("http.nonProxyHosts", "");
        }
        return new SystemDefaultRoutePlanner(ProxySelector.getDefault());
    }

    private RequestConfig createDefaultRequestConfig() {
        return RequestConfig.custom()
                            .setResponseTimeout(responseTimeout)
                            .setCookieSpec(StandardCookieSpec.RELAXED)
                            .build();
    }

    private HttpRequestRetryStrategy createRetryStrategy() {
        return new DefaultHttpRequestRetryStrategy(3, TimeValue.ofMilliseconds(5000));
    }

    private BasicHeader createAuthorizationHeader(String value) {
        return new BasicHeader("Authorization", value);
    }

}
