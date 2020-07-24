package org.cloudfoundry.multiapps.controller.core.cf.clients;

import java.io.IOException;
import java.net.URI;

import javax.inject.Named;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.util.RestUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Named
public class RestTemplateFactory {

    public RestTemplate getRestTemplate(CloudControllerClient client) {
        RestTemplate restTemplate = new RestUtil().createRestTemplate(null, false);
        restTemplate.setRequestFactory(new HttpRequestFactory(restTemplate.getRequestFactory(), client));
        return restTemplate;
    }

    private static class HttpRequestFactory implements ClientHttpRequestFactory {

        private final ClientHttpRequestFactory requestFactory;
        private final CloudControllerClient client;

        public HttpRequestFactory(ClientHttpRequestFactory requestFactory, CloudControllerClient client) {
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
            return client.login()
                         .toString();
        }

    }
}
