package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudMetadata;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceBroker;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudServiceBroker;
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

class ServiceBrokersV3OperationsTest {

    private static final String BROKER_GUID = "11111111-2222-3333-4444-555555555555";
    private static final String SPACE_GUID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
    private static final String OFFERING_GUID = "66666666-7777-8888-9999-000000000000";
    private static final String PLAN_GUID = "abcabcab-cdcd-efef-1212-343434343434";
    private static final String JOB_GUID = "99999999-8888-7777-6666-555555555555";

    private static final String BROKERS_URI = MockControllerClientFactory.BASE_URL + "/v3/service_brokers";

    @Mock
    private CloudSpace target;
    @Mock
    private CloudMetadata targetMetadata;

    private MockControllerClientFactory factory;
    private ServiceBrokersV3Operations operations;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        factory = MockControllerClientFactory.create();
        operations = new ServiceBrokersV3Operations(factory.client(), target);
    }

    @Test
    void testGetServiceBrokersReturnsMappedList() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(BROKERS_URI + "?per_page=5000"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess("{\"resources\":[" + getBrokerJson("broker-a") + "]}",
                                                                MediaType.APPLICATION_JSON));

        List<CloudServiceBroker> result = operations.getServiceBrokers();

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("broker-a", result.getFirst()
                                                  .getName());
        Assertions.assertEquals("https://broker-a.example.com", result.getFirst()
                                                                      .getUrl());
        factory.verify();
    }

    @Test
    void testGetServiceBrokersReturnsEmptyList() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(BROKERS_URI + "?per_page=5000"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess("{\"resources\":[]}", MediaType.APPLICATION_JSON));

        List<CloudServiceBroker> result = operations.getServiceBrokers();

        Assertions.assertTrue(result.isEmpty());
        factory.verify();
    }

    @Test
    void testGetServiceBrokerReturnsMappedBroker() {
        stubBrokerLookup("broker-a", "{\"resources\":[" + getBrokerJson("broker-a") + "]}");

        CloudServiceBroker result = operations.getServiceBroker("broker-a");

        Assertions.assertEquals("broker-a", result.getName());
        Assertions.assertEquals(UUID.fromString(BROKER_GUID), result.getMetadata()
                                                                    .getGuid());
        factory.verify();
    }

    @Test
    void testGetServiceBrokerThrowsWhenRequiredAndMissing() {
        stubBrokerLookup("missing", "{\"resources\":[]}");

        Assertions.assertThrows(CloudOperationException.class, () -> operations.getServiceBroker("missing"));
        factory.verify();
    }

    @Test
    void testGetServiceBrokerReturnsNullWhenNotRequiredAndMissing() {
        stubBrokerLookup("missing", "{\"resources\":[]}");

        Assertions.assertNull(operations.getServiceBroker("missing", false));
        factory.verify();
    }

    @Test
    void testCreateServiceBrokerReturnsJobGuidFromLocation() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(BROKERS_URI))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
               .andRespond(MockRestResponseCreators.withStatus(HttpStatus.ACCEPTED)
                                                   .location(URI.create(MockControllerClientFactory.BASE_URL + "/v3/jobs/" + JOB_GUID)));

        CloudServiceBroker broker = ImmutableCloudServiceBroker.builder()
                                                               .name("broker-a")
                                                               .url("https://broker-a.example.com")
                                                               .username("user")
                                                               .password("pass")
                                                               .spaceGuid(SPACE_GUID)
                                                               .build();

        String jobGuid = operations.createServiceBroker(broker);

        Assertions.assertEquals(JOB_GUID, jobGuid);
        factory.verify();
    }

    @Test
    void testCreateServiceBrokerReturnsNullWhenNoLocation() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(BROKERS_URI))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
               .andRespond(MockRestResponseCreators.withStatus(HttpStatus.CREATED));

        CloudServiceBroker broker = ImmutableCloudServiceBroker.builder()
                                                               .name("broker-a")
                                                               .url("https://broker-a.example.com")
                                                               .build();

        Assertions.assertNull(operations.createServiceBroker(broker));
        factory.verify();
    }

    @Test
    void testCreateServiceBrokerThrowsWhenNull() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> operations.createServiceBroker(null));
    }

    @Test
    void testDeleteServiceBrokerLooksUpThenDeletesAndReturnsJobGuid() {
        stubBrokerLookup("broker-a", "{\"resources\":[" + getBrokerJson("broker-a") + "]}");

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(BROKERS_URI + "/" + BROKER_GUID))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.DELETE))
               .andRespond(MockRestResponseCreators.withStatus(HttpStatus.ACCEPTED)
                                                   .location(URI.create(MockControllerClientFactory.BASE_URL + "/v3/jobs/" + JOB_GUID)));

        String jobGuid = operations.deleteServiceBroker("broker-a");

        Assertions.assertEquals(JOB_GUID, jobGuid);
        factory.verify();
    }

    @Test
    void testDeleteServiceBrokerThrowsWhenBrokerMissing() {
        stubBrokerLookup("missing", "{\"resources\":[]}");

        Assertions.assertThrows(CloudOperationException.class, () -> operations.deleteServiceBroker("missing"));
        factory.verify();
    }

    @Test
    void testUpdateServiceBrokerLooksUpThenUpdatesAndReturnsJobGuid() {
        stubBrokerLookup("broker-a", "{\"resources\":[" + getBrokerJson("broker-a") + "]}");

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(BROKERS_URI + "/" + BROKER_GUID))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.PATCH))
               .andRespond(MockRestResponseCreators.withStatus(HttpStatus.ACCEPTED)
                                                   .location(URI.create(MockControllerClientFactory.BASE_URL + "/v3/jobs/" + JOB_GUID)));

        CloudServiceBroker broker = ImmutableCloudServiceBroker.builder()
                                                               .name("broker-a")
                                                               .url("https://broker-a-new.example.com")
                                                               .username("user")
                                                               .password("pass")
                                                               .build();

        String jobGuid = operations.updateServiceBroker(broker);

        Assertions.assertEquals(JOB_GUID, jobGuid);
        factory.verify();
    }

    @Test
    void testUpdateServiceBrokerThrowsWhenNull() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> operations.updateServiceBroker(null));
    }

    @Test
    void testUpdateServiceBrokerThrowsWhenBrokerMissing() {
        stubBrokerLookup("missing", "{\"resources\":[]}");

        CloudServiceBroker broker = ImmutableCloudServiceBroker.builder()
                                                               .name("missing")
                                                               .url("https://missing.example.com")
                                                               .build();

        Assertions.assertThrows(CloudOperationException.class, () -> operations.updateServiceBroker(broker));
        factory.verify();
    }

    @Test
    void testUpdateServicePlanVisibilityForBrokerUpdatesEachPlan() {
        Mockito.when(target.getMetadata())
               .thenReturn(targetMetadata);

        Mockito.when(targetMetadata.getGuid())
               .thenReturn(UUID.fromString(SPACE_GUID));

        stubBrokerLookup("broker-a", "{\"resources\":[" + getBrokerJson("broker-a") + "]}");

        factory.server()
               .expect(
                   MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/service_offerings?service_broker_guids="
                                                         + BROKER_GUID + "&space_guids=" + SPACE_GUID + "&per_page=5000"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess("{\"resources\":[{\"guid\":\"" + OFFERING_GUID + "\"}]}",
                                                                MediaType.APPLICATION_JSON));

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/service_plans?service_offering_guids="
                                                             + OFFERING_GUID + "&per_page=5000"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess("{\"resources\":[{\"guid\":\"" + PLAN_GUID + "\"}]}",
                                                                MediaType.APPLICATION_JSON));

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/service_plans/" + PLAN_GUID
                                                             + "/visibility"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.PATCH))
               .andRespond(MockRestResponseCreators.withSuccess("{\"type\":\"public\"}", MediaType.APPLICATION_JSON));

        operations.updateServicePlanVisibilityForBroker("broker-a", ServicePlanVisibility.PUBLIC);

        factory.verify();
    }

    @Test
    void testUpdateServicePlanVisibilityForBrokerWithNoOfferingsUpdatesNothing() {
        Mockito.when(target.getMetadata())
               .thenReturn(targetMetadata);

        Mockito.when(targetMetadata.getGuid())
               .thenReturn(UUID.fromString(SPACE_GUID));

        stubBrokerLookup("broker-a", "{\"resources\":[" + getBrokerJson("broker-a") + "]}");

        factory.server()
               .expect(
                   MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/service_offerings?service_broker_guids="
                                                         + BROKER_GUID + "&space_guids=" + SPACE_GUID + "&per_page=5000"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess("{\"resources\":[]}", MediaType.APPLICATION_JSON));

        operations.updateServicePlanVisibilityForBroker("broker-a", ServicePlanVisibility.ADMIN);

        factory.verify();
    }

    @Test
    void testUpdateServicePlanVisibilityForBrokerThrowsWhenBrokerMissing() {
        stubBrokerLookup("missing", "{\"resources\":[]}");

        Assertions.assertThrows(CloudOperationException.class,
                                () -> operations.updateServicePlanVisibilityForBroker("missing", ServicePlanVisibility.PUBLIC));
        factory.verify();
    }

    private void stubBrokerLookup(String name, String responseJson) {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(BROKERS_URI + "?names=" + name + "&per_page=5000"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess(responseJson, MediaType.APPLICATION_JSON));
    }

    private static String getBrokerJson(String name) {
        return "{\"guid\":\"" + BROKER_GUID + "\",\"name\":\"" + name + "\",\"url\":\"https://" + name
            + ".example.com\",\"relationships\":{\"space\":{\"data\":{\"guid\":\"" + SPACE_GUID + "\"}}}}";
    }

}
