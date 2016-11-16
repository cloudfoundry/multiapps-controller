package com.sap.cloud.lm.sl.cf.core.helpers;

import java.io.IOException;
import java.net.URI;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class RestTemplateFactory {

    public RestTemplate getRestTemplate(CloudFoundryOperations client) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(new HttpRequestFactory(restTemplate.getRequestFactory(), client));
        return restTemplate;
    }

    private class HttpRequestFactory implements ClientHttpRequestFactory {

        private ClientHttpRequestFactory requestFactory;
        private CloudFoundryOperations client;

        public HttpRequestFactory(ClientHttpRequestFactory requestFactory, CloudFoundryOperations client) {
            this.requestFactory = requestFactory;
            this.client = client;
        }

        @Override
        public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
            ClientHttpRequest request = requestFactory.createRequest(uri, httpMethod);
            HttpHeaders requestHeaders = request.getHeaders();
            if (!requestHeaders.containsKey(HttpHeaders.AUTHORIZATION)) {
                requestHeaders.add(HttpHeaders.AUTHORIZATION, "Bearer " + computeAuthorizationToken());
            }
            return request;
        }

        private String computeAuthorizationToken() {
            return client.login().toString();
        }

    }
}
