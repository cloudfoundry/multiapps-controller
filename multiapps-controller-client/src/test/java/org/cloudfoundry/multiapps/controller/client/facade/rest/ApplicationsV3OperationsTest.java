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
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;

import static org.mockito.Mockito.when;

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
        when(target.getGuid())
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
        when(target.getGuid())
            .thenReturn(UUID.fromString(SPACE_GUID));

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/apps"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
               .andRespond(MockRestResponseCreators.withSuccess(appJson(APP_GUID, "my-app", "STOPPED"), MediaType.APPLICATION_JSON));

        factory.stubMethod(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID
                                                             + "/processes/web/actions/scale", HttpMethod.POST);

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
        when(target.getGuid())
            .thenReturn(null);

        ApplicationToCreateDto dto = ImmutableApplicationToCreateDto.builder()
                                                                    .name("my-app")
                                                                    .build();

        Assertions.assertThrows(CloudOperationException.class, () -> operations.createApplication(dto));
    }

    @Test
    void testDeleteApplicationLooksUpGuidAndDeletes() {
        when(target.getGuid())
            .thenReturn(null);

        stubAppLookupByName("my-app", APP_GUID);

        factory.stubMethod(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID, HttpMethod.DELETE);

        operations.deleteApplication("my-app");

        factory.verify();
    }

    @Test
    void testDeleteApplicationThrowsWhenApplicationNotFound() {
        when(target.getGuid())
            .thenReturn(null);

        stubEmptyAppLookupByName("missing-app");

        Assertions.assertThrows(CloudOperationException.class, () -> operations.deleteApplication("missing-app"));
    }

    @Test
    void testGetApplicationReturnsMappedApplication() {
        when(target.getGuid())
            .thenReturn(null);

        factory.stubGet(APPS_BASE + "&names=my-app", listJson(appJson(APP_GUID, "my-app", "STARTED")));

        CloudApplication result = operations.getApplication("my-app");

        Assertions.assertEquals("my-app", result.getName());
        Assertions.assertEquals(CloudApplication.State.STARTED, result.getState());
        Assertions.assertEquals(UUID.fromString(APP_GUID), result.getGuid());
        factory.verify();
    }

    @Test
    void testGetApplicationThrowsWhenRequiredAndNotFound() {
        when(target.getGuid())
            .thenReturn(null);

        stubEmptyAppLookupByName("missing-app");

        Assertions.assertThrows(CloudOperationException.class, () -> operations.getApplication("missing-app"));
    }

    @Test
    void testGetApplicationReturnsNullWhenNotRequiredAndNotFound() {
        when(target.getGuid())
            .thenReturn(null);

        stubEmptyAppLookupByName("missing-app");

        CloudApplication result = operations.getApplication("missing-app", false);

        Assertions.assertNull(result);
        factory.verify();
    }

    @Test
    void testGetApplicationGuidReturnsGuid() {
        when(target.getGuid())
            .thenReturn(null);

        stubAppLookupByName("my-app", APP_GUID);

        UUID result = operations.getApplicationGuid("my-app");

        Assertions.assertEquals(UUID.fromString(APP_GUID), result);
        factory.verify();
    }

    @Test
    void testGetApplicationQueryIncludesSpaceGuidWhenPresent() {
        when(target.getGuid())
            .thenReturn(UUID.fromString(SPACE_GUID));
        factory.stubGet(APPS_BASE + "&space_guids=" + SPACE_GUID + "&names=my-app", listJson(appJson(APP_GUID, "my-app", "STARTED")));

        CloudApplication result = operations.getApplication("my-app");

        Assertions.assertEquals("my-app", result.getName());
        factory.verify();
    }

    @Test
    void testGetApplicationNameReturnsName() {
        factory.stubGet(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID, appJson(APP_GUID, "my-app", "STARTED"));

        String result = operations.getApplicationName(UUID.fromString(APP_GUID));

        Assertions.assertEquals("my-app", result);
        factory.verify();
    }

    @Test
    void testGetApplicationEnvironmentByGuidReturnsVariables() {
        factory.stubGet(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID
                                                             + "/environment_variables", "{\"var\":{\"FOO\":\"bar\"}}");

        Map<String, String> result = operations.getApplicationEnvironment(UUID.fromString(APP_GUID));

        Assertions.assertEquals(Map.of("FOO", "bar"), result);
        factory.verify();
    }

    @Test
    void testGetApplicationEnvironmentByGuidReturnsEmptyWhenNoVars() {
        factory.stubGet(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID
                                                             + "/environment_variables", "{}");

        Map<String, String> result = operations.getApplicationEnvironment(UUID.fromString(APP_GUID));

        Assertions.assertTrue(result.isEmpty());
        factory.verify();
    }

    @Test
    void testGetApplicationEnvironmentByNameResolvesGuidFirst() {
        when(target.getGuid())
            .thenReturn(null);

        stubAppLookupByName("my-app", APP_GUID);

        factory.stubGet(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID
                                                             + "/environment_variables", "{\"var\":{\"A\":\"B\"}}");

        Map<String, String> result = operations.getApplicationEnvironment("my-app");

        Assertions.assertEquals(Map.of("A", "B"), result);
        factory.verify();
    }

    @Test
    void testGetApplicationsReturnsMappedList() {
        when(target.getGuid())
            .thenReturn(null);

        factory.stubGet(APPS_BASE, listJson(appJson(APP_GUID, "my-app", "STARTED")));

        List<CloudApplication> result = operations.getApplications();

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("my-app", result.getFirst()
                                                .getName());
        factory.verify();
    }

    @Test
    void testGetApplicationsReturnsEmptyList() {
        when(target.getGuid())
            .thenReturn(null);

        factory.stubGet(APPS_BASE, "{\"resources\":[]}");

        List<CloudApplication> result = operations.getApplications();

        Assertions.assertTrue(result.isEmpty());
        factory.verify();
    }

    @Test
    void testGetApplicationsByMetadataLabelSelectorAppendsSelector() {
        when(target.getGuid())
            .thenReturn(null);

        factory.stubGet(APPS_BASE + "&label_selector=env", listJson(appJson(APP_GUID, "my-app", "STARTED")));

        List<CloudApplication> result = operations.getApplicationsByMetadataLabelSelector("env");

        Assertions.assertEquals(1, result.size());
        factory.verify();
    }

    @Test
    void testGetApplicationsByMetadataLabelSelectorWithNullSelector() {
        when(target.getGuid())
            .thenReturn(null);

        factory.stubGet(APPS_BASE, "{\"resources\":[]}");

        List<CloudApplication> result = operations.getApplicationsByMetadataLabelSelector(null);

        Assertions.assertTrue(result.isEmpty());
        factory.verify();
    }

    @Test
    void testStartApplicationPostsToStartAction() {
        when(target.getGuid())
            .thenReturn(null);

        stubAppLookupByName("my-app", APP_GUID);

        factory.stubMethod(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID
                                                             + "/actions/start", HttpMethod.POST);

        operations.startApplication("my-app");

        factory.verify();
    }

    @Test
    void testStopApplicationPostsToStopAction() {
        when(target.getGuid())
            .thenReturn(null);

        stubAppLookupByName("my-app", APP_GUID);

        factory.stubMethod(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID
                                                             + "/actions/stop", HttpMethod.POST);

        operations.stopApplication("my-app");

        factory.verify();
    }

    @Test
    void testRenameUpdatesName() {
        when(target.getGuid())
            .thenReturn(null);

        stubAppLookupByName("my-app", APP_GUID);

        factory.stubMethod(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID, HttpMethod.PATCH);

        operations.rename("my-app", "new-name");

        factory.verify();
    }

    @Test
    void testRenameSwallowsServiceUnavailableWhenAlreadyRenamed() {
        when(target.getGuid())
            .thenReturn(null);

        stubAppLookupByName("my-app", APP_GUID);

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.PATCH))
               .andRespond(MockRestResponseCreators.withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        factory.stubGet(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID, appJson(APP_GUID, "new-name", "STOPPED"));

        operations.rename("my-app", "new-name");

        factory.verify();
    }

    @Test
    void testRenameRethrowsServiceUnavailableWhenNotRenamed() {
        when(target.getGuid())
            .thenReturn(null);

        stubAppLookupByName("my-app", APP_GUID);

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.PATCH))
               .andRespond(MockRestResponseCreators.withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        factory.stubGet(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID, appJson(APP_GUID, "still-old", "STOPPED"));

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
        when(target.getGuid())
            .thenReturn(null);

        stubAppLookupByName("my-app", APP_GUID);

        factory.stubMethod(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID
                                                             + "/environment_variables", HttpMethod.PATCH);

        operations.updateApplicationEnv("my-app", Map.of("FOO", "bar"));

        factory.verify();
    }

    @Test
    void testBindDropletToAppUpdatesCurrentDroplet() {
        UUID dropletGuid = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

        factory.stubMethod(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID
                                                             + "/relationships/current_droplet", HttpMethod.PATCH);

        operations.bindDropletToApp(dropletGuid, UUID.fromString(APP_GUID));

        factory.verify();
    }

    @Test
    void testUpdateApplicationMetadataUpdatesApp() {
        factory.stubMethod(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID, HttpMethod.PATCH);

        Metadata metadata = Metadata.builder()
                                    .label("k", "v")
                                    .annotation("a", "b")
                                    .build();

        operations.updateApplicationMetadata(UUID.fromString(APP_GUID), metadata);

        factory.verify();
    }

    private void stubScaleFlow() {
        when(target.getGuid())
            .thenReturn(null);

        stubAppLookupByName("my-app", APP_GUID);

        factory.stubMethod(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID
                                                             + "/processes/web/actions/scale", HttpMethod.POST);
    }

    private void stubAppLookupByName(String name, String guid) {
        factory.stubGet(APPS_BASE + "&names=" + name, listJson(appJson(guid, name, "STARTED")));
    }

    private void stubEmptyAppLookupByName(String name) {
        factory.stubGet(APPS_BASE + "&names=" + name, "{\"resources\":[]}");
    }

    private static String appJson(String guid, String name, String state) {
        return "{\"guid\":\"" + guid + "\",\"name\":\"" + name + "\",\"state\":\"" + state
            + "\",\"lifecycle\":{\"type\":\"buildpack\",\"data\":{\"buildpacks\":[\"java_buildpack\"],\"stack\":\"cflinuxfs4\"}}}";
    }

    private static String listJson(String... resources) {
        return "{\"resources\":[" + String.join(",", resources) + "]}";
    }

}
