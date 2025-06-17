package org.cloudfoundry.multiapps.controller.core.test;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicHeader;
import org.immutables.value.Value;
import org.mockito.Mockito;

@Value.Immutable
public abstract class HttpResponseMock {

    abstract int getStatusCode();

    abstract Map<String, String> getHeaders();

    abstract String getBody();

    @Value.Derived
    public CloseableHttpResponse getMock() {
        CloseableHttpResponse response = Mockito.mock(CloseableHttpResponse.class);
        Mockito.when(response.getCode())
               .thenReturn(getStatusCode());
        Mockito.when(response.getEntity())
               .thenReturn(createHttpEntity(getBody()));
        mockHeaders(response);
        return response;
    }

    private static HttpEntity createHttpEntity(String body) {
        return new StringEntity(body, StandardCharsets.UTF_8);
    }

    private void mockHeaders(HttpResponse response) {
        for (Entry<String, String> header : getHeaders().entrySet()) {
            Mockito.when(response.containsHeader(header.getKey()))
                   .thenReturn(true);
            Mockito.when(response.getFirstHeader(header.getKey()))
                   .thenReturn(createHeader(header));
        }
    }

    private static Header createHeader(Entry<String, String> header) {
        return new BasicHeader(header.getKey(), header.getValue());
    }

}
