package com.sap.cloud.lm.sl.cf.core.cf.clients;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.RestTemplate;

import com.sap.cloud.lm.sl.common.util.TestUtil;

public abstract class CloudServiceOperatorTest {

    private static final String CONTROLLER_URL = "https://api.cf.sap.hana.ondemand.com";
    private static final String SERVICES_ENDPOINT = "/v2/services?inline-relations-depth=1";

    private static final String SERVICE_OFFERINGS_RESPONSE_PATH = "service-offerings.json";

    @Mock
    private RestTemplate restTemplate;
    @Mock
    private RestTemplateFactory restTemplateFactory;
    @Mock
    private CloudFoundryOperations client;

    @Before
    public void prepareClients() throws IOException {
        MockitoAnnotations.initMocks(this);
        prepareRestTemplate();
        prepareRestTemplateFactory();
        prepareClient();
    }

    private void prepareRestTemplate() throws IOException {
        String serviceOfferingsResponse = getResourceAsString(SERVICE_OFFERINGS_RESPONSE_PATH);
        String serviceOfferingsUrl = CONTROLLER_URL + SERVICES_ENDPOINT;
        Mockito.when(restTemplate.getForObject(serviceOfferingsUrl, String.class, Collections.emptyMap()))
            .thenReturn(serviceOfferingsResponse);
    }

    private String getResourceAsString(String serviceOfferingsResponsePath) throws IOException {
        return TestUtil.getResourceAsString(serviceOfferingsResponsePath, getClass());
    }

    private void prepareRestTemplateFactory() {
        Mockito.when(restTemplateFactory.getRestTemplate(client))
            .thenReturn(restTemplate);
    }

    private void prepareClient() throws MalformedURLException {
        URL controllerUrl = new URL(CONTROLLER_URL);
        Mockito.when(client.getCloudControllerUrl())
            .thenReturn(controllerUrl);
    }

    protected static String getControllerUrl() {
        return CONTROLLER_URL;
    }

    protected RestTemplate getMockedRestTemplate() {
        return restTemplate;
    }

    protected RestTemplateFactory getMockedRestTemplateFactory() {
        return restTemplateFactory;
    }

    protected CloudFoundryOperations getMockedClient() {
        return client;
    }

}
