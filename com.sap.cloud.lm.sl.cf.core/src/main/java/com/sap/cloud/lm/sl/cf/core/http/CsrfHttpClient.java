package com.sap.cloud.lm.sl.cf.core.http;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

public class CsrfHttpClient implements HttpClient, Closeable {

    public static final String CSRF_TOKEN_HEADER_NAME = "X-CSRF-TOKEN";
    public static final String CSRF_TOKEN_HEADER_FETCH_VALUE = "Fetch";
    public static final String CSRF_TOKEN_HEADER_REQUIRED_VALUE = "Required";
    private static final List<String> NON_PROTECTED_METHODS = Arrays.asList(HttpGet.METHOD_NAME, HttpOptions.METHOD_NAME,
                                                                            HttpHead.METHOD_NAME);

    private final HttpClient delegate;
    private String csrfToken;
    private final String csrfGetTokenUrl;
    private final Map<String, String> httpRequestHeaders;
    private boolean isTokenInitialized;

    public CsrfHttpClient(HttpClient httpClient, String csrfGetTokenUrl, Map<String, String> httpRequestHeaders) {
        this.delegate = httpClient;
        this.csrfGetTokenUrl = csrfGetTokenUrl;
        this.httpRequestHeaders = httpRequestHeaders;
    }

    @Override
    public HttpParams getParams() {
        return delegate.getParams();
    }

    @Override
    public ClientConnectionManager getConnectionManager() {
        return delegate.getConnectionManager();
    }

    @Override
    public HttpResponse execute(HttpUriRequest request) throws IOException {
        return executeRequest(request, () -> delegate.execute(request));
    }

    @Override
    public HttpResponse execute(HttpUriRequest request, HttpContext context) throws IOException {
        return executeRequest(request, () -> delegate.execute(request, context));
    }

    @Override
    public HttpResponse execute(HttpHost target, HttpRequest request) throws IOException {
        return executeRequest(request, () -> delegate.execute(target, request));
    }

    @Override
    public HttpResponse execute(HttpHost target, HttpRequest request, HttpContext context) throws IOException {
        return executeRequest(request, () -> delegate.execute(target, request, context));
    }

    @Override
    public <T> T execute(HttpUriRequest request, ResponseHandler<? extends T> responseHandler) throws IOException {
        return executeRequest(request, () -> delegate.execute(request, responseHandler));
    }

    @Override
    public <T> T execute(HttpUriRequest request, ResponseHandler<? extends T> responseHandler, HttpContext context)
        throws IOException {
        return executeRequest(request, () -> delegate.execute(request, responseHandler, context));
    }

    @Override
    public <T> T execute(HttpHost target, HttpRequest request, ResponseHandler<? extends T> responseHandler)
        throws IOException {
        return executeRequest(request, () -> delegate.execute(target, request, responseHandler));
    }

    @Override
    public <T> T execute(HttpHost target, HttpRequest request, ResponseHandler<? extends T> responseHandler, HttpContext context)
        throws IOException {
        return executeRequest(request, () -> delegate.execute(target, request, responseHandler, context));
    }

    private <T> T executeRequest(HttpRequest request, Executor<T> executionSupplier) throws IOException {
        T result = executeWithCsrfTokenSetting(request, executionSupplier);
        if (!(result instanceof HttpResponse)) {
            return result;
        }
        HttpResponse response = (HttpResponse) result;
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
        return !NON_PROTECTED_METHODS.contains(request.getRequestLine()
                                                      .getMethod());
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
        HttpResponse response = delegate.execute(fetchTokenRequest);
        EntityUtils.consume(response.getEntity());
        if (response.containsHeader(CSRF_TOKEN_HEADER_NAME)) {
            return response.getFirstHeader(CSRF_TOKEN_HEADER_NAME)
                           .getValue();
        }

        return null;
    }

    /**
     * Checks if a request has failed due to an expired session(token is not valid anymore) and regenerates the token if needed.
     */
    private boolean isRetryNeeded(HttpRequest request, HttpResponse response) throws IOException {
        if (!isProtectionRequired(request)) {
            // The request was not protected so the error was not caused by
            // missing token.
            return false;
        }

        // The token was initialized but probably the session has expired. If it
        // is so, then the token needs to be regenerated and request retried.
        if (isTokenInitialized && (response.getStatusLine()
                                           .getStatusCode() == HttpStatus.SC_FORBIDDEN)) {

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
        if (delegate instanceof Closeable) {
            ((Closeable) delegate).close();
        }
    }

    private interface Executor<T> {

        T execute() throws IOException;

    }
}
