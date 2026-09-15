package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudApplication;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudBuild;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudDomain;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudEvent;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudMetadata;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudOrganization;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudPackage;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudProcess;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudRoute;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceBinding;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceBroker;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceInstance;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceKey;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceOffering;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudStack;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudTask;
import org.cloudfoundry.multiapps.controller.client.facade.domain.DropletInfo;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudDomain;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudMetadata;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudOrganization;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudRoute;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudServiceBroker;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudServiceInstance;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudTask;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableStaging;
import org.cloudfoundry.multiapps.controller.client.facade.domain.InstancesInfo;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Metadata;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ServicePlanVisibility;
import org.cloudfoundry.multiapps.controller.client.facade.domain.UserRole;
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
import org.springframework.web.client.RestClient;

import static org.mockito.Mockito.when;

class CloudControllerRestClientV3ImplTest {

    private static final String SPACE_GUID = "11111111-2222-3333-4444-555555555555";
    private static final String APP_GUID = "3725a721-6e3f-4b6e-8f2b-1c2d3e4f5a6b";
    private static final String TASK_GUID = "99999999-8888-7777-6666-555555555555";
    private static final String BUILD_GUID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
    private static final String PACKAGE_GUID = "bbbbbbbb-cccc-dddd-eeee-ffffffffffff";
    private static final String SERVICE_INSTANCE_GUID = "cccccccc-dddd-eeee-ffff-000000000000";
    private static final String BINDING_GUID = "dddddddd-eeee-ffff-0000-111111111111";
    private static final String BROKER_GUID = "eeeeeeee-ffff-0000-1111-222222222222";
    private static final String SERVICE_KEY_GUID = "ffffffff-0000-1111-2222-333333333333";
    private static final String OFFERING_GUID = "a1a1a1a1-b2b2-c3c3-d4d4-e5e5e5e5e5e5";
    private static final String PLAN_GUID = "f6f6f6f6-a7a7-b8b8-c9c9-d0d0d0d0d0d0";
    private static final String DOMAIN_NAME = "example.com";
    private static final String APPS_BASE = MockControllerClientFactory.BASE_URL + "/v3/apps?per_page=5000";
    private static final String SI_BASE = MockControllerClientFactory.BASE_URL + "/v3/service_instances?per_page=5000";
    private static final String BINDINGS = MockControllerClientFactory.BASE_URL + "/v3/service_credential_bindings";

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
        when(target.getGuid())
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
        when(target.getGuid())
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
        when(target.getGuid())
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
        when(target.getGuid())
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
        when(target.getGuid())
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
        when(target.getGuid())
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

