package org.cloudfoundry.multiapps.controller.core.cf;

import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;

import reactor.core.publisher.Mono;

class TaggingRequestFilterFunction implements ExchangeFilterFunction {

    public static final String TAG_HEADER_SPACE_NAME = "source-space";
    public static final String TAG_HEADER_ORG_NAME = "source-org";
    public static final String TAG_HEADER_NAME = "source";
    public static final String TAG_HEADER_CORRELATION_ID = "x-correlation-id";
    private final String headerValue;
    private final String orgHeaderValue;
    private final String spaceHeaderValue;
    private final String correlationIdHeaderValue;

    TaggingRequestFilterFunction(String deployServiceVersion) {
        this(deployServiceVersion, null, null, null);
    }

    TaggingRequestFilterFunction(String deployServiceVersion, String org, String space, String correlationId) {
        this.headerValue = getHeaderValue(deployServiceVersion);
        this.orgHeaderValue = org;
        this.spaceHeaderValue = space;
        this.correlationIdHeaderValue = correlationId;
    }

    String getHeaderValue(String deployServiceVersion) {
        return "MTA deploy-service v" + deployServiceVersion;
    }

    @Override
    public Mono<ClientResponse> filter(ClientRequest clientRequest, ExchangeFunction nextFilter) {
        HttpHeaders headers = clientRequest.headers();
        setHeader(headers, TAG_HEADER_NAME, headerValue);
        if (orgHeaderValue != null && spaceHeaderValue != null) {
            setHeader(headers, TAG_HEADER_ORG_NAME, orgHeaderValue);
            setHeader(headers, TAG_HEADER_SPACE_NAME, spaceHeaderValue);
        }
        if (correlationIdHeaderValue != null) {
            setHeader(headers, TAG_HEADER_CORRELATION_ID, correlationIdHeaderValue);
        }
        return nextFilter.exchange(clientRequest);
    }

    private void setHeader(HttpHeaders headers, String tagHeaderName, String headerValue) {
        if (headers.containsKey(tagHeaderName)) {
            return;
        }
        headers.add(tagHeaderName, headerValue);
    }

}