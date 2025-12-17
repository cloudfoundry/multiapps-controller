package org.cloudfoundry.multiapps.controller.core.http;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpHead;
import org.apache.hc.client5.http.classic.methods.HttpOptions;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.io.CloseMode;

public class CsrfHttpClient extends CloseableHttpClient {

    public static final String CSRF_TOKEN_HEADER_NAME = "X-CSRF-TOKEN";
    public static final String CSRF_TOKEN_HEADER_FETCH_VALUE = "Fetch";
    public static final String CSRF_TOKEN_HEADER_REQUIRED_VALUE = "Required";
    private static final List<String> NON_PROTECTED_METHODS = List.of(HttpGet.METHOD_NAME, HttpOptions.METHOD_NAME,
                                                                      HttpHead.METHOD_NAME);

    private final CloseableHttpClient delegate;
    private String csrfToken;
    private final String csrfGetTokenUrl;
    private final Map<String, String> httpRequestHeaders;
    private boolean isTokenInitialized;

    public CsrfHttpClient(CloseableHttpClient httpClient, String csrfGetTokenUrl, Map<String, String> httpRequestHeaders) {
        this.delegate = httpClient;
        this.csrfGetTokenUrl = csrfGetTokenUrl;
        this.httpRequestHeaders = httpRequestHeaders;
    }

    @Override
    protected CloseableHttpResponse doExecute(HttpHost target, ClassicHttpRequest request, HttpContext context) throws IOException {
        return executeRequest(request, () -> delegate.execute(target, request, context));
    }

    private <T> T executeRequest(HttpRequest request, Executor<T> executionSupplier) throws IOException {
        T result = executeWithCsrfTokenSetting(request, executionSupplier);
        if (!(result instanceof ClassicHttpResponse response)) {
            return result;
        }
        if (isRetryNeeded(request, response)) {
            result = executeWithCsrfTokenSetting(request, executionSupplier);
        }
        return result;
    }

    private <T> T executeWithCsrfTokenSetting(HttpRequest request, Executor<T> executionSupplier) throws IOException {
        setHttpRequestHeaders(request);
        setCrsfToken(request);
        return executionSupplier.execute();
    }

    private void setHttpRequestHeaders(HttpRequest request) {
        for (Entry<String, String> httpRequestHeaderEntry : httpRequestHeaders.entrySet()) {
            request.setHeader(httpRequestHeaderEntry.getKey(), httpRequestHeaderEntry.getValue());
        }
    }

    private void setCrsfToken(HttpRequest request) throws IOException {
        if ((request == null) || !isProtectionRequired(request)) {
            return;
        }

        initializeToken(false);
        if (csrfToken != null) {
            request.setHeader(CSRF_TOKEN_HEADER_NAME, csrfToken);
        }
    }

    private boolean isProtectionRequired(HttpRequest request) {
        return !NON_PROTECTED_METHODS.contains(request.getMethod());
    }

    private void initializeToken(boolean force) throws IOException {
        synchronized (this) {
            if (force || !isTokenInitialized) {
                csrfToken = fetchNewCsrfToken();
                isTokenInitialized = true;
            }
        }
    }

    private String fetchNewCsrfToken() throws IOException {
        if (csrfGetTokenUrl == null) {
            return null;
        }

        HttpGet fetchTokenRequest = new HttpGet(csrfGetTokenUrl);
        fetchTokenRequest.addHeader(CSRF_TOKEN_HEADER_NAME, CSRF_TOKEN_HEADER_FETCH_VALUE);
        setHttpRequestHeaders(fetchTokenRequest);
        return delegate.execute(fetchTokenRequest, response -> {
            EntityUtils.consume(response.getEntity());
            if (response.containsHeader(CSRF_TOKEN_HEADER_NAME)) {
                return response.getFirstHeader(CSRF_TOKEN_HEADER_NAME)
                               .getValue();
            }
            return null;
        });
    }

    /**
     * Checks if a request has failed due to an expired session(token is not valid anymore) and regenerates the token if needed.
     */
    private boolean isRetryNeeded(HttpRequest request, ClassicHttpResponse response) throws IOException {
        if (!isProtectionRequired(request)) {
            // The request was not protected so the error was not caused by
            // missing token.
            return false;
        }

        // The token was initialized but probably the session has expired. If it
        // is so, then the token needs to be regenerated and request retried.
        if (isTokenInitialized && (response.getCode() == HttpStatus.SC_FORBIDDEN)) {

            Header csrfTokenHeader = response.getFirstHeader(CSRF_TOKEN_HEADER_NAME);

            // Check if the 403(FORBIDDEN) error is caused by missing token
            if ((csrfTokenHeader != null) && CSRF_TOKEN_HEADER_REQUIRED_VALUE.equals(csrfTokenHeader.getValue())) {
                EntityUtils.consume(response.getEntity());
                // this means that we have previously initialized the token, but
                // server side session has expired and our token is no more
                // valid.
                initializeToken(true);
                // If the new token is null there is no point retrying the
                // request
                return csrfToken != null;
            }
        }
        return false;
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    @Override
    public void close(CloseMode closeMode) {
        delegate.close(closeMode);
    }

    private interface Executor<T> {

        T execute() throws IOException;

    }
}
