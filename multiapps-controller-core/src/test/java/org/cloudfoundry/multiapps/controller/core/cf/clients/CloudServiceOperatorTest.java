package org.cloudfoundry.multiapps.controller.core.cf.clients;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudServiceOffering;
import org.cloudfoundry.multiapps.common.test.TestUtil;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.reactive.function.client.WebClient.RequestBodyUriSpec;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;

import com.fasterxml.jackson.core.type.TypeReference;

import reactor.core.publisher.Mono;

public abstract class CloudServiceOperatorTest {

    private static final String CONTROLLER_URL = "https://api.cf.sap.hana.ondemand.com";
    private static final String SERVICE_OFFERINGS_RESPONSE_PATH = "service-offerings.json";

    @Mock
    private WebClient webClient;
    @Mock
    private RequestBodyUriSpec requestBodyUriSpec;
    @Mock
    private RequestBodySpec requestBodySpec;
    @Mock
    private RequestHeadersSpec requestHeadersSpec;
    @Mock
    private Mono<ClientResponse> clientResponse;
    @Mock
    private WebClientFactory webClientFactory;
    @Mock
    private CloudControllerClient client;

    protected static String getControllerUrl() {
        return CONTROLLER_URL;
    }

    @BeforeEach
    public void prepareClients() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        prepareWebClient();
        prepareRequesBodyUriSpec();
        prepareRequestBodySpec();
        prepareRequestHeadersSpec();
        prepareWebClientFactory();
        prepareClient();
    }

    private void prepareWebClient() {
        Mockito.when(webClient.put())
               .thenReturn(requestBodyUriSpec);
    }

    private void prepareRequesBodyUriSpec() {
        Mockito.when(requestBodyUriSpec.uri(Mockito.anyString()))
               .thenReturn(requestBodySpec);
    }

    private void prepareRequestBodySpec() {
        Mockito.when(requestBodySpec.bodyValue(Mockito.any()))
               .thenReturn(requestHeadersSpec);
    }

    private void prepareRequestHeadersSpec() {
        Mockito.when(requestHeadersSpec.exchange())
               .thenReturn(clientResponse);
    }

    private void prepareWebClientFactory() {
        Mockito.when(webClientFactory.getWebClient(client))
               .thenReturn(webClient);
    }

    private void prepareClient() throws IOException {
        URL controllerUrl = new URL(CONTROLLER_URL);
        Mockito.when(client.getCloudControllerUrl())
               .thenReturn(controllerUrl);

        List<CloudServiceOffering> serviceOfferings = loadServiceOfferingsFromFile(SERVICE_OFFERINGS_RESPONSE_PATH);
        Mockito.when(client.getServiceOfferings())
               .thenReturn(serviceOfferings);
    }

    private List<CloudServiceOffering> loadServiceOfferingsFromFile(String filePath) {
        String serviceOfferingsJson = TestUtil.getResourceAsString(filePath, getClass());
        return JsonUtil.fromJson(serviceOfferingsJson, new TypeReference<List<CloudServiceOffering>>() {
        });
    }

    protected WebClient getMockedWebClient() {
        return webClient;
    }

    protected RequestBodyUriSpec getMockedRequestBodyUriSpec() {
        return requestBodyUriSpec;
    }

    protected RequestBodySpec getMockedRequestBodySpec() {
        return requestBodySpec;
    }

    protected Mono<ClientResponse> getMockedClientResponse() {
        return clientResponse;
    }

    protected RequestHeadersSpec getMockedRequestHeadersSpec() {
        return requestHeadersSpec;
    }

    protected WebClientFactory getMockedWebClientFactory() {
        return webClientFactory;
    }

    protected CloudControllerClient getMockedClient() {
        return client;
    }

}
