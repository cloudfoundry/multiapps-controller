package com.sap.cloud.lm.sl.cf.core.cf.clients;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudEntity.Meta;
import org.cloudfoundry.client.lib.domain.CloudServiceOffering;
import org.cloudfoundry.client.lib.domain.CloudServicePlan;
import org.cloudfoundry.client.lib.util.CloudEntityResourceMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.common.ParsingException;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Parameterized.class)
public class ServiceCreatorTest {
    protected static final String CONTROLLER_ENDPOINT = "https://api.cf.sap.com";
    protected static final String SPACE_ID = "TEST_SPACE";
    protected static final UUID SERVICE_PLAN_GUID = UUID.randomUUID();
    protected static final String SERVICE_NAME = "name";
    protected static final String SPACE_GUID = "space_guid";
    protected static final String SERVICE_PARAMETERS = "parameters";
    protected static final String CREATE_SERVICE_URL = "/v2/service_instances?accepts_incomplete=false";
    protected static final String SERVICE_PLAN = "test-plan";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) Service with credentials 
            {
                "service-01.json", null, null
            },
            // (1) Service without credentials
            {
                "service-02.json", null, null
            },
            // (2) Service doesn't specify label
            {
                "service-03.json", "Service label must not be null", IllegalArgumentException.class
            },
            // (3) Service doesn't specify name
            {
                "service-04.json", "Service name must not be null", IllegalArgumentException.class
            },
            // (4) Service doesn't specify plan
            {
                "service-05.json", "Service plan must not be null", IllegalArgumentException.class
            },
            // (5) Service plan doesn't exist
            {
                "service-06.json", "Service plan different-plan for service test-service not found", SLException.class
            }
// @formatter:on
        });
    }

    @Mock
    protected CloudEntityResourceMapper resourceMapper;

    @Mock
    protected RestTemplate restTemplate;

    @Mock
    private RestTemplateFactory restTemplateFactory;

    @Mock
    protected CloudFoundryOperations client;

    @InjectMocks
    private ServiceCreator serviceCreator = new ServiceCreator() {
        @Override
        protected CloudEntityResourceMapper getResourceMapper() {
            return resourceMapper;
        }
    };

    protected StepInput input;
    private String expectedExceptionMessage;
    private Class<? extends RuntimeException> expectedExceptionClass;

    public ServiceCreatorTest(String inputLocation, String expected, Class<? extends RuntimeException> expectedExceptionClass)
        throws ParsingException, IOException {
        this.input = JsonUtil.fromJson(TestUtil.getResourceAsString(inputLocation, ServiceCreatorTest.class), getStepinput());
        this.expectedExceptionMessage = expected;
        this.expectedExceptionClass = expectedExceptionClass;
    }

    @Before
    public void setUp() throws MalformedURLException {

        MockitoAnnotations.initMocks(this);
        setUpException();
        CloudServiceOffering offering = new CloudServiceOffering(null, getServiceLabel());
        offering.addCloudServicePlan(new CloudServicePlan(new Meta(SERVICE_PLAN_GUID, null, null), getCloudServicePlan()));

        Map<String, Object> resourceMap = new HashMap<>();
        List<Map<String, Object>> resourcesList = new ArrayList<>();
        resourcesList.add(new HashMap<>());
        resourceMap.put("resources", resourcesList);
        Mockito.when(resourceMapper.mapResource(new HashMap<>(), CloudServiceOffering.class)).thenReturn(offering);

        Mockito.when(client.getCloudControllerUrl()).thenReturn(new URL(CONTROLLER_ENDPOINT));
        Mockito.when(restTemplate.getForObject(getUrl("/v2/services?inline-relations-depth=1", new URL(CONTROLLER_ENDPOINT)),
            String.class)).thenReturn(org.cloudfoundry.client.lib.util.JsonUtil.convertToJson(resourceMap));

        Mockito.when(restTemplateFactory.getRestTemplate(client)).thenReturn(restTemplate);
    }

    protected String getServiceLabel() {
        return input.getService().getLabel();
    }

    protected String getCloudServicePlan() {
        return SERVICE_PLAN;
    }

    protected String getUrl(String path, URL cloudControllerUrl) {
        return cloudControllerUrl + (path.startsWith("/") ? path : "/" + path);
    }

    private void setUpException() {
        if (expectedExceptionMessage != null) {
            expectedException.expect(getExpectedExceptionClass());
            expectedException.expectMessage(expectedExceptionMessage);
        }
    }

    protected Class<? extends RuntimeException> getExpectedExceptionClass() {
        return expectedExceptionClass;
    }

    @Test
    public void testExecuteServiceOperation() throws RestClientException, MalformedURLException {
        serviceCreator.createService(client, input.getService(), SPACE_ID);

        validateRestCall();
    }

    protected void validateRestCall() throws RestClientException, MalformedURLException {
        Map<String, Object> serviceRequest = new HashMap<String, Object>();
        serviceRequest.put(SPACE_GUID, SPACE_ID);
        serviceRequest.put(SERVICE_NAME, input.getService().getName());
        serviceRequest.put("service_plan_guid", SERVICE_PLAN_GUID);
        serviceRequest.put(SERVICE_PARAMETERS, input.getService().getCredentials());
        Mockito.verify(restTemplate).postForObject(getUrl(CREATE_SERVICE_URL, new URL(CONTROLLER_ENDPOINT)), serviceRequest, String.class);
    }

    protected Class<? extends StepInput> getStepinput() {
        return StepInput.class;
    }

    protected static class StepInput {
        private CloudServiceExtended service;

        public CloudServiceExtended getService() {
            service.setMeta(new Meta(SERVICE_PLAN_GUID, null, null));
            return service;
        }
    }
}
