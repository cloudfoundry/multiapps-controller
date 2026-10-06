package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudApplication;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudProcess;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.domain.DropletInfo;
import org.cloudfoundry.multiapps.controller.client.facade.domain.HealthCheckType;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudApplication;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudMetadata;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableStaging;
import org.cloudfoundry.multiapps.controller.client.facade.domain.InstanceState;
import org.cloudfoundry.multiapps.controller.client.facade.domain.InstancesInfo;
import org.cloudfoundry.multiapps.controller.client.facade.domain.LifecycleType;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Staging;
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

class ProcessesV3OperationsTest {

    private static final String APP_GUID = "3725a721-6e3f-4b6e-8f2b-1c2d3e4f5a6b";
    private static final String PROCESS_GUID = "11111111-2222-3333-4444-555555555555";
    private static final String PACKAGE_GUID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
    private static final String DROPLET_GUID = "99999999-8888-7777-6666-555555555555";

    @Mock
    private CloudSpace target;

    private MockControllerClientFactory factory;
    private ProcessesV3Operations operations;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        factory = MockControllerClientFactory.create();
        operations = new ProcessesV3Operations(factory.client(), target);
    }

    @Test
    void testGetApplicationProcessReturnsMappedProcess() {
        factory.stubGet(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID + "/processes/web", getProcessJson());

        CloudProcess result = operations.getApplicationProcess(UUID.fromString(APP_GUID));

        Assertions.assertEquals("start-cmd", result.getCommand());
        Assertions.assertEquals(2, result.getInstances());
        Assertions.assertEquals(256, result.getMemoryInMb());
        Assertions.assertEquals(1024, result.getDiskInMb());
        Assertions.assertEquals(HealthCheckType.HTTP, result.getHealthCheckType());
        Assertions.assertEquals("/health", result.getHealthCheckHttpEndpoint());
        factory.verify();
    }

    @Test
    void testGetApplicationProcessReturnsNullWhenNoBody() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID
                                                             + "/processes/web"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withStatus(HttpStatus.NO_CONTENT));

        CloudProcess result = operations.getApplicationProcess(UUID.fromString(APP_GUID));

        Assertions.assertNull(result);
        factory.verify();
    }

    @Test
    void testGetApplicationInstancesByGuidReturnsMappedInstances() {
        factory.stubGet(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID + "/processes/web/stats", getStatsJson());

        InstancesInfo result = operations.getApplicationInstances(UUID.fromString(APP_GUID));

        Assertions.assertEquals(2, result.getInstances()
                                         .size());
        Assertions.assertEquals(InstanceState.RUNNING, result.getInstances()
                                                             .get(0)
                                                             .getState());
        Assertions.assertEquals(InstanceState.CRASHED, result.getInstances()
                                                             .get(1)
                                                             .getState());
        factory.verify();
    }

    @Test
    void testGetApplicationInstancesForStartedApplicationQueriesStats() {
        factory.stubGet(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID + "/processes/web/stats", getStatsJson());

        CloudApplication application = ImmutableCloudApplication.builder()
                                                                .metadata(ImmutableCloudMetadata.builder()
                                                                                                .guid(UUID.fromString(APP_GUID))
                                                                                                .build())
                                                                .name("my-app")
                                                                .state(CloudApplication.State.STARTED)
                                                                .build();

        InstancesInfo result = operations.getApplicationInstances(application);

        Assertions.assertEquals(2, result.getInstances()
                                         .size());
        factory.verify();
    }

    @Test
    void testGetApplicationInstancesForStoppedApplicationReturnsEmptyWithoutHttp() {
        CloudApplication application = ImmutableCloudApplication.builder()
                                                                .metadata(ImmutableCloudMetadata.builder()
                                                                                                .guid(UUID.fromString(APP_GUID))
                                                                                                .build())
                                                                .name("my-app")
                                                                .state(CloudApplication.State.STOPPED)
                                                                .build();

        InstancesInfo result = operations.getApplicationInstances(application);

        Assertions.assertTrue(result.getInstances()
                                    .isEmpty());
        factory.verify();
    }

    @Test
    void testGetApplicationSshEnabledReturnsTrue() {
        factory.stubGet(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID + "/ssh_enabled", "{\"enabled\":true,\"reason\":\"\"}");

        Assertions.assertTrue(operations.getApplicationSshEnabled(UUID.fromString(APP_GUID)));
        factory.verify();
    }

    @Test
    void testGetApplicationSshEnabledReturnsFalseWhenEnabledIsNull() {
        factory.stubGet(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID + "/ssh_enabled", "{\"reason\":\"disabled globally\"}");

        Assertions.assertFalse(operations.getApplicationSshEnabled(UUID.fromString(APP_GUID)));
        factory.verify();
    }

    @Test
    void testGetApplicationFeaturesReturnsMap() {
        factory.stubGet(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID + "/features?per_page=5000",
                   "{\"resources\":[{\"name\":\"ssh\",\"enabled\":true},{\"name\":\"revisions\",\"enabled\":false}]}");

        Map<String, Boolean> result = operations.getApplicationFeatures(UUID.fromString(APP_GUID));

        Assertions.assertEquals(2, result.size());
        Assertions.assertTrue(result.get("ssh"));
        Assertions.assertFalse(result.get("revisions"));
        factory.verify();
    }

    @Test
    void testGetApplicationFeaturesReturnsEmptyMap() {
        factory.stubGet(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID + "/features?per_page=5000", "{\"resources\":[]}");

        Map<String, Boolean> result = operations.getApplicationFeatures(UUID.fromString(APP_GUID));

        Assertions.assertTrue(result.isEmpty());
        factory.verify();
    }

    @Test
    void testGetCurrentDropletForApplicationReturnsDropletInfo() {
        factory.stubGet(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID + "/droplets/current",
                   getDropletJson(MockControllerClientFactory.BASE_URL + "/v3/packages/" + PACKAGE_GUID));

        DropletInfo result = operations.getCurrentDropletForApplication(UUID.fromString(APP_GUID));

        Assertions.assertEquals(UUID.fromString(DROPLET_GUID), result.getGuid());
        Assertions.assertEquals(UUID.fromString(PACKAGE_GUID), result.getPackageGuid());
        factory.verify();
    }

    @Test
    void testGetCurrentDropletForApplicationHandlesTrailingSlashInPackageLink() {
        factory.stubGet(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID + "/droplets/current",
                   getDropletJson(MockControllerClientFactory.BASE_URL + "/v3/packages/" + PACKAGE_GUID + "/"));

        DropletInfo result = operations.getCurrentDropletForApplication(UUID.fromString(APP_GUID));

        Assertions.assertEquals(UUID.fromString(PACKAGE_GUID), result.getPackageGuid());
        factory.verify();
    }

    @Test
    void testGetCurrentDropletForApplicationThrowsWhenNoDroplet() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID
                                                             + "/droplets/current"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withStatus(HttpStatus.NO_CONTENT));

        UUID appGuid = UUID.fromString(APP_GUID);
        Assertions.assertThrows(CloudOperationException.class,
                                () -> operations.getCurrentDropletForApplication(appGuid));
    }

    @Test
    void testUpdateApplicationStagingUpdatesAppAndProcess() {
        when(target.getGuid())
            .thenReturn(null);

        factory.stubGet(MockControllerClientFactory.BASE_URL + "/v3/apps?per_page=5000&names=my-app", "{\"resources\":[{\"guid\":\"" + APP_GUID + "\"}]}");

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.PATCH))
               .andRespond(MockRestResponseCreators.withSuccess("{}", MediaType.APPLICATION_JSON));

        factory.stubGet(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID + "/processes/web", getProcessJson());

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/processes/" + PROCESS_GUID))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.PATCH))
               .andRespond(MockRestResponseCreators.withSuccess("{}", MediaType.APPLICATION_JSON));

        Staging staging = ImmutableStaging.builder()
                                          .command("start-cmd")
                                          .stackName("cflinuxfs4")
                                          .addBuildpacks("java_buildpack")
                                          .healthCheckType("http")
                                          .healthCheckHttpEndpoint("/health")
                                          .healthCheckTimeout(60)
                                          .readinessHealthCheckType("http")
                                          .readinessHealthCheckHttpEndpoint("/ready")
                                          .build();

        operations.updateApplicationStaging("my-app", staging);

        factory.verify();
    }

    @Test
    void testUpdateApplicationStagingUpdatesAppFeatures() {
        when(target.getGuid())
            .thenReturn(null);

        factory.stubGet(MockControllerClientFactory.BASE_URL + "/v3/apps?per_page=5000&names=my-app", "{\"resources\":[{\"guid\":\"" + APP_GUID + "\"}]}");
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.PATCH))
               .andRespond(MockRestResponseCreators.withSuccess("{}", MediaType.APPLICATION_JSON));

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID
                                                             + "/features/ssh"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.PATCH))
               .andRespond(MockRestResponseCreators.withSuccess("{}", MediaType.APPLICATION_JSON));

        factory.stubGet(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID + "/processes/web", getProcessJson());

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/processes/" + PROCESS_GUID))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.PATCH))
               .andRespond(MockRestResponseCreators.withSuccess("{}", MediaType.APPLICATION_JSON));

        Staging staging = ImmutableStaging.builder()
                                          .command("start-cmd")
                                          .appFeatures(Map.of("ssh", true))
                                          .build();

        operations.updateApplicationStaging("my-app", staging);

        factory.verify();
    }

    @Test
    void testUpdateApplicationStagingIncludesSpaceGuidInLookupQuery() {
        UUID spaceGuid = UUID.randomUUID();
        when(target.getGuid())
            .thenReturn(spaceGuid);

        factory.stubGet(MockControllerClientFactory.BASE_URL + "/v3/apps?per_page=5000&space_guids="
                                                             + spaceGuid + "&names=my-app", "{\"resources\":[{\"guid\":\"" + APP_GUID + "\"}]}");

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.PATCH))
               .andRespond(MockRestResponseCreators.withSuccess("{}", MediaType.APPLICATION_JSON));

        factory.stubGet(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID + "/processes/web", getProcessJson());

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/processes/" + PROCESS_GUID))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.PATCH))
               .andRespond(MockRestResponseCreators.withSuccess("{}", MediaType.APPLICATION_JSON));

        Staging staging = ImmutableStaging.builder()
                                          .command("start-cmd")
                                          .build();

        operations.updateApplicationStaging("my-app", staging);

        factory.verify();
    }

    @Test
    void testUpdateApplicationStagingThrowsWhenApplicationNotFound() {
        when(target.getGuid())
            .thenReturn(null);

        factory.stubGet(MockControllerClientFactory.BASE_URL
                                                             + "/v3/apps?per_page=5000&names=missing-app", "{\"resources\":[]}");

        Staging staging = ImmutableStaging.builder()
                                          .command("cmd")
                                          .build();

        Assertions.assertThrows(CloudOperationException.class, () -> operations.updateApplicationStaging("missing-app", staging));
    }

    @Test
    void testUpdateApplicationStagingWithCnbLifecycleWithoutBuildpacksThrows() {
        when(target.getGuid())
            .thenReturn(null);

        factory.stubGet(MockControllerClientFactory.BASE_URL + "/v3/apps?per_page=5000&names=my-app", "{\"resources\":[{\"guid\":\"" + APP_GUID + "\"}]}");

        Staging staging = ImmutableStaging.builder()
                                          .command("cmd")
                                          .lifecycleType(LifecycleType.CNB)
                                          .build();

        Assertions.assertThrows(IllegalArgumentException.class, () -> operations.updateApplicationStaging("my-app", staging));
    }

    private static String getProcessJson() {
        return "{\"guid\":\"" + PROCESS_GUID + "\",\"command\":\"start-cmd\",\"instances\":2,\"memory_in_mb\":256,\"disk_in_mb\":1024,"
            + "\"health_check\":{\"type\":\"http\",\"data\":{\"timeout\":60,\"invocation_timeout\":5,\"endpoint\":\"/health\","
            + "\"interval\":10}}}";
    }

    private static String getStatsJson() {
        return "{\"resources\":[{\"index\":0,\"state\":\"RUNNING\",\"routable\":\"true\"},"
            + "{\"index\":1,\"state\":\"CRASHED\",\"routable\":\"false\"}]}";
    }

    private static String getDropletJson(String packageHref) {
        return "{\"guid\":\"" + DROPLET_GUID + "\",\"links\":{\"package\":{\"href\":\"" + packageHref + "\"}}}";
    }

}
