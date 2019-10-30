package com.sap.cloud.lm.sl.cf.core.http;

import java.io.IOException;
import java.util.Collections;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.protocol.HttpContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.core.util.HttpClientMockBuilder;
import com.sap.cloud.lm.sl.cf.core.util.HttpClientRequestMockBuilder;
import com.sap.cloud.lm.sl.cf.core.util.HttpClientResponseMockBuilder;

public class CsrfHttpClientTest {

    private static final String DUMMY_TOKEN_2 = "dummy-token-2";
    private static final String DUMMY_TOKEN = "dummy-token";

    @Test
    public void testExecuteRequestWithNoProtectionNeeded() throws IOException {
        HttpClient mockHttpClient = HttpClientMockBuilder.builder()
                                                         .response(HttpClientResponseMockBuilder.builder()
                                                                                                .response("dummy-response")
                                                                                                .statusCodes(200)
                                                                                                .build())
                                                         .build();
        HttpRequest mockRequest = HttpClientRequestMockBuilder.builder()
                                                              .method(HttpGet.METHOD_NAME)
                                                              .build();
        HttpResponse result = getResultFromExecution(mockHttpClient, mockRequest, (client) -> client.execute(null, mockRequest));

        Mockito.verify(mockRequest, Mockito.times(0))
               .setHeader(Mockito.anyString(), Mockito.anyString());

        Assertions.assertEquals(200, result.getStatusLine()
                                           .getStatusCode());
    }

    @Test
    public void testExecuteSimpleRequestWithNoRetryShouldReturnTheResponse() throws IOException {
        HttpClient mockHttpClient = HttpClientMockBuilder.builder()
                                                         .response(HttpClientResponseMockBuilder.builder()
                                                                                                .response("dummy-response")
                                                                                                .statusCodes(200)
                                                                                                .firstHeader(CsrfHttpClient.CSRF_TOKEN_HEADER_NAME,
                                                                                                             DUMMY_TOKEN)
                                                                                                .build())
                                                         .build();

        HttpRequest mockRequest = HttpClientRequestMockBuilder.builder()
                                                              .method(HttpPost.METHOD_NAME)
                                                              .build();

        HttpResponse result = getResultFromExecution(mockHttpClient, "dummy", mockRequest, (client) -> client.execute(null, mockRequest));

        Mockito.verify(mockRequest)
               .setHeader(CsrfHttpClient.CSRF_TOKEN_HEADER_NAME, DUMMY_TOKEN);

        Assertions.assertEquals(200, result.getStatusLine()
                                           .getStatusCode());
    }

    @Test
    public void testExecuteSimpleRequestWithRetryNeeded() throws IOException {
        HttpClient mockHttpClient = HttpClientMockBuilder.builder()
                                                         .response(HttpClientResponseMockBuilder.builder()
                                                                                                .response("dummy-response")
                                                                                                .statusCodes(403, 200)
                                                                                                .firstHeader(CsrfHttpClient.CSRF_TOKEN_HEADER_NAME,
                                                                                                             DUMMY_TOKEN, "Required",
                                                                                                             DUMMY_TOKEN_2)
                                                                                                .build())
                                                         .build();

        HttpRequest mockRequest = HttpClientRequestMockBuilder.builder()
                                                              .method(HttpPost.METHOD_NAME)
                                                              .build();

        HttpResponse result = getResultFromExecution(mockHttpClient, "dummy", mockRequest, (client) -> client.execute(null, mockRequest));

        Mockito.verify(mockRequest)
               .setHeader(CsrfHttpClient.CSRF_TOKEN_HEADER_NAME, DUMMY_TOKEN);

        Mockito.verify(mockRequest)
               .setHeader(CsrfHttpClient.CSRF_TOKEN_HEADER_NAME, DUMMY_TOKEN_2);

        Assertions.assertEquals(200, result.getStatusLine()
                                           .getStatusCode());
    }

