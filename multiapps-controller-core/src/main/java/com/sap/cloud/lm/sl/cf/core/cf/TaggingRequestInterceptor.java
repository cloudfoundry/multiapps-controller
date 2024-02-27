package com.sap.cloud.lm.sl.cf.core.cf;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

class TaggingRequestInterceptor implements ClientHttpRequestInterceptor {

    public static final String TAG_HEADER_SPACE_NAME = "source-space";
    public static final String TAG_HEADER_ORG_NAME = "source-org";
    public static final String TAG_HEADER_NAME = "source";
    private final String headerValue;
    private String orgHeaderValue;
    private String spaceHeaderValue;

    TaggingRequestInterceptor(String deployServiceVersion) {
        this(deployServiceVersion, null, null);
    }

    TaggingRequestInterceptor(String deployServiceVersion, String org, String space) {
        this.headerValue = getHeaderValue(deployServiceVersion);
        this.orgHeaderValue = org;
        this.spaceHeaderValue = space;
    }

    String getHeaderValue(String deployServiceVersion) {
        return "MTA deploy-service v" + deployServiceVersion;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        HttpHeaders headers = request.getHeaders();
        setHeader(headers, TAG_HEADER_NAME, headerValue);
        if (orgHeaderValue != null && spaceHeaderValue != null) {
            setHeader(headers, TAG_HEADER_ORG_NAME, orgHeaderValue);
            setHeader(headers, TAG_HEADER_SPACE_NAME, spaceHeaderValue);
        }
        return execution.execute(request, body);
    }

    private void setHeader(HttpHeaders headers, String tagHeaderName, String headerValue) {
        if (headers.containsKey(tagHeaderName)) {
            return;
        }
        headers.add(tagHeaderName, headerValue);
    }

}