package org.cloudfoundry.multiapps.controller.core.test;

import java.io.IOException;
import java.util.List;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.cloudfoundry.multiapps.common.Nullable;
import org.immutables.value.Value;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

@Value.Immutable
public abstract class HttpClientMock {

    public abstract List<HttpResponse> getResponses();

    @Nullable
    public abstract Throwable getException();

    @Value.Derived
    public CloseableHttpClient getMock() {
        try {
            CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
            if (getException() != null) {
                Mockito.when(httpClient.execute(ArgumentMatchers.any()))
                       .thenThrow(getException());
                return httpClient;
            }

            if (getResponses().isEmpty()) {
                throw new IllegalArgumentException("At least one response must be configured!");
            }

            Answer<HttpResponse> answer = createAnswer(getResponses());

            Mockito.when(httpClient.execute(ArgumentMatchers.any()))
                   .thenAnswer(answer);
            Mockito.when(httpClient.execute(ArgumentMatchers.<HttpHost> any(), ArgumentMatchers.any()))
                   .thenAnswer(answer);
            Mockito.when(httpClient.execute(ArgumentMatchers.<HttpHost> any(), ArgumentMatchers.any(),
                                            ArgumentMatchers.<HttpContext> any()))
                   .thenAnswer(answer);
            Mockito.when(
                       httpClient.execute(ArgumentMatchers.<ClassicHttpRequest> any(), ArgumentMatchers.<HttpClientResponseHandler<Object>> any()))
                   .thenAnswer(answer);

            return httpClient;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Answer<HttpResponse> createAnswer(List<HttpResponse> responses) {
        return new Answer<HttpResponse>() {

            private int invocations = -1;

            @Override
            public HttpResponse answer(InvocationOnMock invocation) {
                invocations++;
                if (invocations >= responses.size()) {
                    return responses.get(responses.size() - 1);
                }
                return responses.get(invocations);
            }

        };
    }

}
