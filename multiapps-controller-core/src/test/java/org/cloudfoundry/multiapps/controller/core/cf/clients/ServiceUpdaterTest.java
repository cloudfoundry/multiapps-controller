package org.cloudfoundry.multiapps.controller.core.cf.clients;

import static org.junit.Assert.assertEquals;

import java.net.MalformedURLException;
import java.util.UUID;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.cloudfoundry.client.lib.domain.ImmutableCloudMetadata;
import org.cloudfoundry.client.lib.domain.ImmutableCloudServiceInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

public class ServiceUpdaterTest extends CloudServiceOperatorTest {

    private static final String SERVICE_INSTANCES_ENDPOINT = "/v2/service_instances";
    private static final String EXISTING_SERVICE_GUID = "6061e8a8-3c0a-4826-9c01-cc676447af59";
    private static final String EXISTING_SERVICE_INSTANCE_NAME = "foo";
    private static final String EXISTING_SERVICE_PLAN = "v3.4-large";
    private static final String EXISTING_SERVICE_LABEL = "mongodb";
    private static final CloudServiceInstance EXISTING_SERVICE_INSTANCE = ImmutableCloudServiceInstance.builder()
                                                                                                       .metadata(ImmutableCloudMetadata.builder()
                                                                                                                                       .guid(UUID.fromString(EXISTING_SERVICE_GUID))
                                                                                                                                       .build())
                                                                                                       .name(EXISTING_SERVICE_INSTANCE_NAME)
                                                                                                       .plan(EXISTING_SERVICE_PLAN)
                                                                                                       .label(EXISTING_SERVICE_LABEL)
                                                                                                       .build();

    private ServiceUpdater serviceUpdater;

    @BeforeEach
    public void createServiceUpdater() {
        serviceUpdater = new ServiceUpdater(getMockedRestTemplateFactory());
    }

    @Test
    public void testUpdateServicePlan1() throws MalformedURLException {
        CloudControllerClient client = getMockedClient();
        Mockito.when(client.getServiceInstance(EXISTING_SERVICE_INSTANCE_NAME))
               .thenReturn(EXISTING_SERVICE_INSTANCE);

        serviceUpdater.updateServicePlan(client, EXISTING_SERVICE_INSTANCE_NAME, "v3.0-small");

        validatePlanUpdate("8e886beb-85cb-4455-9474-b6dfda36ffeb");
    }

    @SuppressWarnings("unchecked")
    private void validatePlanUpdate(String servicePlanGuid) {
        String updateServicePlanUrl = getUpdateServicePlanUrl();
        Mockito.verify(getMockedRestTemplate())
               .exchange(ArgumentMatchers.eq(updateServicePlanUrl), ArgumentMatchers.any(HttpMethod.class), ArgumentMatchers.any(),
                         ArgumentMatchers.any(Class.class));
    }

    private String getUpdateServicePlanUrl() {
        return getControllerUrl() + SERVICE_INSTANCES_ENDPOINT + "/" + EXISTING_SERVICE_GUID + "?accepts_incomplete=true";
    }

    @Test
    public void testUpdateServicePlan2() {
        // Given:
        CloudControllerClient client = getMockedClient();
        Mockito.when(client.getServiceInstance(EXISTING_SERVICE_INSTANCE_NAME))
               .thenReturn(EXISTING_SERVICE_INSTANCE);

        try {
            // When:
            serviceUpdater.updateServicePlan(client, EXISTING_SERVICE_INSTANCE_NAME, "v3.0-large");
        } catch (CloudOperationException e) {
            // Then:
            assertEquals("404 Not Found: Could not create service instance \"foo\". Service plan \"v3.0-large\" from service offering \"mongodb\" was not found.",
                         e.getMessage());
        }
    }

    @Test
    public void testUpdateServicePlan3() {
        // Given:
        CloudControllerClient client = getMockedClient();
        Mockito.when(client.getServiceInstance(EXISTING_SERVICE_INSTANCE_NAME))
               .thenThrow(new CloudOperationException(HttpStatus.NOT_FOUND,
                                                      HttpStatus.NOT_FOUND.getReasonPhrase(),
                                                      "Service \"foo\" was not found!"));

        try {
            // When:
            serviceUpdater.updateServicePlan(client, EXISTING_SERVICE_INSTANCE_NAME, "v3.0-small");
        } catch (CloudOperationException e) {
            // Then:
            assertEquals("404 Not Found: Service \"foo\" was not found!", e.getMessage());
        }
    }

}
