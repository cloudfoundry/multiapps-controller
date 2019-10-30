package com.sap.cloud.lm.sl.cf.core.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.mockito.Mockito;

public class HttpClientResponseMockBuilder {

    private final CloseableHttpResponse mockedResponse = Mockito.mock(CloseableHttpResponse.class);

    public static HttpClientResponseMockBuilder builder() {
        return new HttpClientResponseMockBuilder();
    }

    public HttpClientResponseMockBuilder statusCodes(Integer... statusCodes) {
        StatusLine statusLineMock = Mockito.mock(StatusLine.class);

        Mockito.when(mockedResponse.getStatusLine())
               .thenReturn(statusLineMock);

        Integer[] restStatusCodes = Arrays.copyOfRange(statusCodes, 1, statusCodes.length);

        Mockito.when(statusLineMock.getStatusCode())
               .thenReturn(statusCodes[0], restStatusCodes);

        return this;
    }

    public HttpClientResponseMockBuilder response(String response) throws UnsupportedOperationException, IOException {
        HttpEntity httpEntityMock = Mockito.mock(HttpEntity.class);
        Mockito.when(mockedResponse.getEntity())
               .thenReturn(httpEntityMock);
        Mockito.when(httpEntityMock.getContent())
               .thenReturn(new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8)));
        return this;
    }

    public HttpClientResponseMockBuilder firstHeader(String headerName, String... headerValues) {
        Mockito.when(mockedResponse.containsHeader(headerName))
               .thenReturn(Boolean.TRUE);
        Header mockedHeader = Mockito.mock(Header.class);
        String[] restHeaderValues = Arrays.copyOfRange(headerValues, 1, headerValues.length);
        Mockito.when(mockedHeader.getValue())
               .thenReturn(headerValues[0], restHeaderValues);
        Mockito.when(mockedResponse.getFirstHeader(headerName))
               .thenReturn(mockedHeader);
        return this;
    }

    public CloseableHttpResponse build() {
        return mockedResponse;
    }

}
