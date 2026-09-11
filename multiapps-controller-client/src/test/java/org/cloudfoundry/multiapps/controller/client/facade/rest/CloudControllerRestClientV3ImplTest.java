package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudApplication;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudBuild;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudDomain;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudEvent;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudMetadata;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudRoute;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceInstance;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudStack;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudTask;
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
import org.springframework.web.client.RestClient;

class CloudControllerRestClientV3ImplTest {

    private static final String SPACE_GUID = "11111111-2222-3333-4444-555555555555";
    private static final String APP_GUID = "3725a721-6e3f-4b6e-8f2b-1c2d3e4f5a6b";
    private static final String TASK_GUID = "99999999-8888-7777-6666-555555555555";
    private static final String BUILD_GUID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
    private static final String PACKAGE_GUID = "bbbbbbbb-cccc-dddd-eeee-ffffffffffff";
    private static final String SERVICE_INSTANCE_GUID = "cccccccc-dddd-eeee-ffff-000000000000";
    private static final String DOMAIN_NAME = "example.com";
    private static final String APPS_BASE = MockControllerClientFactory.BASE_URL + "/v3/apps?per_page=5000";

    @Mock
    private CloudSpace target;
    @Mock
    private CloudMetadata targetMetadata;

    private MockControllerClientFactory factory;
    private CloudControllerRestClientV3Impl client;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        factory = MockControllerClientFactory.create();
        Function<Duration, RestClient> uploadRestClientFactory = duration -> factory.client()
                                                                                    .getRestClient();
        client = new CloudControllerRestClientV3Impl(target, factory.client(), uploadRestClientFactory);
    }

    @Test
    void testGetTargetReturnsInjectedSpace() {
        Assertions.assertSame(target, client.getTarget());
    }

    @Test
    void testGetStackReturnsMappedStack() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(
                   MockControllerClientFactory.BASE_URL + "/v3/stacks?names=cflinuxfs4&per_page=5000"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess(getStackListJson(), MediaType.APPLICATION_JSON));

        CloudStack result = client.getStack("cflinuxfs4");

        Assertions.assertEquals("cflinuxfs4", result.getName());
        factory.verify();
    }

    @Test
    void testGetStacksReturnsMappedList() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/stacks?per_page=5000"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess(getStackListJson(), MediaType.APPLICATION_JSON));

        List<CloudStack> result = client.getStacks();

        Assertions.assertEquals(1, result.size());
        factory.verify();
    }

    @Test
    void testGetDomainsReturnsMappedList() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/domains?per_page=5000"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess(getDomainListJson(), MediaType.APPLICATION_JSON));

        List<CloudDomain> result = client.getDomains();

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(DOMAIN_NAME, result.getFirst()
                                                   .getName());
        factory.verify();
    }

    @Test
    void testGetSharedDomainsReturnsMappedList() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/domains?per_page=5000"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess(getDomainListJson(), MediaType.APPLICATION_JSON));

        List<CloudDomain> result = client.getSharedDomains();

        Assertions.assertEquals(1, result.size());
        factory.verify();
    }

    @Test
    void testGetApplicationReturnsMappedApplication() {
        Mockito.when(target.getGuid())
               .thenReturn(null);

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(APPS_BASE + "&names=my-app"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess(getAppListJson(), MediaType.APPLICATION_JSON));

        CloudApplication result = client.getApplication("my-app");

        Assertions.assertEquals("my-app", result.getName());
        Assertions.assertEquals(UUID.fromString(APP_GUID), result.getGuid());
        factory.verify();
    }

    @Test
    void testGetApplicationsReturnsMappedList() {
        Mockito.when(target.getGuid())
               .thenReturn(null);

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(APPS_BASE))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess(getAppListJson(), MediaType.APPLICATION_JSON));

        List<CloudApplication> result = client.getApplications();

        Assertions.assertEquals(1, result.size());
        factory.verify();
    }

    @Test
    void testGetApplicationGuidReturnsGuid() {
        Mockito.when(target.getGuid())
               .thenReturn(null);

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(APPS_BASE + "&names=my-app"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess(getAppListJson(), MediaType.APPLICATION_JSON));

        UUID result = client.getApplicationGuid("my-app");

        Assertions.assertEquals(UUID.fromString(APP_GUID), result);
        factory.verify();
    }

    @Test
    void testGetApplicationNameReturnsName() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess(getAppJson(), MediaType.APPLICATION_JSON));

        String result = client.getApplicationName(UUID.fromString(APP_GUID));

        Assertions.assertEquals("my-app", result);
        factory.verify();
    }

    @Test
    void testGetApplicationEnvironmentReturnsVariables() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID
                                                             + "/environment_variables"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess("{\"var\":{\"FOO\":\"bar\"}}", MediaType.APPLICATION_JSON));

        Map<String, String> result = client.getApplicationEnvironment(UUID.fromString(APP_GUID));

        Assertions.assertEquals(Map.of("FOO", "bar"), result);
        factory.verify();
    }

    @Test
    void testCreateApplicationWithoutStagingOrRoutesOnlyPosts() {
        Mockito.when(target.getGuid())
               .thenReturn(UUID.fromString(SPACE_GUID));

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/apps"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
               .andRespond(MockRestResponseCreators.withSuccess(getAppJson(), MediaType.APPLICATION_JSON));

        ApplicationToCreateDto dto = ImmutableApplicationToCreateDto.builder()
                                                                    .name("my-app")
                                                                    .build();

        client.createApplication(dto);

        factory.verify();
    }

    @Test
    void testCreateApplicationWithStagingUpdatesStagingThroughProcess() {
        Mockito.when(target.getGuid())
               .thenReturn(UUID.fromString(SPACE_GUID));

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/apps"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
               .andRespond(MockRestResponseCreators.withSuccess(getAppJson(), MediaType.APPLICATION_JSON));

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(APPS_BASE + "&space_guids=" + SPACE_GUID + "&names=my-app"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess(getAppListJson(), MediaType.APPLICATION_JSON));

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.PATCH))
               .andRespond(MockRestResponseCreators.withSuccess());

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID
                                                             + "/processes/web"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess(getWebProcessJson(), MediaType.APPLICATION_JSON));

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/processes/" + APP_GUID))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.PATCH))
               .andRespond(MockRestResponseCreators.withSuccess());

        ApplicationToCreateDto dto = ImmutableApplicationToCreateDto.builder()
                                                                    .name("my-app")
                                                                    .staging(
                                                                        org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableStaging.builder()
                                                                                                                                                   .command(
                                                                                                                                                       "run.sh")
                                                                                                                                                   .build())
                                                                    .build();

        client.createApplication(dto);

        factory.verify();
    }

    @Test
    void testGetEventsReturnsMappedList() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(
                   MockControllerClientFactory.BASE_URL + "/v3/audit_events?per_page=5000"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess(getAuditEventListJson(), MediaType.APPLICATION_JSON));

        List<CloudEvent> result = client.getEvents();

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("audit.app.update", result.getFirst()
                                                          .getType());
        factory.verify();
    }

    @Test
    void testGetEventsByTargetReturnsMappedList() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/audit_events?per_page=5000"
                                                             + "&target_guids=" + APP_GUID))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess(getAuditEventListJson(), MediaType.APPLICATION_JSON));

        List<CloudEvent> result = client.getEventsByTarget(UUID.fromString(APP_GUID));

        Assertions.assertEquals(1, result.size());
        factory.verify();
    }

    @Test
    void testGetServiceInstanceReturnsMappedInstance() {
        stubTargetSpaceMetadata();
        stubServiceInstanceLookup();

        client.getServiceInstance("my-service", false);

        factory.verify();
    }

    @Test
    void testGetServiceInstanceNameReturnsName() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(
                   MockControllerClientFactory.BASE_URL + "/v3/service_instances/" + SERVICE_INSTANCE_GUID))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess(getServiceInstanceJson(), MediaType.APPLICATION_JSON));

        String result = client.getServiceInstanceName(UUID.fromString(SERVICE_INSTANCE_GUID));

        Assertions.assertEquals("my-service", result);
        factory.verify();
    }

    @Test
    void testGetServiceInstanceWithoutAuxiliaryContentReturnsMappedInstance() {
        stubTargetSpaceMetadata();
        stubServiceInstanceLookup();

        CloudServiceInstance result = client.getServiceInstanceWithoutAuxiliaryContent("my-service");

        Assertions.assertEquals("my-service", result.getName());
        factory.verify();
    }

    @Test
    void testGetRequiredServiceInstanceGuidReturnsGuid() {
        stubTargetSpaceMetadata();
        stubServiceInstanceLookup();

        UUID result = client.getRequiredServiceInstanceGuid("my-service");

        Assertions.assertEquals(UUID.fromString(SERVICE_INSTANCE_GUID), result);
        factory.verify();
    }

    @Test
    void testGetTaskReturnsMappedTask() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/tasks/" + TASK_GUID))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess(getTaskJson(), MediaType.APPLICATION_JSON));

        CloudTask result = client.getTask(UUID.fromString(TASK_GUID));

        Assertions.assertEquals("migrate", result.getName());
        Assertions.assertEquals(CloudTask.State.SUCCEEDED, result.getState());
        factory.verify();
    }

    @Test
    void testGetTasksReturnsMappedList() {
        Mockito.when(target.getGuid())
               .thenReturn(null);

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(APPS_BASE + "&names=my-app"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess(getAppListJson(), MediaType.APPLICATION_JSON));

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/tasks?per_page=5000&app_guids="
                                                             + APP_GUID))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess("{\"resources\":[" + getTaskJson() + "]}", MediaType.APPLICATION_JSON));

        List<CloudTask> result = client.getTasks("my-app");

        Assertions.assertEquals(1, result.size());
        factory.verify();
    }

    @Test
    void testGetBuildReturnsMappedBuild() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/builds/" + BUILD_GUID))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess(getBuildJson(), MediaType.APPLICATION_JSON));

        CloudBuild result = client.getBuild(UUID.fromString(BUILD_GUID));

        Assertions.assertEquals(CloudBuild.State.STAGED, result.getState());
        factory.verify();
    }

    @Test
    void testGetBuildsForApplicationReturnsMappedList() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID
                                                             + "/builds?per_page=5000"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess("{\"resources\":[" + getBuildJson() + "]}", MediaType.APPLICATION_JSON));

        List<CloudBuild> result = client.getBuildsForApplication(UUID.fromString(APP_GUID));

        Assertions.assertEquals(1, result.size());
        factory.verify();
    }

    @Test
    void testGetBuildsForPackageReturnsMappedList() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/builds?package_guids=" + PACKAGE_GUID
                                                             + "&per_page=5000"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess("{\"resources\":[" + getBuildJson() + "]}", MediaType.APPLICATION_JSON));

        List<CloudBuild> result = client.getBuildsForPackage(UUID.fromString(PACKAGE_GUID));

        Assertions.assertEquals(1, result.size());
        factory.verify();
    }

    @Test
    void testGetApplicationRoutesReturnsMappedList() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID
                                                             + "/routes?per_page=5000"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess("{\"resources\":[" + getRouteJson() + "]}", MediaType.APPLICATION_JSON));

        List<CloudRoute> result = client.getApplicationRoutes(UUID.fromString(APP_GUID));

        Assertions.assertEquals(1, result.size());
        factory.verify();
    }

    @Test
    void testGetBuildPropagatesCloudOperationExceptionOnNotFound() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/builds/" + BUILD_GUID))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withStatus(HttpStatus.NOT_FOUND));

        Assertions.assertThrows(CloudOperationException.class, () -> client.getBuild(UUID.fromString(BUILD_GUID)));
    }

    private void stubTargetSpaceMetadata() {
        Mockito.when(target.getMetadata())
               .thenReturn(targetMetadata);
        Mockito.when(targetMetadata.getGuid())
               .thenReturn(UUID.fromString(SPACE_GUID));
    }

    private void stubServiceInstanceLookup() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(
                   MockControllerClientFactory.BASE_URL + "/v3/service_instances?per_page=5000&space_guids=" + SPACE_GUID
                       + "&names=my-service"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess("{\"resources\":[" + getServiceInstanceJson() + "]}",
                                                                MediaType.APPLICATION_JSON));
    }

    private static String getStackListJson() {
        return "{\"resources\":[{\"guid\":\"" + APP_GUID + "\",\"name\":\"cflinuxfs4\",\"description\":\"Ubuntu\"}]}";
    }

    private static String getDomainListJson() {
        return "{\"resources\":[{\"guid\":\"" + APP_GUID + "\",\"name\":\"" + DOMAIN_NAME + "\"}]}";
    }

    private static String getAppListJson() {
        return "{\"resources\":[" + getAppJson() + "]}";
    }

    private static String getAppJson() {
        return "{\"guid\":\"" + APP_GUID + "\",\"name\":\"my-app\",\"state\":\"STARTED\","
            + "\"lifecycle\":{\"type\":\"buildpack\",\"data\":{\"buildpacks\":[\"java_buildpack\"],\"stack\":\"cflinuxfs4\"}}}";
    }

    private static String getWebProcessJson() {
        return "{\"guid\":\"" + APP_GUID + "\",\"type\":\"web\",\"command\":\"old.sh\",\"instances\":1,\"memory_in_mb\":256,"
            + "\"disk_in_mb\":1024}";
    }

    private static String getAuditEventListJson() {
        return "{\"resources\":[{\"guid\":\"" + APP_GUID + "\",\"type\":\"audit.app.update\","
            + "\"actor\":{\"guid\":\"" + APP_GUID + "\",\"type\":\"user\",\"name\":\"admin\"},"
            + "\"target\":{\"guid\":\"" + APP_GUID + "\",\"type\":\"app\",\"name\":\"my-app\"}}]}";
    }

    private static String getServiceInstanceJson() {
        return "{\"guid\":\"" + SERVICE_INSTANCE_GUID + "\",\"name\":\"my-service\",\"type\":\"managed\"}";
    }

    private static String getTaskJson() {
        return "{\"guid\":\"" + TASK_GUID + "\",\"name\":\"migrate\",\"command\":\"rails db:migrate\",\"state\":\"SUCCEEDED\","
            + "\"memory_in_mb\":256,\"disk_in_mb\":1024}";
    }

    private static String getBuildJson() {
        return "{\"guid\":\"" + BUILD_GUID + "\",\"state\":\"STAGED\",\"package\":{\"guid\":\"" + PACKAGE_GUID + "\"},"
            + "\"droplet\":{\"guid\":\"" + APP_GUID + "\"}}";
    }

    private static String getRouteJson() {
        return "{\"guid\":\"" + APP_GUID + "\",\"host\":\"myhost\",\"path\":\"/mypath\",\"url\":\"myhost.example.com/mypath\","
            + "\"destinations\":[],\"relationships\":{\"space\":{\"data\":{\"guid\":\"" + SPACE_GUID + "\"}},"
            + "\"domain\":{\"data\":{\"guid\":\"" + APP_GUID + "\"}}}}";
    }

}
