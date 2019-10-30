package com.sap.cloud.lm.sl.cf.core.util;

import org.apache.http.HttpRequest;
import org.apache.http.RequestLine;
import org.mockito.Mockito;

public class HttpClientRequestMockBuilder {

    private final HttpRequest mockRequest = Mockito.mock(HttpRequest.class);

    public static HttpClientRequestMockBuilder builder() {
        return new HttpClientRequestMockBuilder();
    }

    public HttpClientRequestMockBuilder method(String method) {
        RequestLine requestLineMock = Mockito.mock(RequestLine.class);
        Mockito.when(requestLineMock.getMethod())
               .thenReturn(method);
        Mockito.when(mockRequest.getRequestLine())
               .thenReturn(requestLineMock);
        return this;
    }

    public HttpRequest build() {
        return mockRequest;
    }
}