        UUID buildGuid = UUID.fromString(BUILD_GUID);
        Assertions.assertThrows(CloudOperationException.class, () -> client.getBuild(buildGuid));
    }

    @Test
    void testGetApplicationWithRequiredFlag() {
        when(target.getGuid()).thenReturn(null);
        stubGet(APPS_BASE + "&names=my-app", getAppListJson());

        CloudApplication result = client.getApplication("my-app", false);

        Assertions.assertEquals("my-app", result.getName());
        factory.verify();
    }

    @Test
    void testGetApplicationEnvironmentByName() {
        when(target.getGuid()).thenReturn(null);
        stubGet(APPS_BASE + "&names=my-app", getAppListJson());
        stubGet(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID + "/environment_variables",
                "{\"var\":{\"FOO\":\"bar\"}}");

        Map<String, String> result = client.getApplicationEnvironment("my-app");

        Assertions.assertEquals(Map.of("FOO", "bar"), result);
        factory.verify();
    }

    @Test
    void testGetApplicationsByMetadataLabelSelector() {
        when(target.getGuid()).thenReturn(null);
        stubGet(APPS_BASE + "&label_selector=env%3Dprod", getAppListJson());

        List<CloudApplication> result = client.getApplicationsByMetadataLabelSelector("env=prod");

        Assertions.assertEquals(1, result.size());
        factory.verify();
    }

    @Test
    void testStartApplication() {
        when(target.getGuid()).thenReturn(null);
        stubGet(APPS_BASE + "&names=my-app", getAppListJson());
        stubMethod(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID + "/actions/start", HttpMethod.POST);

        client.startApplication("my-app");

        factory.verify();
    }

    @Test
    void testStopApplication() {
        when(target.getGuid()).thenReturn(null);
        stubGet(APPS_BASE + "&names=my-app", getAppListJson());
        stubMethod(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID + "/actions/stop", HttpMethod.POST);

        client.stopApplication("my-app");

        factory.verify();
    }

    @Test
    void testRestartApplication() {
        when(target.getGuid()).thenReturn(null);
        stubGet(APPS_BASE + "&names=my-app", getAppListJson());
        stubMethod(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID + "/actions/stop", HttpMethod.POST);
        stubGet(APPS_BASE + "&names=my-app", getAppListJson());
        stubMethod(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID + "/actions/start", HttpMethod.POST);

        client.restartApplication("my-app");

        factory.verify();
    }

    @Test
    void testRenameApplication() {
        when(target.getGuid()).thenReturn(null);
        stubGet(APPS_BASE + "&names=my-app", getAppListJson());
        stubMethod(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID, HttpMethod.PATCH);

        client.rename("my-app", "new-name");

        factory.verify();
    }

    @Test
    void testUpdateApplicationInstances() {
        when(target.getGuid()).thenReturn(null);
        stubGet(APPS_BASE + "&names=my-app", getAppListJson());
        stubMethod(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID + "/processes/web/actions/scale", HttpMethod.POST);

        client.updateApplicationInstances("my-app", 3);

        factory.verify();
    }

    @Test
    void testUpdateApplicationMemory() {
        when(target.getGuid()).thenReturn(null);
        stubGet(APPS_BASE + "&names=my-app", getAppListJson());
        stubMethod(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID + "/processes/web/actions/scale", HttpMethod.POST);

        client.updateApplicationMemory("my-app", 512);

        factory.verify();
    }

    @Test
    void testUpdateApplicationDiskQuota() {
        when(target.getGuid()).thenReturn(null);
        stubGet(APPS_BASE + "&names=my-app", getAppListJson());
        stubMethod(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID + "/processes/web/actions/scale", HttpMethod.POST);

        client.updateApplicationDiskQuota("my-app", 2048);

        factory.verify();
    }

    @Test
    void testUpdateApplicationEnv() {
        when(target.getGuid()).thenReturn(null);
        stubGet(APPS_BASE + "&names=my-app", getAppListJson());
        stubMethod(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID + "/environment_variables", HttpMethod.PATCH);

        client.updateApplicationEnv("my-app", Map.of("FOO", "bar"));

        factory.verify();
    }

    @Test
    void testBindDropletToApp() {
        stubMethod(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID + "/relationships/current_droplet", HttpMethod.PATCH);

        client.bindDropletToApp(UUID.fromString(BUILD_GUID), UUID.fromString(APP_GUID));

        factory.verify();
    }

    @Test
    void testUpdateApplicationMetadata() {
        stubMethod(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID, HttpMethod.PATCH);

        client.updateApplicationMetadata(UUID.fromString(APP_GUID), Metadata.builder()
                                                                            .label("key", "value")
                                                                            .build());

        factory.verify();
    }

    @Test
    void testDeleteApplication() {
        when(target.getGuid()).thenReturn(null);
        stubGet(APPS_BASE + "&names=my-app", getAppListJson());
        stubMethod(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID, HttpMethod.DELETE);

        client.deleteApplication("my-app");

        factory.verify();
    }

    @Test
    void testGetApplicationProcess() {
        stubGet(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID + "/processes/web", getWebProcessJson());

        CloudProcess result = client.getApplicationProcess(UUID.fromString(APP_GUID));

        Assertions.assertNotNull(result);
        factory.verify();
    }

    @Test
    void testGetApplicationInstancesByGuid() {
        stubGet(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID + "/processes/web/stats",
                "{\"resources\":[{\"state\":\"RUNNING\",\"index\":0}]}");

        InstancesInfo result = client.getApplicationInstances(UUID.fromString(APP_GUID));

        Assertions.assertNotNull(result);
        factory.verify();
    }

    @Test
    void testGetApplicationSshEnabled() {
        stubGet(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID + "/ssh_enabled", "{\"enabled\":true}");

        Assertions.assertTrue(client.getApplicationSshEnabled(UUID.fromString(APP_GUID)));
        factory.verify();
    }

    @Test
    void testGetApplicationFeatures() {
        stubGet(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID + "/features?per_page=5000",
                "{\"resources\":[{\"name\":\"ssh\",\"enabled\":true}]}");

        Map<String, Boolean> result = client.getApplicationFeatures(UUID.fromString(APP_GUID));

        Assertions.assertEquals(Boolean.TRUE, result.get("ssh"));
        factory.verify();
    }

    @Test
    void testGetCurrentDropletForApplication() {
        stubGet(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID + "/droplets/current",
                "{\"guid\":\"" + BUILD_GUID + "\",\"links\":{\"package\":{\"href\":\"https://api.example.com/v3/packages/" + PACKAGE_GUID
                    + "\"}}}");

        DropletInfo result = client.getCurrentDropletForApplication(UUID.fromString(APP_GUID));

        Assertions.assertEquals(UUID.fromString(PACKAGE_GUID), result.getPackageGuid());
        factory.verify();
    }

    @Test
    void testUpdateApplicationStaging() {
        when(target.getGuid()).thenReturn(null);
        stubGet(APPS_BASE + "&names=my-app", getAppListJson());
        stubMethod(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID, HttpMethod.PATCH);
        stubGet(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID + "/processes/web", getWebProcessJson());
        stubMethod(MockControllerClientFactory.BASE_URL + "/v3/processes/" + APP_GUID, HttpMethod.PATCH);

        client.updateApplicationStaging("my-app", ImmutableStaging.builder()
                                                                  .command("run.sh")
                                                                  .build());

        factory.verify();
    }

    @Test
    void testGetApplicationEvents() {
        when(target.getGuid()).thenReturn(null);
        stubGet(APPS_BASE + "&names=my-app", getAppListJson());
        stubGet(MockControllerClientFactory.BASE_URL + "/v3/audit_events?per_page=5000&target_guids=" + APP_GUID,
                getAuditEventListJson());

        List<CloudEvent> result = client.getApplicationEvents("my-app");

        Assertions.assertEquals(1, result.size());
        factory.verify();
    }

    @Test
    void testGetDefaultDomain() {
        stubTargetOrganization();
        stubGet(MockControllerClientFactory.BASE_URL + "/v3/organizations/" + SPACE_GUID + "/domains/default",
                "{\"guid\":\"" + APP_GUID + "\",\"name\":\"" + DOMAIN_NAME + "\"}");

        CloudDomain result = client.getDefaultDomain();

        Assertions.assertEquals(DOMAIN_NAME, result.getName());
        factory.verify();
    }

    @Test
    void testGetPrivateDomains() {
        stubGet(MockControllerClientFactory.BASE_URL + "/v3/domains?per_page=5000",
                "{\"resources\":[{\"guid\":\"" + APP_GUID + "\",\"name\":\"" + DOMAIN_NAME
                    + "\",\"relationships\":{\"organization\":{\"data\":{\"guid\":\"" + SPACE_GUID + "\"}}}}]}");

        List<CloudDomain> result = client.getPrivateDomains();

        Assertions.assertEquals(1, result.size());
        factory.verify();
    }

    @Test
    void testGetDomainsForOrganization() {
        stubTargetOrganization();
        stubGet(MockControllerClientFactory.BASE_URL + "/v3/organizations/" + SPACE_GUID + "/domains?per_page=5000",
                getDomainListJson());

        List<CloudDomain> result = client.getDomainsForOrganization();

        Assertions.assertEquals(1, result.size());
        factory.verify();
    }

    @Test
    void testAddDomainCreatesWhenMissing() {
        stubTargetOrganization();
        stubGet(MockControllerClientFactory.BASE_URL + "/v3/domains?names=" + DOMAIN_NAME + "&per_page=5000",
                "{\"resources\":[]}");
        stubMethod(MockControllerClientFactory.BASE_URL + "/v3/domains", HttpMethod.POST);

        client.addDomain(DOMAIN_NAME);

        factory.verify();
    }

    @Test
    void testDeleteDomain() {
        stubTargetOrganization();
        stubGet(MockControllerClientFactory.BASE_URL + "/v3/domains?names=" + DOMAIN_NAME + "&per_page=5000", getDomainListJson());
        stubMethod(MockControllerClientFactory.BASE_URL + "/v3/domains/" + APP_GUID, HttpMethod.DELETE);

        client.deleteDomain(DOMAIN_NAME);

        factory.verify();
    }

    @Test
    void testAddRoute() {
        when(target.getGuid()).thenReturn(UUID.fromString(SPACE_GUID));
        stubGet(MockControllerClientFactory.BASE_URL + "/v3/domains?names=" + DOMAIN_NAME + "&per_page=5000", getDomainListJson());
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/routes"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
               .andRespond(MockRestResponseCreators.withSuccess(getRouteJson(), MediaType.APPLICATION_JSON));

        client.addRoute("myhost", DOMAIN_NAME, "/mypath");

        factory.verify();
    }

    @Test
    void testGetRoutes() {
        when(target.getGuid()).thenReturn(UUID.fromString(SPACE_GUID));
        stubGet(MockControllerClientFactory.BASE_URL + "/v3/domains?names=" + DOMAIN_NAME + "&per_page=5000", getDomainListJson());
        stubGet(MockControllerClientFactory.BASE_URL + "/v3/routes?per_page=5000&domain_guids=" + APP_GUID + "&space_guids=" + SPACE_GUID,
                "{\"resources\":[" + getRouteJson() + "]}");

        List<CloudRoute> result = client.getRoutes(DOMAIN_NAME);

        Assertions.assertEquals(1, result.size());
        factory.verify();
    }

    @Test
    void testDeleteOrphanedRoutes() {
        when(target.getGuid()).thenReturn(UUID.fromString(SPACE_GUID));
        stubMethod(MockControllerClientFactory.BASE_URL + "/v3/spaces/" + SPACE_GUID + "/routes?unmapped=true", HttpMethod.DELETE);

        client.deleteOrphanedRoutes();

        factory.verify();
    }

    @Test
    void testDeleteRoute() {
        when(target.getGuid()).thenReturn(UUID.fromString(SPACE_GUID));
        stubGet(MockControllerClientFactory.BASE_URL + "/v3/domains?names=" + DOMAIN_NAME + "&per_page=5000", getDomainListJson());
        stubGet(MockControllerClientFactory.BASE_URL + "/v3/routes?per_page=5000&domain_guids=" + APP_GUID + "&space_guids=" + SPACE_GUID
                    + "&hosts=myhost&paths=/mypath", "{\"resources\":[" + getRouteJson() + "]}");
        stubMethod(MockControllerClientFactory.BASE_URL + "/v3/routes/" + APP_GUID, HttpMethod.DELETE);

        client.deleteRoute("myhost", DOMAIN_NAME, "/mypath");

        factory.verify();
    }

    @Test
    void testUpdateApplicationRoutesAddsRoute() {
        when(target.getGuid()).thenReturn(UUID.fromString(SPACE_GUID));
        stubGet(APPS_BASE + "&names=my-app&space_guids=" + SPACE_GUID, "{\"resources\":[{\"guid\":\"" + APP_GUID + "\"}]}");
        stubGet(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID + "/routes?per_page=5000", "{\"resources\":[]}");
        stubGet(MockControllerClientFactory.BASE_URL + "/v3/domains?names=" + DOMAIN_NAME + "&per_page=5000",
                "{\"resources\":[{\"guid\":\"" + APP_GUID + "\",\"name\":\"" + DOMAIN_NAME + "\"}]}");
        stubGet(MockControllerClientFactory.BASE_URL + "/v3/routes?per_page=5000&domain_guids=" + APP_GUID + "&space_guids=" + SPACE_GUID
                    + "&hosts=myhost&paths=/mypath", "{\"resources\":[]}");

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/routes"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
               .andRespond(MockRestResponseCreators.withSuccess(getRouteJson(), MediaType.APPLICATION_JSON));
        stubMethod(MockControllerClientFactory.BASE_URL + "/v3/routes/" + APP_GUID + "/destinations", HttpMethod.POST);

        CloudRoute route = ImmutableCloudRoute.builder()
                                              .domain(ImmutableCloudDomain.builder()
                                                                          .name(DOMAIN_NAME)
                                                                          .build())
                                              .host("myhost")
                                              .path("/mypath")
                                              .url("myhost.example.com/mypath")
                                              .build();

        client.updateApplicationRoutes("my-app", Set.of(route));

        factory.verify();
    }

    @Test
    void testGetServiceInstanceSingleArgument() {
        stubTargetSpaceMetadata();
        stubServiceInstanceLookupWithPlan();
        stubGet(MockControllerClientFactory.BASE_URL + "/v3/service_plans/" + PLAN_GUID, "{\"guid\":\"" + PLAN_GUID
            + "\",\"name\":\"my-plan\"}");

        CloudServiceInstance result = client.getServiceInstance("my-service", false);

        Assertions.assertEquals("my-service", result.getName());
        factory.verify();
    }

    @Test
    void testGetServiceInstanceWithoutAuxiliaryContentRequiredFlag() {
        stubTargetSpaceMetadata();
        stubServiceInstanceLookup();

        CloudServiceInstance result = client.getServiceInstanceWithoutAuxiliaryContent("my-service", false);

        Assertions.assertEquals("my-service", result.getName());
        factory.verify();
    }

    @Test
    void testCreateServiceInstance() {
        stubTargetSpaceMetadata();
        stubGet(MockControllerClientFactory.BASE_URL + "/v3/service_offerings?per_page=5000&space_guids=" + SPACE_GUID
                    + "&names=my-offering", "{\"resources\":[{\"guid\":\"" + OFFERING_GUID + "\",\"name\":\"my-offering\"}]}");
        stubGet(MockControllerClientFactory.BASE_URL + "/v3/service_plans?per_page=5000&service_offering_guids=" + OFFERING_GUID
                    + "&names=my-plan", "{\"resources\":[{\"guid\":\"" + PLAN_GUID + "\",\"name\":\"my-plan\"}]}");
        stubMethod(MockControllerClientFactory.BASE_URL + "/v3/service_instances", HttpMethod.POST);

        CloudServiceInstance serviceInstance = ImmutableCloudServiceInstance.builder()
                                                                            .name("my-service")
                                                                            .label("my-offering")
                                                                            .plan("my-plan")
                                                                            .build();

        client.createServiceInstance(serviceInstance);

        factory.verify();
    }

    @Test
    void testCreateUserProvidedServiceInstance() {
        stubTargetSpaceMetadata();
        stubMethod(MockControllerClientFactory.BASE_URL + "/v3/service_instances", HttpMethod.POST);

        CloudServiceInstance serviceInstance = ImmutableCloudServiceInstance.builder()
                                                                            .name("my-ups")
                                                                            .build();

        client.createUserProvidedServiceInstance(serviceInstance);

        factory.verify();
    }

    @Test
    void testDeleteServiceInstanceByName() {
        stubTargetSpaceMetadata();
        stubServiceInstanceLookup();
        stubMethod(MockControllerClientFactory.BASE_URL + "/v3/service_instances/" + SERVICE_INSTANCE_GUID, HttpMethod.DELETE);

        client.deleteServiceInstance("my-service");

        factory.verify();
    }

    @Test
    void testDeleteServiceInstanceByInstance() {
        stubMethod(MockControllerClientFactory.BASE_URL + "/v3/service_instances/" + SERVICE_INSTANCE_GUID, HttpMethod.DELETE);

        CloudServiceInstance serviceInstance = ImmutableCloudServiceInstance.builder()
                                                                            .name("my-service")
                                                                            .metadata(ImmutableCloudMetadata.builder()
                                                                                                            .guid(
                                                                                                                UUID.fromString(
                                                                                                                    SERVICE_INSTANCE_GUID))
                                                                                                            .build())
                                                                            .build();

        client.deleteServiceInstance(serviceInstance);

        factory.verify();
    }

    @Test
    void testGetServiceInstanceParameters() {
        stubGet(MockControllerClientFactory.BASE_URL + "/v3/service_instances/" + SERVICE_INSTANCE_GUID + "/parameters",
                "{\"foo\":\"bar\"}");

        Map<String, Object> result = client.getServiceInstanceParameters(UUID.fromString(SERVICE_INSTANCE_GUID));

        Assertions.assertEquals("bar", result.get("foo"));
        factory.verify();
    }

    @Test
    void testGetUserProvidedServiceInstanceParameters() {
        stubGet(MockControllerClientFactory.BASE_URL + "/v3/service_instances/" + SERVICE_INSTANCE_GUID + "/credentials",
                "{\"user\":\"admin\"}");

        Map<String, Object> result = client.getUserProvidedServiceInstanceParameters(UUID.fromString(SERVICE_INSTANCE_GUID));

        Assertions.assertEquals("admin", result.get("user"));
        factory.verify();
    }

    @Test
    void testGetServiceInstancesWithoutAuxiliaryContentByNames() {
        stubTargetSpaceMetadata();
        stubGet(SI_BASE + "&space_guids=" + SPACE_GUID + "&names=my-service",
                "{\"resources\":[" + getServiceInstanceJson() + "]}");

        List<CloudServiceInstance> result = client.getServiceInstancesWithoutAuxiliaryContentByNames(List.of("my-service"));

        Assertions.assertEquals(1, result.size());
        factory.verify();
    }

    @Test
    void testGetServiceInstancesByMetadataLabelSelector() {
        stubTargetSpaceMetadata();
        stubGet(SI_BASE + "&space_guids=" + SPACE_GUID + "&label_selector=env",
                "{\"resources\":[{\"guid\":\"" + SERVICE_INSTANCE_GUID + "\",\"name\":\"my-service\",\"type\":\"user-provided\"}]}");

        List<CloudServiceInstance> result = client.getServiceInstancesByMetadataLabelSelector("env");

        Assertions.assertEquals(1, result.size());
        factory.verify();
    }

    @Test
    void testGetServiceInstancesWithoutAuxiliaryContentByMetadataLabelSelector() {
        stubTargetSpaceMetadata();
        stubGet(SI_BASE + "&space_guids=" + SPACE_GUID + "&label_selector=env",
                "{\"resources\":[" + getServiceInstanceJson() + "]}");

        List<CloudServiceInstance> result = client.getServiceInstancesWithoutAuxiliaryContentByMetadataLabelSelector("env");

        Assertions.assertEquals(1, result.size());
        factory.verify();
    }

    @Test
    void testUpdateServiceParameters() {
        stubTargetSpaceMetadata();
        stubServiceInstanceLookup();
        stubMethod(MockControllerClientFactory.BASE_URL + "/v3/service_instances/" + SERVICE_INSTANCE_GUID, HttpMethod.PATCH);

        client.updateServiceParameters("my-service", Map.of("foo", "bar"));

        factory.verify();
    }

    @Test
    void testUpdateServiceTags() {
        stubTargetSpaceMetadata();
        stubServiceInstanceLookup();
        stubMethod(MockControllerClientFactory.BASE_URL + "/v3/service_instances/" + SERVICE_INSTANCE_GUID, HttpMethod.PATCH);

        client.updateServiceTags("my-service", List.of("t1", "t2"));

        factory.verify();
    }

    @Test
    void testUpdateServiceSyslogDrainUrlSkipsManaged() {
        stubTargetSpaceMetadata();
        stubServiceInstanceLookup();

        client.updateServiceSyslogDrainUrl("my-service", "syslog://x");

        factory.verify();
    }

    @Test
    void testUpdateServiceInstanceMetadata() {
        stubMethod(MockControllerClientFactory.BASE_URL + "/v3/service_instances/" + SERVICE_INSTANCE_GUID, HttpMethod.PATCH);

        client.updateServiceInstanceMetadata(UUID.fromString(SERVICE_INSTANCE_GUID), Metadata.builder()
                                                                                             .label("k", "v")
                                                                                             .build());

        factory.verify();
    }

    @Test
    void testBindServiceInstance() {
        when(target.getGuid()).thenReturn(UUID.fromString(SPACE_GUID));
        stubGet(APPS_BASE + "&space_guids=" + SPACE_GUID + "&names=my-app", getAppListJson());
        stubGet(SI_BASE + "&space_guids=" + SPACE_GUID + "&names=my-service",
                "{\"resources\":[" + getServiceInstanceJson() + "]}");
        stubMethod(BINDINGS, HttpMethod.POST);

        Optional<String> result = client.bindServiceInstance("my-binding", "my-app", "my-service");

        Assertions.assertTrue(result.isEmpty());
        factory.verify();
    }

    @Test
    void testBindServiceInstanceWithParameters() {
        when(target.getGuid()).thenReturn(UUID.fromString(SPACE_GUID));
        stubGet(APPS_BASE + "&space_guids=" + SPACE_GUID + "&names=my-app", getAppListJson());
        stubGet(SI_BASE + "&space_guids=" + SPACE_GUID + "&names=my-service",
                "{\"resources\":[" + getServiceInstanceJson() + "]}");
        stubMethod(BINDINGS, HttpMethod.POST);

        client.bindServiceInstance("my-binding", "my-app", "my-service", Map.of("p", "v"));

        factory.verify();
    }

    @Test
    void testGetServiceBinding() {
        stubGet(BINDINGS + "?guids=" + BINDING_GUID + "&per_page=5000",
                "{\"resources\":[" + getBindingJson() + "]}");

        CloudServiceBinding result = client.getServiceBinding(UUID.fromString(BINDING_GUID));

        Assertions.assertNotNull(result);
        factory.verify();
    }

    @Test
    void testGetServiceAppBindings() {
        stubGet(BINDINGS + "?service_instance_guids=" + SERVICE_INSTANCE_GUID + "&type=app&per_page=5000",
                "{\"resources\":[" + getBindingJson() + "]}");

        List<CloudServiceBinding> result = client.getServiceAppBindings(UUID.fromString(SERVICE_INSTANCE_GUID));

        Assertions.assertEquals(1, result.size());
        factory.verify();
    }

    @Test
    void testGetAppBindings() {
        stubGet(BINDINGS + "?app_guids=" + APP_GUID + "&per_page=5000", "{\"resources\":[" + getBindingJson() + "]}");

        List<CloudServiceBinding> result = client.getAppBindings(UUID.fromString(APP_GUID));

        Assertions.assertEquals(1, result.size());
        factory.verify();
    }

    @Test
    void testGetServiceBindingsForApplication() {
        stubGet(BINDINGS + "?app_guids=" + APP_GUID + "&service_instance_guids=" + SERVICE_INSTANCE_GUID + "&per_page=5000",
                "{\"resources\":[" + getBindingJson() + "]}");

        List<CloudServiceBinding> result = client.getServiceBindingsForApplication(UUID.fromString(APP_GUID),
                                                                                   UUID.fromString(SERVICE_INSTANCE_GUID));

        Assertions.assertEquals(1, result.size());
        factory.verify();
    }

    @Test
    void testGetServiceBindingParameters() {
        stubGet(BINDINGS + "/" + BINDING_GUID + "/parameters", "{\"foo\":\"bar\"}");

        Map<String, Object> result = client.getServiceBindingParameters(UUID.fromString(BINDING_GUID));

        Assertions.assertEquals("bar", result.get("foo"));
        factory.verify();
    }

    @Test
    void testUpdateServiceBindingMetadata() {
        stubMethod(BINDINGS + "/" + BINDING_GUID, HttpMethod.PATCH);

        client.updateServiceBindingMetadata(UUID.fromString(BINDING_GUID), Metadata.builder()
                                                                                   .label("k", "v")
                                                                                   .build());

        factory.verify();
    }

    @Test
    void testDeleteServiceBindingByGuid() {
        stubMethod(BINDINGS + "/" + BINDING_GUID, HttpMethod.DELETE);

        Optional<String> result = client.deleteServiceBinding(UUID.fromString(BINDING_GUID));

        Assertions.assertTrue(result.isEmpty());
        factory.verify();
    }

    @Test
    void testUnbindServiceInstanceByName() {
        when(target.getGuid()).thenReturn(UUID.fromString(SPACE_GUID));
        stubGet(APPS_BASE + "&space_guids=" + SPACE_GUID + "&names=my-app", getAppListJson());
        stubGet(SI_BASE + "&space_guids=" + SPACE_GUID + "&names=my-service",
                "{\"resources\":[" + getServiceInstanceJson() + "]}");
        stubGet(BINDINGS + "?app_guids=" + APP_GUID + "&service_instance_guids=" + SERVICE_INSTANCE_GUID + "&per_page=5000",
                "{\"resources\":[" + getBindingJson() + "]}");
        stubMethod(BINDINGS + "/" + BINDING_GUID, HttpMethod.DELETE);

        List<String> result = client.unbindServiceInstance("my-app", "my-service");

        Assertions.assertTrue(result.isEmpty());
        factory.verify();
    }

    @Test
    void testUnbindServiceInstanceByGuid() {
        stubGet(BINDINGS + "?app_guids=" + APP_GUID + "&service_instance_guids=" + SERVICE_INSTANCE_GUID + "&per_page=5000",
                "{\"resources\":[" + getBindingJson() + "]}");
        stubMethod(BINDINGS + "/" + BINDING_GUID, HttpMethod.DELETE);

        client.unbindServiceInstance(UUID.fromString(APP_GUID), UUID.fromString(SERVICE_INSTANCE_GUID));

        factory.verify();
    }

    @Test
    void testGetServiceKey() {
        stubTargetSpaceMetadata();
        stubServiceInstanceLookupWithPlan();
        stubGet(MockControllerClientFactory.BASE_URL + "/v3/service_plans/" + PLAN_GUID, "{\"guid\":\"" + PLAN_GUID
            + "\",\"name\":\"my-plan\"}");
        stubGet(BINDINGS + "?per_page=5000&type=key&service_instance_guids=" + SERVICE_INSTANCE_GUID + "&names=my-key",
                "{\"resources\":[" + getServiceKeyJson() + "]}");
        stubGet(BINDINGS + "/" + SERVICE_KEY_GUID + "/details", "{\"credentials\":{\"user\":\"admin\"}}");

        CloudServiceKey result = client.getServiceKey("my-service", "my-key");

        Assertions.assertNotNull(result);
        factory.verify();
    }

    @Test
    void testGetServiceKeysByName() {
        stubTargetSpaceMetadata();
        stubServiceInstanceLookupWithPlan();
        stubGet(MockControllerClientFactory.BASE_URL + "/v3/service_plans/" + PLAN_GUID, "{\"guid\":\"" + PLAN_GUID
            + "\",\"name\":\"my-plan\"}");
        stubGet(BINDINGS + "?per_page=5000&type=key&service_instance_guids=" + SERVICE_INSTANCE_GUID,
                "{\"resources\":[" + getServiceKeyJson() + "]}");

        List<CloudServiceKey> result = client.getServiceKeys("my-service");

        Assertions.assertEquals(1, result.size());
        factory.verify();
    }

    @Test
    void testCreateServiceKeyWithParameters() {
        stubTargetSpaceMetadata();
        stubServiceInstanceLookupWithPlan();
        stubGet(MockControllerClientFactory.BASE_URL + "/v3/service_plans/" + PLAN_GUID, "{\"guid\":\"" + PLAN_GUID
            + "\",\"name\":\"my-plan\"}");
        stubMethod(BINDINGS, HttpMethod.POST);

        Optional<String> result = client.createServiceKey("my-service", "my-key", Map.of("p", "v"));

        Assertions.assertTrue(result.isEmpty());
        factory.verify();
    }

    @Test
    void testCreateServiceBroker() {
        stubMethod(MockControllerClientFactory.BASE_URL + "/v3/service_brokers", HttpMethod.POST);

        CloudServiceBroker broker = ImmutableCloudServiceBroker.builder()
                                                               .name("my-broker")
                                                               .url("https://broker.example.com")
                                                               .username("user")
                                                               .password("pass")
                                                               .build();

        Assertions.assertNull(client.createServiceBroker(broker));
        factory.verify();
    }

    @Test
    void testGetServiceBroker() {
        stubGet(MockControllerClientFactory.BASE_URL + "/v3/service_brokers?names=my-broker&per_page=5000", getBrokerListJson());

        CloudServiceBroker result = client.getServiceBroker("my-broker");

        Assertions.assertEquals("my-broker", result.getName());
        factory.verify();
    }

    @Test
    void testGetServiceBrokerWithRequiredFlag() {
        stubGet(MockControllerClientFactory.BASE_URL + "/v3/service_brokers?names=my-broker&per_page=5000", getBrokerListJson());

        Assertions.assertNotNull(client.getServiceBroker("my-broker", false));
        factory.verify();
    }

    @Test
    void testGetServiceBrokers() {
        stubGet(MockControllerClientFactory.BASE_URL + "/v3/service_brokers?per_page=5000", getBrokerListJson());

        List<CloudServiceBroker> result = client.getServiceBrokers();

        Assertions.assertEquals(1, result.size());
        factory.verify();
    }

    @Test
    void testUpdateServiceBroker() {
        stubGet(MockControllerClientFactory.BASE_URL + "/v3/service_brokers?names=my-broker&per_page=5000", getBrokerListJson());
        stubMethod(MockControllerClientFactory.BASE_URL + "/v3/service_brokers/" + BROKER_GUID, HttpMethod.PATCH);

        CloudServiceBroker broker = ImmutableCloudServiceBroker.builder()
                                                               .name("my-broker")
                                                               .url("https://broker.example.com")
                                                               .build();

        client.updateServiceBroker(broker);

        factory.verify();
    }

    @Test
    void testDeleteServiceBroker() {
        stubGet(MockControllerClientFactory.BASE_URL + "/v3/service_brokers?names=my-broker&per_page=5000", getBrokerListJson());
        stubMethod(MockControllerClientFactory.BASE_URL + "/v3/service_brokers/" + BROKER_GUID, HttpMethod.DELETE);

        Assertions.assertNull(client.deleteServiceBroker("my-broker"));
        factory.verify();
    }

    @Test
    void testUpdateServicePlanVisibilityForBroker() {
        stubTargetSpaceMetadata();
        stubGet(MockControllerClientFactory.BASE_URL + "/v3/service_brokers?names=my-broker&per_page=5000", getBrokerListJson());
        stubGet(MockControllerClientFactory.BASE_URL + "/v3/service_offerings?service_broker_guids=" + BROKER_GUID + "&space_guids="
                    + SPACE_GUID + "&per_page=5000", "{\"resources\":[{\"guid\":\"" + OFFERING_GUID + "\"}]}");
        stubGet(MockControllerClientFactory.BASE_URL + "/v3/service_plans?service_offering_guids=" + OFFERING_GUID + "&per_page=5000",
                "{\"resources\":[{\"guid\":\"" + PLAN_GUID + "\"}]}");
        stubMethod(MockControllerClientFactory.BASE_URL + "/v3/service_plans/" + PLAN_GUID + "/visibility", HttpMethod.PATCH);

        client.updateServicePlanVisibilityForBroker("my-broker", ServicePlanVisibility.PUBLIC);

        factory.verify();
    }

    @Test
    void testGetServiceOfferings() {
        when(target.getGuid()).thenReturn(UUID.fromString(SPACE_GUID));
        stubGet(MockControllerClientFactory.BASE_URL + "/v3/service_offerings?per_page=5000&space_guids=" + SPACE_GUID,
                "{\"resources\":[{\"guid\":\"" + OFFERING_GUID + "\",\"name\":\"my-offering\"}]}");
        stubGet(MockControllerClientFactory.BASE_URL + "/v3/service_plans?service_offering_guids=" + OFFERING_GUID + "&per_page=5000",
                "{\"resources\":[{\"guid\":\"" + PLAN_GUID + "\",\"name\":\"my-plan\"}]}");

        List<CloudServiceOffering> result = client.getServiceOfferings();

        Assertions.assertEquals(1, result.size());
        factory.verify();
    }

    @Test
    void testRunTask() {
        when(target.getGuid()).thenReturn(null);
        stubGet(APPS_BASE + "&names=my-app", getAppListJson());
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID + "/tasks"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
               .andRespond(MockRestResponseCreators.withSuccess(getTaskJson(), MediaType.APPLICATION_JSON));

        CloudTask task = ImmutableCloudTask.builder()
                                           .name("migrate")
                                           .command("rails db:migrate")
                                           .build();

        CloudTask result = client.runTask("my-app", task);

        Assertions.assertEquals("migrate", result.getName());
        factory.verify();
    }

    @Test
    void testCancelTask() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(
                   MockControllerClientFactory.BASE_URL + "/v3/tasks/" + TASK_GUID + "/actions/cancel"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
               .andRespond(MockRestResponseCreators.withSuccess(getTaskJson(), MediaType.APPLICATION_JSON));

        CloudTask result = client.cancelTask(UUID.fromString(TASK_GUID));

        Assertions.assertEquals("migrate", result.getName());
        factory.verify();
    }

    @Test
    void testCreateBuild() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/builds"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
               .andRespond(MockRestResponseCreators.withSuccess(getBuildJson(), MediaType.APPLICATION_JSON));

        CloudBuild result = client.createBuild(UUID.fromString(PACKAGE_GUID));

        Assertions.assertEquals(CloudBuild.State.STAGED, result.getState());
        factory.verify();
    }

    @Test
    void testGetPackage() {
        stubGet(MockControllerClientFactory.BASE_URL + "/v3/packages/" + PACKAGE_GUID, getPackageJson());

        CloudPackage result = client.getPackage(UUID.fromString(PACKAGE_GUID));

        Assertions.assertEquals(UUID.fromString(PACKAGE_GUID), result.getGuid());
        factory.verify();
    }

    @Test
    void testGetPackagesForApplication() {
        stubGet(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID + "/packages?per_page=5000",
                "{\"resources\":[" + getPackageJson() + "]}");

        List<CloudPackage> result = client.getPackagesForApplication(UUID.fromString(APP_GUID));

        Assertions.assertEquals(1, result.size());
        factory.verify();
    }

    @Test
    void testGetUserRolesBySpaceAndUser() {
        stubGet(MockControllerClientFactory.BASE_URL + "/v3/roles?per_page=5000&space_guids=" + SPACE_GUID + "&user_guids=" + APP_GUID,
                "{\"resources\":[{\"guid\":\"" + BINDING_GUID + "\",\"type\":\"space_developer\"}]}");

        Set<UserRole> result = client.getUserRolesBySpaceAndUser(UUID.fromString(SPACE_GUID), UUID.fromString(APP_GUID));

        Assertions.assertFalse(result.isEmpty());
        factory.verify();
    }

    @Test
    void testGetAsyncJob() {
        stubGet(MockControllerClientFactory.BASE_URL + "/v3/jobs/job-123",
                "{\"guid\":\"job-123\",\"operation\":\"app.delete\",\"state\":\"COMPLETE\"}");

        Assertions.assertNotNull(client.getAsyncJob("job-123"));
        factory.verify();
    }

    private void stubTargetSpaceMetadata() {
        when(target.getMetadata())
            .thenReturn(targetMetadata);
        when(targetMetadata.getGuid())
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

    private void stubServiceInstanceLookupWithPlan() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(
                   MockControllerClientFactory.BASE_URL + "/v3/service_instances?per_page=5000&space_guids=" + SPACE_GUID
                       + "&names=my-service"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess("{\"resources\":[" + getServiceInstanceWithPlanJson() + "]}",
                                                                MediaType.APPLICATION_JSON));
    }

    private void stubTargetOrganization() {
        CloudOrganization organization = ImmutableCloudOrganization.builder()
                                                                   .name("my-org")
                                                                   .metadata(
                                                                       ImmutableCloudMetadata.builder()
                                                                                             .guid(
                                                                                                 UUID.fromString(
                                                                                                     SPACE_GUID))
                                                                                             .build())
                                                                   .build();
        when(target.getOrganization()).thenReturn(organization);
    }

    private void stubGet(String uri, String responseJson) {
        var response = responseJson == null ? MockRestResponseCreators.withSuccess()
            : MockRestResponseCreators.withSuccess(responseJson, MediaType.APPLICATION_JSON);

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(uri))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(response);
    }

    private void stubMethod(String uri, HttpMethod method) {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(uri))
               .andExpect(MockRestRequestMatchers.method(method))
               .andRespond(MockRestResponseCreators.withSuccess());
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

    private static String getServiceInstanceWithPlanJson() {
        return "{\"guid\":\"" + SERVICE_INSTANCE_GUID + "\",\"name\":\"my-service\",\"type\":\"managed\",\"relationships\":"
            + "{\"service_plan\":{\"data\":{\"guid\":\"" + PLAN_GUID + "\"}}}}";
    }

    private static String getBindingJson() {
        return "{\"guid\":\"" + BINDING_GUID + "\",\"name\":\"my-binding\",\"type\":\"app\",\"relationships\":"
            + "{\"app\":{\"data\":{\"guid\":\"" + APP_GUID + "\"}},\"service_instance\":{\"data\":{\"guid\":\""
            + SERVICE_INSTANCE_GUID + "\"}}}}";
    }

    private static String getServiceKeyJson() {
        return "{\"guid\":\"" + SERVICE_KEY_GUID + "\",\"name\":\"my-key\",\"type\":\"key\",\"relationships\":"
            + "{\"service_instance\":{\"data\":{\"guid\":\"" + SERVICE_INSTANCE_GUID + "\"}}}}";
    }

    private static String getBrokerListJson() {
        return "{\"resources\":[{\"guid\":\"" + BROKER_GUID + "\",\"name\":\"my-broker\",\"url\":\"https://broker.example.com\"}]}";
    }

    private static String getPackageJson() {
        return "{\"guid\":\"" + PACKAGE_GUID + "\",\"type\":\"bits\",\"state\":\"READY\"}";
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
