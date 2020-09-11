package org.cloudfoundry.multiapps.controller.core.test;

import java.util.function.UnaryOperator;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;

public class HttpMocks {

    private HttpMocks() {
    }

    public static CloseableHttpClient mockClient(UnaryOperator<ImmutableHttpClientMock.Builder> configurer) {
        HttpClientMock mock = configurer.apply(ImmutableHttpClientMock.builder())
                                        .build();
        return mock.getMock();
    }

    public static CloseableHttpResponse mockResponse(UnaryOperator<ImmutableHttpResponseMock.Builder> configurer) {
        HttpResponseMock mock = configurer.apply(ImmutableHttpResponseMock.builder())
                                          .build();
        return mock.getMock();
    }

}
