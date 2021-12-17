package org.cloudfoundry.multiapps.controller.core.http;

import java.net.ProxySelector;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.DefaultServiceUnavailableRetryStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.SystemDefaultRoutePlanner;
import org.apache.http.message.BasicHeader;

public class HttpClientFactory {

    private static final int CONNECT_TIMEOUT = (int) TimeUnit.MINUTES.toMillis(2);
    private static final int SOCKET_TIMEOUT = (int) TimeUnit.MINUTES.toMillis(3);

    private final LayeredConnectionSocketFactory sslSocketFactory;
    private CookieSpecification cookieSpecification = CookieSpecification.DEFAULT;

    public HttpClientFactory(LayeredConnectionSocketFactory sslSocketFactory) {
        this.sslSocketFactory = sslSocketFactory;
    }

    public HttpClientFactory withCookieSpecification(CookieSpecification cookieSpecification) {
        this.cookieSpecification = cookieSpecification;
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
                                .setSSLSocketFactory(sslSocketFactory)
                                .setRoutePlanner(createRoutePlanner())
                                .setDefaultRequestConfig(createDefaultRequestConfig())
                                .setRetryHandler(createRetryHandler())
                                .setServiceUnavailableRetryStrategy(createServiceUnavailableRetryStrategy());
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
                            .setConnectTimeout(CONNECT_TIMEOUT)
                            .setSocketTimeout(SOCKET_TIMEOUT)
                            .setCookieSpec(cookieSpecification.toString())
                            .build();
    }

    private HttpRequestRetryHandler createRetryHandler() {
        return new DefaultHttpRequestRetryHandler(3, true);
    }

    private BasicHeader createAuthorizationHeader(String value) {
        return new BasicHeader("Authorization", value);
    }

    private ServiceUnavailableRetryStrategy createServiceUnavailableRetryStrategy() {
        return new DefaultServiceUnavailableRetryStrategy(3, 2000);
    }

}
