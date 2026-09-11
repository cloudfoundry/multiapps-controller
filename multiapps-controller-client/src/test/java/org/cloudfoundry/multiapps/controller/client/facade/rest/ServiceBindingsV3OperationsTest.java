package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceBinding;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Metadata;
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

class ServiceBindingsV3OperationsTest {

    private static final String APP_GUID = "3725a721-6e3f-4b6e-8f2b-1c2d3e4f5a6b";
    private static final String SERVICE_INSTANCE_GUID = "11111111-2222-3333-4444-555555555555";
    private static final String BINDING_GUID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
    private static final String JOB_GUID = "99999999-8888-7777-6666-555555555555";

    @Mock
    private CloudSpace target;

    private MockControllerClientFactory factory;
    private ServiceBindingsV3Operations operations;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        factory = MockControllerClientFactory.create();
        operations = new ServiceBindingsV3Operations(factory.client(), target);
    }

    @Test
    void testBindServiceInstanceReturnsJobGuidFromLocation() {
        Mockito.when(target.getGuid())
               .thenReturn(null);

        stubAppLookup("my-app");
        stubServiceInstanceLookup("my-service");

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/service_credential_bindings"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
               .andRespond(MockRestResponseCreators.withStatus(HttpStatus.ACCEPTED)
                                                   .location(URI.create(MockControllerClientFactory.BASE_URL + "/v3/jobs/" + JOB_GUID)));

        Optional<String> jobId = operations.bindServiceInstance("my-binding", "my-app", "my-service");

        Assertions.assertTrue(jobId.isPresent());
        Assertions.assertEquals(JOB_GUID, jobId.get());
        factory.verify();
    }

    @Test
    void testBindServiceInstanceWithParametersReturnsEmptyWhenNoLocation() {
        Mockito.when(target.getGuid())
               .thenReturn(null);

        stubAppLookup("my-app");
        stubServiceInstanceLookup("my-service");

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/service_credential_bindings"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
               .andRespond(MockRestResponseCreators.withStatus(HttpStatus.CREATED));

        Optional<String> jobId = operations.bindServiceInstance("my-binding", "my-app", "my-service", Map.of("key", "value"));

        Assertions.assertTrue(jobId.isEmpty());
        factory.verify();
    }

    @Test
    void testBindServiceInstanceUsesSpaceGuidInLookupWhenTargetHasGuid() {
        UUID spaceGuid = UUID.randomUUID();
        Mockito.when(target.getGuid())
               .thenReturn(spaceGuid);

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/apps?per_page=5000&space_guids="
                                                             + spaceGuid + "&names=my-app"))
               .andRespond(MockRestResponseCreators.withSuccess(getResourcesJson(APP_GUID), MediaType.APPLICATION_JSON));

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL
                                                             + "/v3/service_instances?per_page=5000&space_guids=" + spaceGuid
                                                             + "&names=my-service"))
               .andRespond(MockRestResponseCreators.withSuccess(getResourcesJson(SERVICE_INSTANCE_GUID), MediaType.APPLICATION_JSON));

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/service_credential_bindings"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
               .andRespond(MockRestResponseCreators.withStatus(HttpStatus.CREATED));

        Optional<String> jobId = operations.bindServiceInstance("my-binding", "my-app", "my-service");

        Assertions.assertTrue(jobId.isEmpty());
        factory.verify();
    }

    @Test
    void testBindServiceInstanceThrowsWhenApplicationNotFound() {
        Mockito.when(target.getGuid())
               .thenReturn(null);

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/apps?per_page=5000&names=missing-app"))
               .andRespond(MockRestResponseCreators.withSuccess("{\"resources\":[]}", MediaType.APPLICATION_JSON));

        Assertions.assertThrows(CloudOperationException.class,
                                () -> operations.bindServiceInstance("my-binding", "missing-app", "my-service"));
    }

    @Test
    void testBindServiceInstanceThrowsWhenServiceInstanceNotFound() {
        Mockito.when(target.getGuid())
               .thenReturn(null);

        stubAppLookup("my-app");
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL
                                                             + "/v3/service_instances?per_page=5000&names=missing-service"))
               .andRespond(MockRestResponseCreators.withSuccess("{\"resources\":[]}", MediaType.APPLICATION_JSON));

        Assertions.assertThrows(CloudOperationException.class,
                                () -> operations.bindServiceInstance("my-binding", "my-app", "missing-service"));
    }

    @Test
    void testUnbindServiceInstanceByNameDeletesFoundBindings() {
        Mockito.when(target.getGuid())
               .thenReturn(null);

        stubAppLookup("my-app");
        stubServiceInstanceLookup("my-service");

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL
                                                             + "/v3/service_credential_bindings?app_guids=" + APP_GUID
                                                             + "&service_instance_guids=" + SERVICE_INSTANCE_GUID
                                                             + "&per_page=5000"))
               .andRespond(MockRestResponseCreators.withSuccess(getResourcesJson(BINDING_GUID), MediaType.APPLICATION_JSON));

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/service_credential_bindings/"
                                                             + BINDING_GUID))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.DELETE))
               .andRespond(MockRestResponseCreators.withStatus(HttpStatus.ACCEPTED)
                                                   .location(URI.create(MockControllerClientFactory.BASE_URL + "/v3/jobs/" + JOB_GUID)));

        List<String> jobIds = operations.unbindServiceInstance("my-app", "my-service");

        Assertions.assertEquals(List.of(JOB_GUID), jobIds);
        factory.verify();
    }

    @Test
    void testUnbindServiceInstanceByGuidDeletesFoundBindings() {
        UUID appGuid = UUID.fromString(APP_GUID);
        UUID serviceInstanceGuid = UUID.fromString(SERVICE_INSTANCE_GUID);

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL
                                                             + "/v3/service_credential_bindings?app_guids=" + APP_GUID
                                                             + "&service_instance_guids=" + SERVICE_INSTANCE_GUID
                                                             + "&per_page=5000"))
               .andRespond(MockRestResponseCreators.withSuccess(getResourcesJson(BINDING_GUID), MediaType.APPLICATION_JSON));

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/service_credential_bindings/"
                                                             + BINDING_GUID))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.DELETE))
               .andRespond(MockRestResponseCreators.withStatus(HttpStatus.NO_CONTENT));

        List<String> jobIds = operations.unbindServiceInstance(appGuid, serviceInstanceGuid);

        Assertions.assertTrue(jobIds.isEmpty());
        factory.verify();
    }

    @Test
    void testUnbindServiceInstanceThrowsWhenNoBindingFound() {
        UUID appGuid = UUID.fromString(APP_GUID);
        UUID serviceInstanceGuid = UUID.fromString(SERVICE_INSTANCE_GUID);

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL
                                                             + "/v3/service_credential_bindings?app_guids=" + APP_GUID
                                                             + "&service_instance_guids=" + SERVICE_INSTANCE_GUID
                                                             + "&per_page=5000"))
               .andRespond(MockRestResponseCreators.withSuccess("{\"resources\":[]}", MediaType.APPLICATION_JSON));

        Assertions.assertThrows(CloudOperationException.class,
                                () -> operations.unbindServiceInstance(appGuid, serviceInstanceGuid));
    }

    @Test
    void testDeleteServiceBindingReturnsJobGuid() {
        UUID bindingGuid = UUID.fromString(BINDING_GUID);
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/service_credential_bindings/"
                                                             + BINDING_GUID))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.DELETE))
               .andRespond(MockRestResponseCreators.withStatus(HttpStatus.ACCEPTED)
                                                   .location(URI.create(MockControllerClientFactory.BASE_URL + "/v3/jobs/" + JOB_GUID)));

        Optional<String> jobId = operations.deleteServiceBinding(bindingGuid);

        Assertions.assertTrue(jobId.isPresent());
        Assertions.assertEquals(JOB_GUID, jobId.get());
        factory.verify();
    }

    @Test
    void testGetServiceBindingReturnsMappedBinding() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/service_credential_bindings?guids="
                                                             + BINDING_GUID + "&per_page=5000"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess(getBindingListJson(), MediaType.APPLICATION_JSON));

        CloudServiceBinding result = operations.getServiceBinding(UUID.fromString(BINDING_GUID));

        Assertions.assertEquals(UUID.fromString(APP_GUID), result.getApplicationGuid());
        Assertions.assertEquals(UUID.fromString(SERVICE_INSTANCE_GUID), result.getServiceInstanceGuid());
        factory.verify();
    }

    @Test
    void testGetServiceBindingReturnsNullWhenNoBindingFound() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/service_credential_bindings?guids="
                                                             + BINDING_GUID + "&per_page=5000"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess("{\"resources\":[]}", MediaType.APPLICATION_JSON));

        CloudServiceBinding result = operations.getServiceBinding(UUID.fromString(BINDING_GUID));

        Assertions.assertNull(result);
        factory.verify();
    }

    @Test
    void testGetServiceAppBindingsReturnsMappedList() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL
                                                             + "/v3/service_credential_bindings?service_instance_guids="
                                                             + SERVICE_INSTANCE_GUID + "&type=app&per_page=5000"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess(getBindingListJson(), MediaType.APPLICATION_JSON));

        List<CloudServiceBinding> result = operations.getServiceAppBindings(UUID.fromString(SERVICE_INSTANCE_GUID));

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(UUID.fromString(SERVICE_INSTANCE_GUID), result.getFirst()
                                                                              .getServiceInstanceGuid());
        factory.verify();
    }

    @Test
    void testGetAppBindingsReturnsEmptyList() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL
                                                             + "/v3/service_credential_bindings?app_guids=" + APP_GUID + "&per_page=5000"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess("{\"resources\":[]}", MediaType.APPLICATION_JSON));

        List<CloudServiceBinding> result = operations.getAppBindings(UUID.fromString(APP_GUID));

        Assertions.assertTrue(result.isEmpty());
        factory.verify();
    }

    @Test
    void testGetServiceBindingsForApplicationReturnsMappedList() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL
                                                             + "/v3/service_credential_bindings?app_guids=" + APP_GUID
                                                             + "&service_instance_guids=" + SERVICE_INSTANCE_GUID
                                                             + "&per_page=5000"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess(getBindingListJson(), MediaType.APPLICATION_JSON));

        List<CloudServiceBinding> result = operations.getServiceBindingsForApplication(UUID.fromString(APP_GUID),
                                                                                       UUID.fromString(SERVICE_INSTANCE_GUID));

        Assertions.assertEquals(1, result.size());
        factory.verify();
    }

    @Test
    void testGetServiceBindingParametersReturnsMap() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/service_credential_bindings/"
                                                             + BINDING_GUID + "/parameters"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess("{\"foo\":\"bar\"}", MediaType.APPLICATION_JSON));

        Map<String, Object> result = operations.getServiceBindingParameters(UUID.fromString(BINDING_GUID));

        Assertions.assertEquals("bar", result.get("foo"));
        factory.verify();
    }

    @Test
    void testGetServiceBindingParametersThrowsWhenNotFound() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/service_credential_bindings/"
                                                             + BINDING_GUID + "/parameters"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withStatus(HttpStatus.NOT_FOUND));

        Assertions.assertThrows(CloudOperationException.class,
                                () -> operations.getServiceBindingParameters(UUID.fromString(BINDING_GUID)));
    }

    @Test
    void testUpdateServiceBindingMetadataUpdates() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/service_credential_bindings/"
                                                             + BINDING_GUID))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.PATCH))
               .andRespond(MockRestResponseCreators.withSuccess("{\"guid\":\"" + BINDING_GUID + "\"}", MediaType.APPLICATION_JSON));

        Metadata metadata = Metadata.builder()
                                    .label("key", "value")
                                    .build();

        Assertions.assertDoesNotThrow(() -> operations.updateServiceBindingMetadata(UUID.fromString(BINDING_GUID), metadata));
        factory.verify();
    }

    @Test
    void testUpdateServiceBindingMetadataThrowsOnError() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/service_credential_bindings/"
                                                             + BINDING_GUID))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.PATCH))
               .andRespond(MockRestResponseCreators.withStatus(HttpStatus.UNPROCESSABLE_ENTITY));

        Metadata metadata = Metadata.builder()
                                    .build();

        Assertions.assertThrows(CloudOperationException.class,
                                () -> operations.updateServiceBindingMetadata(UUID.fromString(BINDING_GUID), metadata));
    }

    private void stubAppLookup(String appName) {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/apps?per_page=5000&names=" + appName))
               .andRespond(MockRestResponseCreators.withSuccess(getResourcesJson(APP_GUID), MediaType.APPLICATION_JSON));
    }

    private void stubServiceInstanceLookup(String serviceName) {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/service_instances?per_page=5000&names="
                                                             + serviceName))
               .andRespond(MockRestResponseCreators.withSuccess(getResourcesJson(SERVICE_INSTANCE_GUID), MediaType.APPLICATION_JSON));
    }

    private static String getResourcesJson(String guid) {
        return "{\"resources\":[{\"guid\":\"" + guid + "\"}]}";
    }

    private static String getBindingListJson() {
        return "{\"resources\":[{\"guid\":\"" + BINDING_GUID + "\",\"name\":\"my-binding\",\"type\":\"app\","
            + "\"relationships\":{\"app\":{\"data\":{\"guid\":\"" + APP_GUID + "\"}},"
            + "\"service_instance\":{\"data\":{\"guid\":\"" + SERVICE_INSTANCE_GUID + "\"}}}}]}";
    }

}
