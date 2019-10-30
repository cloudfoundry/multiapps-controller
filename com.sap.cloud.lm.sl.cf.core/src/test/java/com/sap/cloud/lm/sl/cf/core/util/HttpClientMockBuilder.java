package com.sap.cloud.lm.sl.cf.core.util;

import java.io.IOException;

import org.apache.http.HttpHost;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HttpContext;
import org.mockito.Mockito;

public class HttpClientMockBuilder {

    private final CloseableHttpClient httpClientMock = Mockito.mock(CloseableHttpClient.class);

    public static HttpClientMockBuilder builder() {
        return new HttpClientMockBuilder();
    }

    public HttpClientMockBuilder executeWithException(Throwable exception) throws ClientProtocolException, IOException {
        Mockito.when(httpClientMock.execute(Mockito.any()))
               .thenThrow(exception);
        return this;
    }

    public HttpClientMockBuilder response(CloseableHttpResponse response) throws ClientProtocolException, IOException {
        return response(response, null);
    }

    public HttpClientMockBuilder response(CloseableHttpResponse response, String responseHandlerReturnValue)
        throws ClientProtocolException, IOException {
        Mockito.when(httpClientMock.execute(Mockito.any()))
               .thenReturn(response);

        Mockito.when(httpClientMock.execute(Mockito.<HttpHost> any(), Mockito.any()))
               .thenReturn(response);

        Mockito.when(httpClientMock.execute(Mockito.<HttpHost> any(), Mockito.any(), Mockito.<HttpContext> any()))
               .thenReturn(response);

        Object returnObject = getResponseReturnObject(response, responseHandlerReturnValue);
        Mockito.when(httpClientMock.execute(Mockito.any(), Mockito.any(), Mockito.<ResponseHandler> any()))
               .thenReturn(returnObject);

        return this;
    }

    private Object getResponseReturnObject(CloseableHttpResponse response, String responseHandlerReturnValue) {
        if (responseHandlerReturnValue != null) {
            return responseHandlerReturnValue;
        }

        return response;
    }

    public CloseableHttpClient build() {
        return httpClientMock;
    }
}
