package org.cloudfoundry.multiapps.controller.core.http;

import java.io.IOException;
import java.util.Collections;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.protocol.HttpContext;
import org.cloudfoundry.multiapps.controller.core.test.HttpMocks;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CsrfHttpClientTest {

    private static final String BODY = "body";
    private static final String FOO_TOKEN = "foo";
    private static final String BAR_TOKEN = "bar";
    private static final String RESPONSE_HANDLER_RETURN_VALUE = "baz";

    private static final HttpResponse OK_RESPONSE = HttpMocks.mockResponse(builder -> builder.body(BODY)
                                                                                             .statusCode(200));
    private static final HttpResponse OK_RESPONSE_WITH_FOO_TOKEN = HttpMocks.mockResponse(builder -> builder.body(BODY)
                                                                                                            .statusCode(200)
                                                                                                            .putHeader(CsrfHttpClient.CSRF_TOKEN_HEADER_NAME,
                                                                                                                       FOO_TOKEN));
    private static final HttpResponse OK_RESPONSE_WITH_BAR_TOKEN = HttpMocks.mockResponse(builder -> builder.body(BODY)
                                                                                                            .statusCode(200)
                                                                                                            .putHeader(CsrfHttpClient.CSRF_TOKEN_HEADER_NAME,
                                                                                                                       BAR_TOKEN));
    private static final HttpResponse FORBIDDEN_RESPONSE = HttpMocks.mockResponse(b -> b.statusCode(403)
                                                                                        .body(BODY)
                                                                                        .putHeader(CsrfHttpClient.CSRF_TOKEN_HEADER_NAME,
                                                                                                   "Required"));

    @Test
    void testExecuteRequestWithNoProtectionNeeded() throws IOException {
        HttpClient httpClient = HttpMocks.mockClient(builder -> builder.addResponse(OK_RESPONSE));
        HttpRequest request = Mockito.spy(new HttpGet());

        HttpResponse response = getResultFromExecution(httpClient, client -> client.execute(null, request));

        Mockito.verify(request, Mockito.never())
               .setHeader(Mockito.anyString(), Mockito.anyString());
        Assertions.assertEquals(200, response.getStatusLine()
                                             .getStatusCode());
    }

    @Test
    void testExecuteSimpleRequestWithNoRetryShouldReturnTheResponse() throws IOException {
        HttpClient httpClient = HttpMocks.mockClient(builder -> builder.addResponse(OK_RESPONSE_WITH_FOO_TOKEN));
        HttpRequest request = Mockito.spy(new HttpPost());

        HttpResponse response = getResultFromExecution(httpClient, "dummy", client -> client.execute(null, request));

        Mockito.verify(request)
               .setHeader(CsrfHttpClient.CSRF_TOKEN_HEADER_NAME, FOO_TOKEN);

        Assertions.assertEquals(200, response.getStatusLine()
                                             .getStatusCode());
    }

    @Test
    void testExecuteSimpleRequestWithRetryNeeded() throws IOException {
        HttpClient httpClient = HttpMocks.mockClient(builder -> builder.addResponse(OK_RESPONSE_WITH_FOO_TOKEN)
                                                                       .addResponse(FORBIDDEN_RESPONSE)
                                                                       .addResponse(OK_RESPONSE_WITH_BAR_TOKEN));
        HttpRequest request = Mockito.spy(new HttpPost());

        HttpResponse response = getResultFromExecution(httpClient, "dummy", client -> client.execute(null, request));

        Mockito.verify(request)
               .setHeader(CsrfHttpClient.CSRF_TOKEN_HEADER_NAME, FOO_TOKEN);

        Mockito.verify(request)
               .setHeader(CsrfHttpClient.CSRF_TOKEN_HEADER_NAME, BAR_TOKEN);

        Assertions.assertEquals(200, response.getStatusLine()
                                             .getStatusCode());
    }

    @Test
    void testExecuteWithContext() throws IOException {
        HttpClient httpClient = HttpMocks.mockClient(builder -> builder.addResponse(OK_RESPONSE));
        HttpRequest request = Mockito.spy(new HttpGet());

        HttpResponse response = getResultFromExecution(httpClient, "dummy", client -> client.execute(null, request, (HttpContext) null));

        Mockito.verify(request, Mockito.never())
               .setHeader(Mockito.anyString(), Mockito.anyString());

        Assertions.assertEquals(200, response.getStatusLine()
                                             .getStatusCode());
    }

    @Test
    void testExecuteWithTarget() throws IOException {
        HttpClient httpClient = HttpMocks.mockClient(builder -> builder.addResponse(OK_RESPONSE));
        HttpRequest request = Mockito.spy(new HttpGet());

        HttpResponse response = getResultFromExecution(httpClient, "dummy", client -> client.execute(null, request, (HttpContext) null));

        Mockito.verify(request, Mockito.never())
               .setHeader(Mockito.anyString(), Mockito.anyString());

        Assertions.assertEquals(200, response.getStatusLine()
                                             .getStatusCode());
    }

    @Test
    void testExecuteWithResponseHandler() throws IOException {
        HttpClient httpClient = HttpMocks.mockClient(builder -> builder.addResponse(OK_RESPONSE)
                                                                       .responseHandlerReturnValue(RESPONSE_HANDLER_RETURN_VALUE));
        HttpRequest request = Mockito.spy(new HttpGet());

        String result = getResultFromExecution(httpClient, client -> client.execute(null, request, response -> null));

        Assertions.assertEquals(RESPONSE_HANDLER_RETURN_VALUE, result);
    }

    @Test
    void testWithNoUrlSet() throws IOException {
        HttpClient httpClient = HttpMocks.mockClient(builder -> builder.addResponse(OK_RESPONSE));
        HttpRequest request = Mockito.spy(new HttpPost());

        HttpResponse response = getResultFromExecution(httpClient, client -> client.execute(null, request));

        Assertions.assertNotNull(response);

        Mockito.verify(request, Mockito.never())
               .setHeader(Mockito.anyString(), Mockito.anyString());
    }

    private <T> T getResultFromExecution(HttpClient mockHttpClient, TestHttpExecutor<T> executor) throws IOException {
        return getResultFromExecution(mockHttpClient, null, executor);
    }

    private <T> T getResultFromExecution(HttpClient mockHttpClient, String csrfUrl, TestHttpExecutor<T> executor) throws IOException {
        try (CsrfHttpClient client = new CsrfHttpClient(mockHttpClient, csrfUrl, Collections.emptyMap())) {
            return executor.execute(client);
        }
    }

    interface TestHttpExecutor<T> {

        T execute(CsrfHttpClient client) throws IOException;

    }

}
