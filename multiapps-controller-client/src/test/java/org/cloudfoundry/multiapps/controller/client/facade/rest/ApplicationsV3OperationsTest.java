package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudApplication;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableStaging;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Metadata;
import org.cloudfoundry.multiapps.controller.client.facade.dto.ApplicationToCreateDto;
import org.cloudfoundry.multiapps.controller.client.facade.dto.ImmutableApplicationToCreateDto;
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

class ApplicationsV3OperationsTest {

    private static final String SPACE_GUID = "11111111-2222-3333-4444-555555555555";
    private static final String APP_GUID = "3725a721-6e3f-4b6e-8f2b-1c2d3e4f5a6b";
    private static final String APPS_BASE = MockControllerClientFactory.BASE_URL + "/v3/apps?per_page=5000";

    @Mock
    private CloudSpace target;

    private MockControllerClientFactory factory;

    private ApplicationsV3Operations operations;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        factory = MockControllerClientFactory.create();
        operations = new ApplicationsV3Operations(factory.client(), target);
    }

    @Test
    void testCreateApplicationPostsAndReturnsGuid() {
        Mockito.when(target.getGuid())
               .thenReturn(UUID.fromString(SPACE_GUID));

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/apps"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
               .andRespond(MockRestResponseCreators.withSuccess(appJson(APP_GUID, "my-app", "STOPPED"), MediaType.APPLICATION_JSON));

        ApplicationToCreateDto dto = ImmutableApplicationToCreateDto.builder()
                                                                    .name("my-app")
                                                                    .build();

        UUID result = operations.createApplication(dto);

        Assertions.assertEquals(UUID.fromString(APP_GUID), result);
        factory.verify();
    }

    @Test
    void testCreateApplicationWithScaleAlsoScalesWebProcess() {
        Mockito.when(target.getGuid())
               .thenReturn(UUID.fromString(SPACE_GUID));

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/apps"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
               .andRespond(MockRestResponseCreators.withSuccess(appJson(APP_GUID, "my-app", "STOPPED"), MediaType.APPLICATION_JSON));

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID
                                                             + "/processes/web/actions/scale"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
               .andRespond(MockRestResponseCreators.withSuccess());

        ApplicationToCreateDto dto = ImmutableApplicationToCreateDto.builder()
                                                                    .name("my-app")
                                                                    .memoryInMb(512)
                                                                    .diskQuotaInMb(1024)
                                                                    .staging(ImmutableStaging.builder()
                                                                                             .addBuildpacks("java_buildpack")
                                                                                             .stackName("cflinuxfs4")
                                                                                             .build())
                                                                    .env(Map.of("FOO", "bar"))
                                                                    .metadata(Metadata.builder()
                                                                                      .label("k", "v")
                                                                                      .build())
                                                                    .build();

        UUID result = operations.createApplication(dto);

        Assertions.assertEquals(UUID.fromString(APP_GUID), result);
        factory.verify();
    }

    @Test
    void testCreateApplicationThrowsWhenTargetGuidIsNull() {
        Mockito.when(target.getGuid())
               .thenReturn(null);

        ApplicationToCreateDto dto = ImmutableApplicationToCreateDto.builder()
                                                                    .name("my-app")
                                                                    .build();

        Assertions.assertThrows(CloudOperationException.class, () -> operations.createApplication(dto));
    }

    @Test
    void testDeleteApplicationLooksUpGuidAndDeletes() {
        Mockito.when(target.getGuid())
               .thenReturn(null);

        stubAppLookupByName("my-app", APP_GUID);

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.DELETE))
               .andRespond(MockRestResponseCreators.withSuccess());

        operations.deleteApplication("my-app");

        factory.verify();
    }

    @Test
    void testDeleteApplicationThrowsWhenApplicationNotFound() {
        Mockito.when(target.getGuid())
               .thenReturn(null);

        stubEmptyAppLookupByName("missing-app");

        Assertions.assertThrows(CloudOperationException.class, () -> operations.deleteApplication("missing-app"));
    }

    @Test
    void testGetApplicationReturnsMappedApplication() {
        Mockito.when(target.getGuid())
               .thenReturn(null);

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(APPS_BASE + "&names=my-app"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess(listJson(appJson(APP_GUID, "my-app", "STARTED")),
                                                                MediaType.APPLICATION_JSON));

        CloudApplication result = operations.getApplication("my-app");

        Assertions.assertEquals("my-app", result.getName());
        Assertions.assertEquals(CloudApplication.State.STARTED, result.getState());
        Assertions.assertEquals(UUID.fromString(APP_GUID), result.getGuid());
        factory.verify();
    }

    @Test
    void testGetApplicationThrowsWhenRequiredAndNotFound() {
        Mockito.when(target.getGuid())
               .thenReturn(null);

        stubEmptyAppLookupByName("missing-app");

        Assertions.assertThrows(CloudOperationException.class, () -> operations.getApplication("missing-app"));
    }

    @Test
    void testGetApplicationReturnsNullWhenNotRequiredAndNotFound() {
        Mockito.when(target.getGuid())
               .thenReturn(null);

        stubEmptyAppLookupByName("missing-app");

        CloudApplication result = operations.getApplication("missing-app", false);

        Assertions.assertNull(result);
        factory.verify();
    }

    @Test
    void testGetApplicationGuidReturnsGuid() {
        Mockito.when(target.getGuid())
               .thenReturn(null);

        stubAppLookupByName("my-app", APP_GUID);

        UUID result = operations.getApplicationGuid("my-app");

        Assertions.assertEquals(UUID.fromString(APP_GUID), result);
        factory.verify();
    }

    @Test
    void testGetApplicationQueryIncludesSpaceGuidWhenPresent() {
        Mockito.when(target.getGuid())
               .thenReturn(UUID.fromString(SPACE_GUID));
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(APPS_BASE + "&space_guids=" + SPACE_GUID + "&names=my-app"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess(listJson(appJson(APP_GUID, "my-app", "STARTED")),
                                                                MediaType.APPLICATION_JSON));

        CloudApplication result = operations.getApplication("my-app");

        Assertions.assertEquals("my-app", result.getName());
        factory.verify();
    }

    @Test
    void testGetApplicationNameReturnsName() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess(appJson(APP_GUID, "my-app", "STARTED"), MediaType.APPLICATION_JSON));

        String result = operations.getApplicationName(UUID.fromString(APP_GUID));

        Assertions.assertEquals("my-app", result);
        factory.verify();
    }

    @Test
    void testGetApplicationEnvironmentByGuidReturnsVariables() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID
                                                             + "/environment_variables"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess("{\"var\":{\"FOO\":\"bar\"}}", MediaType.APPLICATION_JSON));

        Map<String, String> result = operations.getApplicationEnvironment(UUID.fromString(APP_GUID));

        Assertions.assertEquals(Map.of("FOO", "bar"), result);
        factory.verify();
    }

    @Test
    void testGetApplicationEnvironmentByGuidReturnsEmptyWhenNoVars() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID
                                                             + "/environment_variables"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess("{}", MediaType.APPLICATION_JSON));

        Map<String, String> result = operations.getApplicationEnvironment(UUID.fromString(APP_GUID));

        Assertions.assertTrue(result.isEmpty());
        factory.verify();
    }

    @Test
    void testGetApplicationEnvironmentByNameResolvesGuidFirst() {
        Mockito.when(target.getGuid())
               .thenReturn(null);

        stubAppLookupByName("my-app", APP_GUID);

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID
                                                             + "/environment_variables"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess("{\"var\":{\"A\":\"B\"}}", MediaType.APPLICATION_JSON));

        Map<String, String> result = operations.getApplicationEnvironment("my-app");

        Assertions.assertEquals(Map.of("A", "B"), result);
        factory.verify();
    }

    @Test
    void testGetApplicationsReturnsMappedList() {
        Mockito.when(target.getGuid())
               .thenReturn(null);

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(APPS_BASE))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess(listJson(appJson(APP_GUID, "my-app", "STARTED")),
                                                                MediaType.APPLICATION_JSON));

        List<CloudApplication> result = operations.getApplications();

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("my-app", result.getFirst()
                                                .getName());
        factory.verify();
    }

    @Test
    void testGetApplicationsReturnsEmptyList() {
        Mockito.when(target.getGuid())
               .thenReturn(null);

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(APPS_BASE))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess("{\"resources\":[]}", MediaType.APPLICATION_JSON));

        List<CloudApplication> result = operations.getApplications();

        Assertions.assertTrue(result.isEmpty());
        factory.verify();
    }

    @Test
    void testGetApplicationsByMetadataLabelSelectorAppendsSelector() {
        Mockito.when(target.getGuid())
               .thenReturn(null);

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(APPS_BASE + "&label_selector=env"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess(listJson(appJson(APP_GUID, "my-app", "STARTED")),
                                                                MediaType.APPLICATION_JSON));

        List<CloudApplication> result = operations.getApplicationsByMetadataLabelSelector("env");

        Assertions.assertEquals(1, result.size());
        factory.verify();
    }

    @Test
    void testGetApplicationsByMetadataLabelSelectorWithNullSelector() {
        Mockito.when(target.getGuid())
               .thenReturn(null);

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(APPS_BASE))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess("{\"resources\":[]}", MediaType.APPLICATION_JSON));

        List<CloudApplication> result = operations.getApplicationsByMetadataLabelSelector(null);

        Assertions.assertTrue(result.isEmpty());
        factory.verify();
    }

    @Test
    void testStartApplicationPostsToStartAction() {
        Mockito.when(target.getGuid())
               .thenReturn(null);

        stubAppLookupByName("my-app", APP_GUID);

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID
                                                             + "/actions/start"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
               .andRespond(MockRestResponseCreators.withSuccess());

        operations.startApplication("my-app");

        factory.verify();
    }

    @Test
    void testStopApplicationPostsToStopAction() {
        Mockito.when(target.getGuid())
               .thenReturn(null);

        stubAppLookupByName("my-app", APP_GUID);

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID
                                                             + "/actions/stop"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
               .andRespond(MockRestResponseCreators.withSuccess());

        operations.stopApplication("my-app");

        factory.verify();
    }

    @Test
    void testRenameUpdatesName() {
        Mockito.when(target.getGuid())
               .thenReturn(null);

        stubAppLookupByName("my-app", APP_GUID);

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.PATCH))
               .andRespond(MockRestResponseCreators.withSuccess());

        operations.rename("my-app", "new-name");

        factory.verify();
    }

    @Test
    void testRenameSwallowsServiceUnavailableWhenAlreadyRenamed() {
        Mockito.when(target.getGuid())
               .thenReturn(null);

        stubAppLookupByName("my-app", APP_GUID);

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.PATCH))
               .andRespond(MockRestResponseCreators.withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess(appJson(APP_GUID, "new-name", "STOPPED"), MediaType.APPLICATION_JSON));

        operations.rename("my-app", "new-name");

        factory.verify();
    }

    @Test
    void testRenameRethrowsServiceUnavailableWhenNotRenamed() {
        Mockito.when(target.getGuid())
               .thenReturn(null);

        stubAppLookupByName("my-app", APP_GUID);

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.PATCH))
               .andRespond(MockRestResponseCreators.withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess(appJson(APP_GUID, "still-old", "STOPPED"), MediaType.APPLICATION_JSON));

        Assertions.assertThrows(CloudOperationException.class, () -> operations.rename("my-app", "new-name"));
    }

    @Test
    void testUpdateApplicationInstancesScalesWebProcess() {
        stubScaleFlow();

        operations.updateApplicationInstances("my-app", 3);

        factory.verify();
    }

    @Test
    void testUpdateApplicationMemoryScalesWebProcess() {
        stubScaleFlow();

        operations.updateApplicationMemory("my-app", 512);

        factory.verify();
    }

    @Test
    void testUpdateApplicationDiskQuotaScalesWebProcess() {
        stubScaleFlow();

        operations.updateApplicationDiskQuota("my-app", 2048);

        factory.verify();
    }

    @Test
    void testUpdateApplicationEnvUpdatesEnvVars() {
        Mockito.when(target.getGuid())
               .thenReturn(null);

        stubAppLookupByName("my-app", APP_GUID);

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID
                                                             + "/environment_variables"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.PATCH))
               .andRespond(MockRestResponseCreators.withSuccess());

        operations.updateApplicationEnv("my-app", Map.of("FOO", "bar"));

        factory.verify();
    }

    @Test
    void testBindDropletToAppUpdatesCurrentDroplet() {
        UUID dropletGuid = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID
                                                             + "/relationships/current_droplet"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.PATCH))
               .andRespond(MockRestResponseCreators.withSuccess());

        operations.bindDropletToApp(dropletGuid, UUID.fromString(APP_GUID));

        factory.verify();
    }

    @Test
    void testUpdateApplicationMetadataUpdatesApp() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.PATCH))
               .andRespond(MockRestResponseCreators.withSuccess());

        Metadata metadata = Metadata.builder()
                                    .label("k", "v")
                                    .annotation("a", "b")
                                    .build();

        operations.updateApplicationMetadata(UUID.fromString(APP_GUID), metadata);

        factory.verify();
    }

    private void stubScaleFlow() {
        Mockito.when(target.getGuid())
               .thenReturn(null);

        stubAppLookupByName("my-app", APP_GUID);
        
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID
                                                             + "/processes/web/actions/scale"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
               .andRespond(MockRestResponseCreators.withSuccess());
    }

    private void stubAppLookupByName(String name, String guid) {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(APPS_BASE + "&names=" + name))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess(listJson(appJson(guid, name, "STARTED")), MediaType.APPLICATION_JSON));
    }

    private void stubEmptyAppLookupByName(String name) {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(APPS_BASE + "&names=" + name))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess("{\"resources\":[]}", MediaType.APPLICATION_JSON));
    }

    private static String appJson(String guid, String name, String state) {
        return "{\"guid\":\"" + guid + "\",\"name\":\"" + name + "\",\"state\":\"" + state
            + "\",\"lifecycle\":{\"type\":\"buildpack\",\"data\":{\"buildpacks\":[\"java_buildpack\"],\"stack\":\"cflinuxfs4\"}}}";
    }

    private static String listJson(String... resources) {
        return "{\"resources\":[" + String.join(",", resources) + "]}";
    }

}
