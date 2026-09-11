package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudMetadata;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceInstance;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudServiceInstance;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Metadata;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ServiceInstanceType;
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

class ServiceInstancesV3OperationsTest {

    private static final String GUID_STRING = "3725a721-6e3f-4b6e-8f2b-1c2d3e4f5a6b";
    private static final UUID SPACE_GUID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final String OFFERING_GUID = "aaaaaaaa-1111-1111-1111-aaaaaaaaaaaa";
    private static final String PLAN_GUID = "bbbbbbbb-2222-2222-2222-bbbbbbbbbbbb";

    @Mock
    private CloudSpace target;
    @Mock
    private CloudMetadata targetMetadata;

    private ServiceInstancesV3Operations operations;

    private MockControllerClientFactory factory;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        factory = MockControllerClientFactory.create();
        Mockito.when(target.getMetadata())
               .thenReturn(targetMetadata);
        Mockito.when(targetMetadata.getGuid())
               .thenReturn(SPACE_GUID);
        operations = new ServiceInstancesV3Operations(factory.client(), target);
    }

    @Test
    void testGetRequiredServiceInstanceGuidReturnsGuidWhenFound() {
        stubServiceInstanceLookup("my-service", getManagedInstanceJson("my-service"));

        UUID result = operations.getRequiredServiceInstanceGuid("my-service");

        Assertions.assertEquals(GUID_STRING, result.toString());
    }

    @Test
    void testGetRequiredServiceInstanceGuidThrowsWhenNotFound() {
        stubServiceInstanceLookup("missing", null);

        CloudOperationException thrown = Assertions.assertThrows(CloudOperationException.class,
                                                                 () -> operations.getRequiredServiceInstanceGuid("missing"));

        Assertions.assertEquals(HttpStatus.NOT_FOUND, thrown.getStatusCode());
    }

    @Test
    void testGetServiceInstanceWithoutAuxiliaryContentReturnsNullWhenNotRequiredAndMissing() {
        stubServiceInstanceLookup("missing", null);

        Assertions.assertNull(operations.getServiceInstanceWithoutAuxiliaryContent("missing", false));
    }

    @Test
    void testGetServiceInstanceWithoutAuxiliaryContentThrowsWhenRequiredAndMissing() {
        stubServiceInstanceLookup("missing", null);

        CloudOperationException thrown = Assertions.assertThrows(CloudOperationException.class,
                                                                 () -> operations.getServiceInstanceWithoutAuxiliaryContent("missing"));

        Assertions.assertEquals(HttpStatus.NOT_FOUND, thrown.getStatusCode());
    }

    @Test
    void testGetServiceInstanceWithoutAuxiliaryContentMapsWhenFound() {
        stubServiceInstanceLookup("my-service", getManagedInstanceJson("my-service"));

        CloudServiceInstance result = operations.getServiceInstanceWithoutAuxiliaryContent("my-service");

        Assertions.assertEquals("my-service", result.getName());
    }

    @Test
    void testGetServiceInstanceNameReturnsNameFromGet() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/service_instances/" + SPACE_GUID))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess(getManagedInstanceJson("my-service"), MediaType.APPLICATION_JSON));

        Assertions.assertEquals("my-service", operations.getServiceInstanceName(SPACE_GUID));
        factory.verify();
    }

    @Test
    void testGetServiceInstanceNameReturnsNullWhenGetReturnsNull() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/service_instances/" + SPACE_GUID))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess());

        Assertions.assertNull(operations.getServiceInstanceName(SPACE_GUID));
        factory.verify();
    }

    @Test
    void testCreateServiceInstancePostsManagedInstance() {
        stubOfferingLookup("my-offering", null);
        stubPlanLookup("my-plan");

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/service_instances"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
               .andRespond(MockRestResponseCreators.withSuccess());

        CloudServiceInstance serviceInstance = ImmutableCloudServiceInstance.builder()
                                                                            .name("my-service")
                                                                            .label("my-offering")
                                                                            .plan("my-plan")
                                                                            .build();

        operations.createServiceInstance(serviceInstance);

        factory.verify();
    }

    @Test
    void testCreateServiceInstanceThrowsWhenPlanNotFound() {
        stubOfferingLookup("my-offering", null);
        factory.server()
               .expect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess("{\"resources\":[]}", MediaType.APPLICATION_JSON));

        CloudServiceInstance serviceInstance = ImmutableCloudServiceInstance.builder()
                                                                            .name("my-service")
                                                                            .label("my-offering")
                                                                            .plan("missing-plan")
                                                                            .build();

        Assertions.assertThrows(CloudOperationException.class, () -> operations.createServiceInstance(serviceInstance));
    }

    @Test
    void testCreateServiceInstanceThrowsWhenSpaceNotProvided() {
        ServiceInstancesV3Operations noSpaceOperations = new ServiceInstancesV3Operations(factory.client(), null);

        CloudServiceInstance serviceInstance = ImmutableCloudServiceInstance.builder()
                                                                            .name("my-service")
                                                                            .label("my-offering")
                                                                            .plan("my-plan")
                                                                            .build();

        Assertions.assertThrows(IllegalArgumentException.class, () -> noSpaceOperations.createServiceInstance(serviceInstance));
    }

    @Test
    void testCreateUserProvidedServiceInstancePosts() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/service_instances"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
               .andRespond(MockRestResponseCreators.withSuccess());

        CloudServiceInstance serviceInstance = ImmutableCloudServiceInstance.builder()
                                                                            .name("my-ups")
                                                                            .syslogDrainUrl("syslog://example.com")
                                                                            .credentials(Map.of("user", "admin"))
                                                                            .build();

        operations.createUserProvidedServiceInstance(serviceInstance);

        factory.verify();
    }

    @Test
    void testCreateUserProvidedServiceInstanceThrowsWhenSpaceNotProvided() {
        ServiceInstancesV3Operations noSpaceOperations = new ServiceInstancesV3Operations(factory.client(), null);

        CloudServiceInstance serviceInstance = ImmutableCloudServiceInstance.builder()
                                                                            .name("my-ups")
                                                                            .build();

        Assertions.assertThrows(IllegalArgumentException.class, () -> noSpaceOperations.createUserProvidedServiceInstance(serviceInstance));
    }

    @Test
    void testDeleteServiceInstanceByNameDeletesFoundInstance() {
        stubServiceInstanceLookup("my-service", getManagedInstanceJson("my-service"));

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/service_instances/" + GUID_STRING))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.DELETE))
               .andRespond(MockRestResponseCreators.withSuccess());

        operations.deleteServiceInstance("my-service");

        factory.verify();
    }

    @Test
    void testDeleteServiceInstanceByNameThrowsWhenNotFound() {
        stubServiceInstanceLookup("missing", null);

        Assertions.assertThrows(CloudOperationException.class, () -> operations.deleteServiceInstance("missing"));
    }

    @Test
    void testDeleteServiceInstanceByInstanceDeletes() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/service_instances/" + GUID_STRING))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.DELETE))
               .andRespond(MockRestResponseCreators.withSuccess());

        CloudServiceInstance serviceInstance = ImmutableCloudServiceInstance.builder()
                                                                            .metadata(
                                                                                org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudMetadata.builder()
                                                                                                                                                                 .guid(
                                                                                                                                                                     UUID.fromString(
                                                                                                                                                                         GUID_STRING))
                                                                                                                                                                 .build())
                                                                            .name("my-service")
                                                                            .build();

        operations.deleteServiceInstance(serviceInstance);

        factory.verify();
    }

    @Test
    void testUpdateServicePlanUpdatesRelationship() {
        stubServiceInstanceLookup("my-service", getManagedInstanceJson("my-service"));

        stubPlanForNameResolution();
        stubOfferingForNameResolution();

        stubOfferingLookup("my-offering", null);
        stubPlanLookup("new-plan");
        stubPatch();

        operations.updateServicePlan("my-service", "new-plan");

        factory.verify();
    }

    @Test
    void testUpdateServicePlanSkipsUserProvided() {
        stubServiceInstanceLookup("my-ups", getUserProvidedInstanceJson("my-ups"));

        operations.updateServicePlan("my-ups", "new-plan");

        factory.verify();
    }

    @Test
    void testUpdateServiceParametersUpdatesManaged() {
        stubServiceInstanceLookup("my-service", getManagedInstanceJson("my-service"));
        stubPatch();

        operations.updateServiceParameters("my-service", Map.of("foo", "bar"));

        factory.verify();
    }

    @Test
    void testUpdateServiceParametersUpdatesUserProvided() {
        stubServiceInstanceLookup("my-ups", getUserProvidedInstanceJson("my-ups"));
        stubPatch();

        operations.updateServiceParameters("my-ups", Map.of("user", "admin"));

        factory.verify();
    }

    @Test
    void testUpdateServiceTagsUpdates() {
        stubServiceInstanceLookup("my-service", getManagedInstanceJson("my-service"));
        stubPatch();

        operations.updateServiceTags("my-service", List.of("tag1", "tag2"));

        factory.verify();
    }

    @Test
    void testUpdateServiceTagsThrowsWhenNotFound() {
        stubServiceInstanceLookup("missing", null);

        Assertions.assertThrows(CloudOperationException.class, () -> operations.updateServiceTags("missing", List.of("tag1")));
    }

    @Test
    void testUpdateServiceSyslogDrainUrlUpdatesUserProvided() {
        stubServiceInstanceLookup("my-ups", getUserProvidedInstanceJson("my-ups"));
        stubPatch();

        operations.updateServiceSyslogDrainUrl("my-ups", "syslog://new.example.com");

        factory.verify();
    }

    @Test
    void testUpdateServiceSyslogDrainUrlSkipsManaged() {
        stubServiceInstanceLookup("my-service", getManagedInstanceJson("my-service"));

        operations.updateServiceSyslogDrainUrl("my-service", "syslog://new.example.com");

        factory.verify();
    }

    @Test
    void testUpdateServiceInstanceMetadataUpdates() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/service_instances/" + GUID_STRING))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.PATCH))
               .andRespond(MockRestResponseCreators.withSuccess());

        Metadata metadata = Metadata.builder()
                                    .label("key", "value")
                                    .build();

        operations.updateServiceInstanceMetadata(UUID.fromString(GUID_STRING), metadata);

        factory.verify();
    }

    @Test
    void testUpdateServiceInstanceMetadataUpdatesWithNullMetadata() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/service_instances/" + GUID_STRING))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.PATCH))
               .andRespond(MockRestResponseCreators.withSuccess());

        operations.updateServiceInstanceMetadata(UUID.fromString(GUID_STRING), null);

        factory.verify();
    }

    @Test
    void testGetServiceInstancesByMetadataLabelSelectorMapsUserProvided() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/service_instances?per_page=5000"
                                                             + "&space_guids=" + SPACE_GUID + "&label_selector=env"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess("{\"resources\":[" + getUserProvidedInstanceJson("ups-1") + "]}",
                                                                MediaType.APPLICATION_JSON));

        List<CloudServiceInstance> result = operations.getServiceInstancesByMetadataLabelSelector("env");

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("ups-1", result.getFirst()
                                               .getName());
        factory.verify();
    }

    @Test
    void testGetServiceInstancesByMetadataLabelSelectorReturnsEmpty() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/service_instances?per_page=5000"
                                                             + "&space_guids=" + SPACE_GUID + "&label_selector=env"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess("{\"resources\":[]}", MediaType.APPLICATION_JSON));

        List<CloudServiceInstance> result = operations.getServiceInstancesByMetadataLabelSelector("env");

        Assertions.assertEquals(0, result.size());
        factory.verify();
    }

    @Test
    void testGetServiceInstancesWithoutAuxiliaryContentByMetadataLabelSelectorMaps() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/service_instances?per_page=5000"
                                                             + "&space_guids=" + SPACE_GUID + "&label_selector=env"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess("{\"resources\":[" + getManagedInstanceJson("mi-1") + "]}",
                                                                MediaType.APPLICATION_JSON));

        List<CloudServiceInstance> result = operations.getServiceInstancesWithoutAuxiliaryContentByMetadataLabelSelector("env");

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("mi-1", result.getFirst()
                                              .getName());
        Assertions.assertEquals(ServiceInstanceType.MANAGED, result.getFirst()
                                                                   .getType());
        factory.verify();
    }

    @Test
    void testGetServiceInstancesWithoutAuxiliaryContentByNames() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/service_instances?per_page=5000"
                                                             + "&space_guids=" + SPACE_GUID + "&names=my-service"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess("{\"resources\":[" + getManagedInstanceJson("my-service") + "]}",
                                                                MediaType.APPLICATION_JSON));

        List<CloudServiceInstance> result = operations.getServiceInstancesWithoutAuxiliaryContentByNames(List.of("my-service"));

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("my-service", result.getFirst()
                                                    .getName());
        factory.verify();
    }

    @Test
    void testGetServiceInstancesWithoutAuxiliaryContentByNamesReturnsEmptyForEmptyInput() {
        Assertions.assertEquals(0, operations.getServiceInstancesWithoutAuxiliaryContentByNames(List.of())
                                             .size());

        factory.verify();
    }

    private void stubServiceInstanceLookup(String name, String instanceJson) {
        String body = instanceJson == null ? "{\"resources\":[]}" : "{\"resources\":[" + instanceJson + "]}";

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/service_instances?per_page=5000"
                                                             + "&space_guids=" + SPACE_GUID + "&names=" + name))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess(body, MediaType.APPLICATION_JSON));
    }

    private void stubOfferingLookup(String label, String broker) {
        String uri = MockControllerClientFactory.BASE_URL + "/v3/service_offerings?per_page=5000&space_guids=" + SPACE_GUID + "&names="
            + label + (broker == null ? "" : "&service_broker_names=" + broker);

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(uri))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess("{\"resources\":[{\"guid\":\"" + OFFERING_GUID + "\",\"name\":\"" + label
                                                                    + "\"}]}", MediaType.APPLICATION_JSON));
    }

    private void stubPlanLookup(String planName) {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/service_plans?per_page=5000"
                                                             + "&service_offering_guids=" + OFFERING_GUID + "&names=" + planName))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess("{\"resources\":[{\"guid\":\"" + PLAN_GUID + "\",\"name\":\"" + planName
                                                                    + "\"}]}", MediaType.APPLICATION_JSON));
    }

    private void stubPlanForNameResolution() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/service_plans/" + PLAN_GUID))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess("{\"guid\":\"" + PLAN_GUID + "\",\"name\":\"my-plan\",\"relationships\":"
                                                                    + "{\"service_offering\":{\"data\":{\"guid\":\"" + OFFERING_GUID
                                                                    + "\"}}}}", MediaType.APPLICATION_JSON));
    }

    private void stubOfferingForNameResolution() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/service_offerings/" + OFFERING_GUID))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess("{\"guid\":\"" + OFFERING_GUID + "\",\"name\":\"my-offering\"}",
                                                                MediaType.APPLICATION_JSON));
    }

    private void stubPatch() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/service_instances/" + GUID_STRING))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.PATCH))
               .andRespond(MockRestResponseCreators.withSuccess());
    }

    private static String getManagedInstanceJson(String name) {
        return "{\"guid\":\"" + GUID_STRING + "\",\"name\":\"" + name + "\",\"type\":\"managed\",\"relationships\":"
            + "{\"service_plan\":{\"data\":{\"guid\":\"" + PLAN_GUID + "\"}}}}";
    }

    private static String getUserProvidedInstanceJson(String name) {
        return "{\"guid\":\"" + GUID_STRING + "\",\"name\":\"" + name + "\",\"type\":\"user-provided\"}";
    }

}
