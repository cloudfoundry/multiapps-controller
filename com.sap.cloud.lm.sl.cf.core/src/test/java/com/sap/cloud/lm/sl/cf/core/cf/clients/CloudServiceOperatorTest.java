package com.sap.cloud.lm.sl.cf.core.cf.clients;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.CloudServiceOffering;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

public abstract class CloudServiceOperatorTest {

    private static final String CONTROLLER_URL = "https://api.cf.sap.hana.ondemand.com";
    private static final String SERVICE_OFFERINGS_RESPONSE_PATH = "service-offerings.json";
    private static final String SERVICE_FILE_NAME = "service.json";

    @Mock
    private RestTemplate restTemplate;
    @Mock
    private RestTemplateFactory restTemplateFactory;
    @Mock
    private CloudControllerClient client;

    protected static String getControllerUrl() {
        return CONTROLLER_URL;
    }

    @BeforeEach
    public void prepareClients() throws IOException {
        MockitoAnnotations.initMocks(this);
        prepareRestTemplateFactory();
        prepareClient();
    }

    private void prepareRestTemplateFactory() {
        Mockito.when(restTemplateFactory.getRestTemplate(client))
               .thenReturn(restTemplate);
    }

    private void prepareClient() throws IOException {
        URL controllerUrl = new URL(CONTROLLER_URL);
        Mockito.when(client.getCloudControllerUrl())
               .thenReturn(controllerUrl);

        List<CloudServiceOffering> serviceOfferings = loadServiceOfferingsFromFile(SERVICE_OFFERINGS_RESPONSE_PATH);
        Mockito.when(client.getServiceOfferings())
               .thenReturn(serviceOfferings);
        CloudService cloudService = loadServiceFromFile(SERVICE_FILE_NAME);
        Mockito.when(client.getServices())
               .thenReturn(Arrays.asList(cloudService));
    }

    private List<CloudServiceOffering> loadServiceOfferingsFromFile(String filePath) throws IOException {
        String serviceOfferingsJson = TestUtil.getResourceAsString(filePath, getClass());
        return JsonUtil.fromJson(serviceOfferingsJson, new TypeReference<List<CloudServiceOffering>>() {
        });
    }

    private CloudService loadServiceFromFile(String filePath) {
        String cloudServiceJson = TestUtil.getResourceAsString(filePath, getClass());
        return JsonUtil.fromJson(cloudServiceJson, new TypeReference<CloudService>() {
        });
    }

    protected RestTemplate getMockedRestTemplate() {
        return restTemplate;
    }

    protected RestTemplateFactory getMockedRestTemplateFactory() {
        return restTemplateFactory;
    }

    protected CloudControllerClient getMockedClient() {
        return client;
    }

}
