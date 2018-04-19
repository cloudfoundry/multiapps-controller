package com.sap.cloud.lm.sl.cf.core.cf;

import java.io.IOException;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import com.sap.cloud.lm.sl.cf.core.util.Configuration;

final class TaggingRequestInterceptor implements ClientHttpRequestInterceptor {
    private static final String SOURCE = "source";
    private final String headerValue;

    TaggingRequestInterceptor(String org, String space) {
        this.headerValue = getHeaderValue(org, space);
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        request.getHeaders()
            .add(SOURCE, headerValue);
        return execution.execute(request, body);
    }

    private String getHeaderValue(String org, String space) {
        StringBuilder headerValueBuilder = new StringBuilder("MTA deploy-service v.");
        headerValueBuilder.append(Configuration.getInstance()
            .getVersion());
        if (org != null && space != null) {
            headerValueBuilder.append(", for org: ");
            headerValueBuilder.append(org);
            headerValueBuilder.append(" space: ");
            headerValueBuilder.append(space);
        }
        final String headerValue = headerValueBuilder.toString();
        return headerValue;
    }
}