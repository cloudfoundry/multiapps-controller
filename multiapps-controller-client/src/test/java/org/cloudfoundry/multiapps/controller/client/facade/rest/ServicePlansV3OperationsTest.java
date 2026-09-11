package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ServicePlanVisibility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;

class ServicePlansV3OperationsTest {

    private static final String BROKER_GUID = "11111111-1111-1111-1111-111111111111";
    private static final String OFFERING_GUID = "22222222-2222-2222-2222-222222222222";
    private static final String PLAN_GUID = "33333333-3333-3333-3333-333333333333";
    private static final UUID SPACE_GUID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final String BROKER_NAME = "my-broker";

    @Mock
    private CloudSpace target;

    private MockControllerClientFactory factory;
    private ServicePlansV3Operations operations;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        factory = MockControllerClientFactory.create();
        operations = new ServicePlansV3Operations(factory.client(), target);
    }

    @Test
    void testUpdateServicePlanVisibilityForBrokerUpdatesAllPlansWhenNoSpaceFilter() {
        Mockito.when(target.getGuid())
               .thenReturn(null);

        stubBrokerLookup("{\"resources\":[{\"guid\":\"" + BROKER_GUID + "\"}]}");
        stubOfferingLookup(null, "{\"resources\":[{\"guid\":\"" + OFFERING_GUID + "\"}]}");
        stubPlanLookup("{\"resources\":[{\"guid\":\"" + PLAN_GUID + "\"}]}");
        stubVisibilityPatch(PLAN_GUID);

        operations.updateServicePlanVisibilityForBroker(BROKER_NAME, ServicePlanVisibility.PUBLIC);

        factory.verify();
    }

    @Test
    void testUpdateServicePlanVisibilityForBrokerAppendsSpaceGuidWhenTargetHasGuid() {
        Mockito.when(target.getGuid())
               .thenReturn(SPACE_GUID);
        stubBrokerLookup("{\"resources\":[{\"guid\":\"" + BROKER_GUID + "\"}]}");
        stubOfferingLookup(SPACE_GUID, "{\"resources\":[{\"guid\":\"" + OFFERING_GUID + "\"}]}");
        stubPlanLookup("{\"resources\":[{\"guid\":\"" + PLAN_GUID + "\"}]}");
        stubVisibilityPatch(PLAN_GUID);

        operations.updateServicePlanVisibilityForBroker(BROKER_NAME, ServicePlanVisibility.ORGANIZATION);

        factory.verify();
    }

    @Test
    void testUpdateServicePlanVisibilityForBrokerThrowsWhenBrokerNotFound() {
        stubBrokerLookup("{\"resources\":[]}");

        Assertions.assertThrows(CloudOperationException.class,
                                () -> operations.updateServicePlanVisibilityForBroker(BROKER_NAME, ServicePlanVisibility.PUBLIC));
        factory.verify();
    }

    @Test
    void testUpdateServicePlanVisibilityForBrokerDoesNothingWhenNoOfferings() {
        Mockito.when(target.getGuid())
               .thenReturn(null);

        stubBrokerLookup("{\"resources\":[{\"guid\":\"" + BROKER_GUID + "\"}]}");
        stubOfferingLookup(null, "{\"resources\":[]}");

        operations.updateServicePlanVisibilityForBroker(BROKER_NAME, ServicePlanVisibility.PUBLIC);

        factory.verify();
    }

    @Test
    void testUpdateServicePlanVisibilityForBrokerDoesNothingWhenNoPlans() {
        Mockito.when(target.getGuid())
               .thenReturn(null);

        stubBrokerLookup("{\"resources\":[{\"guid\":\"" + BROKER_GUID + "\"}]}");
        stubOfferingLookup(null, "{\"resources\":[{\"guid\":\"" + OFFERING_GUID + "\"}]}");
        stubPlanLookup("{\"resources\":[]}");

        operations.updateServicePlanVisibilityForBroker(BROKER_NAME, ServicePlanVisibility.PUBLIC);

        factory.verify();
    }

    private void stubBrokerLookup(String responseJson) {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/service_brokers?names="
                                                             + BROKER_NAME + "&per_page=5000"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess(responseJson, MediaType.APPLICATION_JSON));
    }

    private void stubOfferingLookup(UUID spaceGuid, String responseJson) {
        String uri = MockControllerClientFactory.BASE_URL + "/v3/service_offerings?service_broker_guids=" + BROKER_GUID
            + "&per_page=5000";

        if (spaceGuid != null) {
            uri = uri + "&space_guids=" + spaceGuid;
        }

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(uri))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess(responseJson, MediaType.APPLICATION_JSON));
    }

    private void stubPlanLookup(String responseJson) {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/service_plans?service_offering_guids="
                                                             + OFFERING_GUID + "&per_page=5000"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess(responseJson, MediaType.APPLICATION_JSON));
    }

    private void stubVisibilityPatch(String planGuid) {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/service_plans/" + planGuid
                                                             + "/visibility"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.PATCH))
               .andRespond(MockRestResponseCreators.withStatus(HttpStatus.OK));
    }

}
