package com.sap.cloud.lm.sl.cf.core.cf.clients;

import java.net.URL;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.RestTemplate;

import com.sap.cloud.lm.sl.common.util.TestUtil;

public class ServiceBrokersGetterTest {

    private static final String CONTROLLER_URL = "https://api.cf.sap.hana.ondemand.com";
    private static final String SERVICE_BROKERS_ENDPOINT = "/v2/service_brokers";

    @Mock
    private CloudFoundryOperations client;
    @Mock
    private RestTemplateFactory restTemplateFactory;
    @Mock
    private RestTemplate restTemplate;
    @InjectMocks
    private ServiceBrokersGetter serviceBrokersGetter = new ServiceBrokersGetter();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        String getServiceBrokersResponse = TestUtil.getResourceAsString("valid-get-service-brokers-response.json", getClass());
        Mockito.when(client.getCloudControllerUrl()).thenReturn(new URL(CONTROLLER_URL));
        Mockito.when(restTemplateFactory.getRestTemplate(client)).thenReturn(restTemplate);
        Mockito.when(restTemplate.getForObject(CONTROLLER_URL + SERVICE_BROKERS_ENDPOINT, String.class)).thenReturn(
            getServiceBrokersResponse);
    }

    @Test
    public void testGetServiceBrokers() {
        TestUtil.test(() -> serviceBrokersGetter.getServiceBrokers(client), "R:service-brokers.json", getClass());
    }

}
