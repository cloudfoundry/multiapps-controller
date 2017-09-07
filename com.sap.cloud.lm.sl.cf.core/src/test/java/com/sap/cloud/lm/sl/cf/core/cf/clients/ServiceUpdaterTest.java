package com.sap.cloud.lm.sl.cf.core.cf.clients;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.domain.CloudEntity.Meta;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.CloudServiceOffering;
import org.cloudfoundry.client.lib.domain.CloudServicePlan;
import org.cloudfoundry.client.lib.util.CloudEntityResourceMapper;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.springframework.web.client.RestClientException;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.common.ParsingException;
import com.sap.cloud.lm.sl.common.SLException;

public class ServiceUpdaterTest extends ServiceCreatorTest {

    private static final String SERVICE_INSTANCES_URL = "/v2/service_instances";

    @InjectMocks
    private ServiceUpdater serviceUpdater = new ServiceUpdater() {
        @Override
        protected CloudEntityResourceMapper getResourceMapper() {
            return resourceMapper;
        }
    };

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) Service with a changed plan
            {
                "service-update-01.json", null, null
            },
            // (1) With non-existing service
            {
                "service-update-02.json", "404 Not Found: Service 'com.sap.sample.mta.test' not found", CloudFoundryException.class
            },
            // (2) With non-existing service plan
            {
                "service-update-03.json", "Service plan test-new-plan for service com.sap.sample.mta.test not found", SLException.class
            }
// @formatter:on
        });
    }

    public ServiceUpdaterTest(String inputLocation, String expected, Class<? extends RuntimeException> expectedExceptionClass)
        throws ParsingException, IOException {
        super(inputLocation, expected, expectedExceptionClass);
    }

    @Override
    public void setUp() throws MalformedURLException {
        super.setUp();
        prepareClient();
    }

    @Override
    protected void setUpExistingOfferings() throws MalformedURLException {
        List<Map<String, Object>> resourcesList = new ArrayList<>();
        Map<String, Object> resourceMap = new HashMap<>();
        CloudServiceOffering offering = new CloudServiceOffering(null, getServiceLabel());
        offering.addCloudServicePlan(new CloudServicePlan(new Meta(SERVICE_PLAN_GUID, null, null), getCloudServicePlan()));
        resourcesList.add(new HashMap<>());
        Mockito.when(resourceMapper.mapResource(new HashMap<String, Object>(), CloudServiceOffering.class)).thenReturn(offering);

        resourceMap.put("resources", resourcesList);
        Mockito.when(client.getCloudControllerUrl()).thenReturn(new URL(CONTROLLER_ENDPOINT));
        Mockito.when(restTemplate.getForObject(getUrl("/v2/services?inline-relations-depth=1", new URL(CONTROLLER_ENDPOINT)),
            String.class)).thenReturn(org.cloudfoundry.client.lib.util.JsonUtil.convertToJson(resourceMap));

        Mockito.when(restTemplateFactory.getRestTemplate(client)).thenReturn(restTemplate);
    }

    private void prepareClient() {
        Mockito.when(client.getService(input.getService().getName())).thenReturn(((StepInput) input).getExistingService());
    }

    @Override
    protected String getCloudServicePlan() {
        return input.getService().getPlan();
    }

    @Override
    protected String getServiceLabel() {
        return ((StepInput) input).getService().getLabel();
    }

    @Test
    public void testExecuteServiceOperation() throws RestClientException, MalformedURLException {
        serviceUpdater.updateServicePlan(client, input.getService().getName(), input.getService().getPlan());

        validateRestCall();
    }

    @Override
    protected void validateRestCall() throws RestClientException, MalformedURLException {
        Map<String, Object> serviceRequest = new HashMap<String, Object>();
        serviceRequest.put("service_plan_guid", SERVICE_PLAN_GUID.toString());
        Mockito.verify(restTemplate).put(getUrl(getServiceUpdateUrl(), new URL(CONTROLLER_ENDPOINT)), serviceRequest);
    }

    private String getServiceUpdateUrl() {
        return SERVICE_INSTANCES_URL + "/" + input.getService().getMeta().getGuid().toString() + "?accepts_incomplete=true";
    }

    @Override
    protected Class<? extends StepInput> getStepinput() {
        return StepInput.class;
    }

    private static class StepInput extends ServiceCreatorTest.StepInput {
        CloudService existingService;

        public CloudServiceExtended getService() {
            CloudServiceExtended service = super.getService();
            service.setMeta(new Meta(SERVICE_PLAN_GUID, null, null));
            return service;
        }

        public CloudService getExistingService() {
            if (existingService == null) {
                return null;
            }
            existingService.setMeta(new Meta(SERVICE_PLAN_GUID, null, null));
            return existingService;
        }
    }

}