    @Test
    public void testExecuteWithContext() throws IOException {
        HttpClient mockHttpClient = HttpClientMockBuilder.builder()
                                                         .response(HttpClientResponseMockBuilder.builder()
                                                                                                .response("dummy-response")
                                                                                                .statusCodes(200)
                                                                                                .build())
                                                         .build();

        HttpRequest mockRequest = HttpClientRequestMockBuilder.builder()
                                                              .method(HttpGet.METHOD_NAME)
                                                              .build();

        HttpResponse result = getResultFromExecution(mockHttpClient, "dummy", mockRequest,
                                                     (client) -> client.execute(null, mockRequest, (HttpContext) null));

        Mockito.verify(mockRequest, Mockito.times(0))
               .setHeader(Mockito.anyString(), Mockito.anyString());

        Assertions.assertEquals(200, result.getStatusLine()
                                           .getStatusCode());
    }

    @Test
    public void testExecuteWithTarget() throws IOException {
        HttpClient mockHttpClient = HttpClientMockBuilder.builder()
                                                         .response(HttpClientResponseMockBuilder.builder()
                                                                                                .response("dummy-response")
                                                                                                .statusCodes(200)
                                                                                                .build())
                                                         .build();

        HttpRequest mockRequest = HttpClientRequestMockBuilder.builder()
                                                              .method(HttpGet.METHOD_NAME)
                                                              .build();

        HttpResponse result = getResultFromExecution(mockHttpClient, "dummy", mockRequest,
                                                     (client) -> client.execute(null, mockRequest, (HttpContext) null));

        Mockito.verify(mockRequest, Mockito.times(0))
               .setHeader(Mockito.anyString(), Mockito.anyString());

        Assertions.assertEquals(200, result.getStatusLine()
                                           .getStatusCode());
    }

    @Test
    public void testExecuteWithResponseHandler() throws IOException {
        HttpClient mockHttpClient = HttpClientMockBuilder.builder()
                                                         .response(HttpClientResponseMockBuilder.builder()
                                                                                                .response("dummy-response")
                                                                                                .statusCodes(200)
                                                                                                .build(),
                                                                   "this-is-test")
                                                         .build();

        HttpRequest mockRequest = HttpClientRequestMockBuilder.builder()
                                                              .method(HttpGet.METHOD_NAME)
                                                              .build();

        String result = getResultFromExecution(mockHttpClient, mockRequest,
                                               (client) -> client.execute(null, mockRequest, HttpResponse -> null));

        Assertions.assertEquals("this-is-test", result);
    }

    @Test
    public void testWithNoUrlSet() throws IOException {
        HttpClient mockHttpClient = HttpClientMockBuilder.builder()
                                                         .response(HttpClientResponseMockBuilder.builder()
                                                                                                .response("dummy-response")
                                                                                                .statusCodes(200)
                                                                                                .build())
                                                         .build();

        HttpRequest mockRequest = HttpClientRequestMockBuilder.builder()
                                                              .method(HttpPost.METHOD_NAME)
                                                              .build();

        HttpResponse result = getResultFromExecution(mockHttpClient, mockRequest, (client) -> client.execute(null, mockRequest));

        Assertions.assertNotNull(result);

        Mockito.verify(mockRequest, Mockito.times(0))
               .setHeader(Mockito.anyString(), Mockito.anyString());
    }

    private <T> T getResultFromExecution(HttpClient mockHttpClient, HttpRequest mockRequest, TestHttpExecutor<T> executor)
        throws IOException, ClientProtocolException {
        return getResultFromExecution(mockHttpClient, null, mockRequest, executor);
    }

    private <T> T getResultFromExecution(HttpClient mockHttpClient, String csrfUrl, HttpRequest mockRequest, TestHttpExecutor<T> executor)
        throws IOException, ClientProtocolException {
        try (CsrfHttpClient client = new CsrfHttpClient(mockHttpClient, csrfUrl, Collections.emptyMap())) {
            return executor.execute(client);
        }
    }

    interface TestHttpExecutor<T> {

        T execute(CsrfHttpClient client) throws IOException;

    }

}
