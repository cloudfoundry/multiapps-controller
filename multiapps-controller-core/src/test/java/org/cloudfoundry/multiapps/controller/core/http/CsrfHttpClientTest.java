package org.cloudfoundry.multiapps.controller.core.http;

import java.io.IOException;
import java.util.Collections;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.cloudfoundry.multiapps.controller.core.test.HttpMocks;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class CsrfHttpClientTest {

    private static final String BODY = "body";
    private static final String FOO_TOKEN = "foo";
    private static final String BAR_TOKEN = "bar";
    private static final String RESPONSE_HANDLER_RETURN_VALUE = "baz";
    private static final String TEST_URL = "https://dummy";
    private static final String TEST_CSRF_URL = "dummy";

    private static final HttpResponse OK_RESPONSE = HttpMocks.mockResponse(builder -> builder.body(BODY)
                                                                                             .statusCode(200));
    private static final HttpResponse OK_RESPONSE_WITH_FOO_TOKEN = HttpMocks.mockResponse(builder -> builder.body(BODY)
                                                                                                            .statusCode(200)
                                                                                                            .putHeader(
                                                                                                                CsrfHttpClient.CSRF_TOKEN_HEADER_NAME,
                                                                                                                FOO_TOKEN));
    private static final HttpResponse OK_RESPONSE_WITH_BAR_TOKEN = HttpMocks.mockResponse(builder -> builder.body(BODY)
                                                                                                            .statusCode(200)
                                                                                                            .putHeader(
                                                                                                                CsrfHttpClient.CSRF_TOKEN_HEADER_NAME,
                                                                                                                BAR_TOKEN));
    private static final HttpResponse FORBIDDEN_RESPONSE = HttpMocks.mockResponse(b -> b.statusCode(403)
                                                                                        .body(BODY)
                                                                                        .putHeader(CsrfHttpClient.CSRF_TOKEN_HEADER_NAME,
                                                                                                   "Required"));

    @Test
    void testExecuteRequestWithNoProtectionNeeded() throws IOException {
        CloseableHttpClient httpClient = HttpMocks.mockClient(builder -> builder.addResponse(OK_RESPONSE));
        ClassicHttpRequest request = Mockito.spy(new HttpGet(TEST_URL));

        HttpResponse response = getResultFromExecution(httpClient, client -> client.execute(null, request));

        Mockito.verify(request, Mockito.never())
               .setHeader(Mockito.anyString(), Mockito.anyString());
        Assertions.assertEquals(200, response.getCode());
    }

    @Test
    void testExecuteSimpleRequestWithNoRetryShouldReturnTheResponse() throws IOException {
        CloseableHttpClient httpClient = HttpMocks.mockClient(builder -> builder.addResponse(OK_RESPONSE_WITH_FOO_TOKEN));
        ClassicHttpRequest request = Mockito.spy(new HttpPost(TEST_URL));
        when(httpClient.execute(any(), ArgumentMatchers.<HttpClientResponseHandler<Object>> any())).thenReturn(FOO_TOKEN);

        HttpResponse response = getResultFromExecution(httpClient, TEST_CSRF_URL, client -> client.execute(null, request));

        Mockito.verify(request)
               .setHeader(CsrfHttpClient.CSRF_TOKEN_HEADER_NAME, FOO_TOKEN);

        Assertions.assertEquals(200, response.getCode());
    }

    @Test
    void testExecuteSimpleRequestWithRetryNeeded() throws IOException {
        CloseableHttpClient httpClient = HttpMocks.mockClient(builder -> builder.addResponse(OK_RESPONSE_WITH_FOO_TOKEN)
                                                                                .addResponse(FORBIDDEN_RESPONSE)
                                                                                .addResponse(OK_RESPONSE_WITH_BAR_TOKEN));
        ClassicHttpRequest request = Mockito.spy(new HttpPost(TEST_URL));
        when(httpClient.execute(any(), ArgumentMatchers.<HttpClientResponseHandler<Object>> any())).thenReturn(FOO_TOKEN)
                                                                                                   .thenReturn(BAR_TOKEN);

        HttpResponse response = getResultFromExecution(httpClient, TEST_CSRF_URL, client -> client.execute(null, request));

        Mockito.verify(request)
               .setHeader(CsrfHttpClient.CSRF_TOKEN_HEADER_NAME, FOO_TOKEN);

        Mockito.verify(request)
               .setHeader(CsrfHttpClient.CSRF_TOKEN_HEADER_NAME, BAR_TOKEN);

        Assertions.assertEquals(200, response.getCode());
    }

    @Test
    void testExecuteWithContext() throws IOException {
        CloseableHttpClient httpClient = HttpMocks.mockClient(builder -> builder.addResponse(OK_RESPONSE));
        ClassicHttpRequest request = Mockito.spy(new HttpGet(TEST_URL));

        HttpResponse response = getResultFromExecution(httpClient, TEST_CSRF_URL,
                                                       client -> client.execute(null, request, (HttpContext) null));

        Mockito.verify(request, Mockito.never())
               .setHeader(Mockito.anyString(), Mockito.anyString());

        Assertions.assertEquals(200, response.getCode());
    }

    @Test
    void testExecuteWithTarget() throws IOException {
        CloseableHttpClient httpClient = HttpMocks.mockClient(builder -> builder.addResponse(OK_RESPONSE));
        ClassicHttpRequest request = Mockito.spy(new HttpGet(TEST_URL));

        HttpResponse response = getResultFromExecution(httpClient, TEST_CSRF_URL,
                                                       client -> client.execute(null, request, (HttpContext) null));

        Mockito.verify(request, Mockito.never())
               .setHeader(Mockito.anyString(), Mockito.anyString());

        Assertions.assertEquals(200, response.getCode());
    }

    @Test
    void testExecuteWithResponseHandler() throws IOException {
        CloseableHttpClient httpClient = HttpMocks.mockClient(builder -> builder.addResponse(OK_RESPONSE));
        ClassicHttpRequest request = Mockito.spy(new HttpGet(TEST_URL));

        String result = getResultFromExecution(httpClient,
                                               client -> client.execute(null, request, response -> RESPONSE_HANDLER_RETURN_VALUE));

        Assertions.assertEquals(RESPONSE_HANDLER_RETURN_VALUE, result);
    }

    @Test
    void testWithNoUrlSet() throws IOException {
        CloseableHttpClient httpClient = HttpMocks.mockClient(builder -> builder.addResponse(OK_RESPONSE));
        ClassicHttpRequest request = Mockito.spy(new HttpPost(TEST_URL));

        HttpResponse response = getResultFromExecution(httpClient, client -> client.execute(null, request));

        Assertions.assertNotNull(response);

        Mockito.verify(request, Mockito.never())
               .setHeader(Mockito.anyString(), Mockito.anyString());
    }

    private <T> T getResultFromExecution(CloseableHttpClient mockHttpClient, TestHttpExecutor<T> executor) throws IOException {
        return getResultFromExecution(mockHttpClient, null, executor);
    }

    private <T> T getResultFromExecution(CloseableHttpClient mockHttpClient, String csrfUrl, TestHttpExecutor<T> executor)
        throws IOException {
        try (CsrfHttpClient client = new CsrfHttpClient(mockHttpClient, csrfUrl, Collections.emptyMap())) {
            return executor.execute(client);
        }
    }

    interface TestHttpExecutor<T> {

        T execute(CsrfHttpClient client) throws IOException;

    }

}
