package org.cloudfoundry.multiapps.controller.client.facade.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

final class MockControllerClientFactory {

    static final String BASE_URL = "https://api.cf.test.base.com";

    private final CloudControllerV3Client client;
    private final MockRestServiceServer server;

    private MockControllerClientFactory(CloudControllerV3Client client, MockRestServiceServer server) {
        this.client = client;
        this.server = server;
    }

    static MockControllerClientFactory create() {
        RestClient.Builder builder = RestClient.builder()
                                               .baseUrl(BASE_URL)
                                               .messageConverters(converters -> {
                                                   converters.clear();
                                                   converters.add(new ByteArrayHttpMessageConverter());
                                                   converters.add(new StringHttpMessageConverter());
                                                   converters.add(new MappingJackson2HttpMessageConverter(new ObjectMapper()));
                                               })
                                               .defaultStatusHandler(new CloudControllerResponseErrorHandler());
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder)
                                                            .build();

        CloudControllerV3Client client = new CloudControllerV3Client(builder.build());
        return new MockControllerClientFactory(client, server);
    }

    CloudControllerV3Client client() {
        return client;
    }

    MockRestServiceServer server() {
        return server;
    }

    void verify() {
        server.verify();
    }

}
