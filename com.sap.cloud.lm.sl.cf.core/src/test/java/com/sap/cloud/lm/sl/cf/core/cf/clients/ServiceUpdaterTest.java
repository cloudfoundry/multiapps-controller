package com.sap.cloud.lm.sl.cf.core.cf.clients;

import static org.junit.Assert.assertEquals;

import java.net.MalformedURLException;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudEntity.Meta;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import com.sap.cloud.lm.sl.cf.core.exec.MethodExecution;
import com.sap.cloud.lm.sl.cf.core.exec.MethodExecution.ExecutionState;
import com.sap.cloud.lm.sl.common.util.MapUtil;

public class ServiceUpdaterTest extends CloudServiceOperatorTest {

    private static final String SERVICE_INSTANCES_ENDPOINT = "/v2/service_instances";
    private static final String EXISTING_SERVICE_GUID = "6061e8a8-3c0a-4826-9c01-cc676447af59";
    private static final String EXISTING_SERVICE_NAME = "foo";
    private static final String EXISTING_SERVICE_PLAN = "v3.4-large";
    private static final String EXISTING_SERVICE_LABEL = "mongodb";
    private static final CloudService EXISTING_SERVICE = new CloudService();
    static {
        EXISTING_SERVICE.setMeta(new Meta(UUID.fromString(EXISTING_SERVICE_GUID), null, null));
        EXISTING_SERVICE.setName(EXISTING_SERVICE_NAME);
        EXISTING_SERVICE.setPlan(EXISTING_SERVICE_PLAN);
        EXISTING_SERVICE.setLabel(EXISTING_SERVICE_LABEL);
    }

    private ServiceUpdater serviceUpdater;

    @Before
    public void createServiceUpdater() {
        serviceUpdater = new ServiceUpdater(getMockedRestTemplateFactory());
    }

    @Test
    public void testUpdateServicePlan1() throws MalformedURLException {
        CloudControllerClient client = getMockedClient();
        Mockito.when(client.getService(EXISTING_SERVICE_NAME))
            .thenReturn(EXISTING_SERVICE);

        serviceUpdater.updateServicePlan(client, EXISTING_SERVICE_NAME, "v3.0-small");

        validatePlanUpdate("8e886beb-85cb-4455-9474-b6dfda36ffeb");
    }

    @SuppressWarnings("unchecked")
    private void validatePlanUpdate(String servicePlanGuid) throws MalformedURLException {
        String updateServicePlanUrl = getUpdateServicePlanUrl();
        Mockito.verify(getMockedRestTemplate()).exchange(Matchers.eq(updateServicePlanUrl), Matchers.any(HttpMethod.class), Matchers.any(),
            Matchers.any(Class.class));
    }

    private String getUpdateServicePlanUrl() {
        return getControllerUrl() + SERVICE_INSTANCES_ENDPOINT + "/" + EXISTING_SERVICE_GUID + "?accepts_incomplete=true";
    }

    @Test
    public void testUpdateServicePlan2() {
        // Given:
        CloudControllerClient client = getMockedClient();
        Mockito.when(client.getService(EXISTING_SERVICE_NAME))
            .thenReturn(EXISTING_SERVICE);

        try {
            // When:
            serviceUpdater.updateServicePlan(client, EXISTING_SERVICE_NAME, "v3.0-large");
        } catch (CloudOperationException e) {
            // Then:
            assertEquals(
                "404 Not Found: Could not create service instance \"foo\". Service plan \"v3.0-large\" from service offering \"mongodb\" was not found.",
                e.getMessage());
        }
    }

    @Test
    public void testUpdateServicePlan3() {
        // Given:
        CloudControllerClient client = getMockedClient();
        Mockito.when(client.getService(EXISTING_SERVICE_NAME))
            .thenThrow(new CloudOperationException(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.getReasonPhrase(),
                "Service \"foo\" was not found!"));

        try {
            // When:
            serviceUpdater.updateServicePlan(client, EXISTING_SERVICE_NAME, "v3.0-small");
        } catch (CloudOperationException e) {
            // Then:
            assertEquals("404 Not Found: Service \"foo\" was not found!", e.getMessage());
        }
    }

}
