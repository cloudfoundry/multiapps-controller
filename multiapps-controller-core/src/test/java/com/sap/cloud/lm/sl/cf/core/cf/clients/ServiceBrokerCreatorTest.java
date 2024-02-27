package com.sap.cloud.lm.sl.cf.core.cf.clients;

import java.net.URL;
import java.util.Arrays;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.RestTemplate;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceBrokerExtended;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil.Expectation;

@RunWith(Parameterized.class)
public class ServiceBrokerCreatorTest {

    private static final String CONTROLLER_URL = "https://api.cf.sap.hana.ondemand.com";
    private static final String SERVICE_BROKERS_ENDPOINT = "/v2/service_brokers";

    @Mock
    private CloudControllerClient client;
    @Mock
    private RestTemplateFactory restTemplateFactory;
    @Mock
    private RestTemplate restTemplate;

    private ServiceBrokerCreator serviceBrokerCreator;

    private String serviceBrokerToCreateJsonLocation;
    private CloudServiceBrokerExtended serviceBrokerToCreate;
    private Expectation expectation;

    public ServiceBrokerCreatorTest(String serviceBrokerToCreateJsonLocation, Expectation expectation) {
        this.serviceBrokerToCreateJsonLocation = serviceBrokerToCreateJsonLocation;
        this.expectation = expectation;
    }

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0)
            {
                "space-scoped-service-broker.json", new Expectation(Expectation.Type.RESOURCE, "space-scoped-service-broker-creation-request.json"),
            },
            // (1)
            {
                "global-service-broker.json", new Expectation(Expectation.Type.RESOURCE, "global-service-broker-creation-request.json"),
            },
            // (2)
            {
                "service-broker-with-missing-name.json", new Expectation(Expectation.Type.EXCEPTION, "The service broker's name must not be null!")
            },
            // (3)
            {
                "service-broker-with-missing-username.json", new Expectation(Expectation.Type.EXCEPTION, "The service broker's username must not be null!")
            },
            // (4)
            {
                "service-broker-with-missing-password.json", new Expectation(Expectation.Type.EXCEPTION, "The service broker's password must not be null!")
            },
            // (5)
            {
                "service-broker-with-missing-url.json", new Expectation(Expectation.Type.EXCEPTION, "The service broker's URL must not be null!")
            },
// @formatter:on
        });
    }

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        this.serviceBrokerCreator = new ServiceBrokerCreator(restTemplateFactory);
        String serviceBrokerToCreateJson = TestUtil.getResourceAsString(serviceBrokerToCreateJsonLocation, getClass());
        this.serviceBrokerToCreate = JsonUtil.fromJson(serviceBrokerToCreateJson, CloudServiceBrokerExtended.class);

        Mockito.when(client.getCloudControllerUrl())
               .thenReturn(new URL(CONTROLLER_URL));
        Mockito.when(restTemplateFactory.getRestTemplate(client))
               .thenReturn(restTemplate);
    }

    @Test
    public void testCreateServiceBroker() {
        TestUtil.test(() -> {
            serviceBrokerCreator.createServiceBroker(client, serviceBrokerToCreate);

            ArgumentCaptor<Object> requestCaptor = ArgumentCaptor.forClass(Object.class);
            Mockito.verify(restTemplate)
                   .postForObject(Mockito.eq(CONTROLLER_URL + SERVICE_BROKERS_ENDPOINT), requestCaptor.capture(), Mockito.eq(String.class));

            return requestCaptor.getValue();
        }, expectation, getClass());
    }

}
