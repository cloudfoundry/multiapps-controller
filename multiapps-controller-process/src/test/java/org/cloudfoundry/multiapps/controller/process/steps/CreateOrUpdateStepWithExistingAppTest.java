package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.Constants;
import org.cloudfoundry.multiapps.controller.core.cf.clients.AppBoundServiceInstanceNamesGetter;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationURI;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.CloudRoute;
import com.sap.cloudfoundry.client.facade.domain.HealthCheckType;
import com.sap.cloudfoundry.client.facade.domain.Staging;
import com.sap.cloudfoundry.client.facade.domain.LifecycleType;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudApplication;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudPackage;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudProcess;
import com.sap.cloudfoundry.client.facade.domain.ImmutableDockerData;
import com.sap.cloudfoundry.client.facade.domain.ImmutableDockerInfo;
import com.sap.cloudfoundry.client.facade.domain.ImmutableDropletInfo;
import com.sap.cloudfoundry.client.facade.domain.ImmutableStaging;
import com.sap.cloudfoundry.client.facade.domain.ImmutableLifecycle;
import com.sap.cloudfoundry.client.facade.util.JsonUtil;

class CreateOrUpdateStepWithExistingAppTest extends SyncFlowableStepTest<CreateOrUpdateAppStep> {

    private static final String APP_DIGEST = "12345";
    private static final String APP_NAME_ENV_KEY = "APP_NAME";
    private static final String APP_NAME = "test-application";
    private static final String DEFAULT_STACK = "cflinuxfs3";
    private static final String DEFAULT_COMMAND = "/bin/bash";
    private static final Staging DEFAULT_STAGING = ImmutableStaging.builder()
                                                                   .build();

    @Mock
    private AppBoundServiceInstanceNamesGetter appServicesGetter;

    static Stream<Arguments> testHandleStagingApplicationAttributes() {
        return Stream.of(
//@formatter:off
				Arguments.of(ImmutableStaging.builder().addBuildpack("buildpack-1").command("command1").build(),
						ImmutableStaging.builder().addBuildpack("buildpack-1").command("command2").build(), true),
				Arguments.of(ImmutableStaging.builder().addBuildpack("buildpack-1").build(),
						ImmutableStaging.builder().addBuildpack("buildpack-1").build(), false),
				Arguments.of(
						ImmutableStaging.builder().addBuildpack("buildpack-1").command("command1").stackName("stack1")
								.healthCheckTimeout(5).healthCheckType("process").isSshEnabled(false).build(),
						ImmutableStaging.builder().addBuildpack("buildpack-2").command("command2").stackName("stack2")
								.healthCheckTimeout(10).healthCheckType("port").healthCheckHttpEndpoint("/test")
								.isSshEnabled(true).build(),
						true),
				Arguments.of(
						ImmutableStaging.builder().addBuildpack("buildpack-2").command("command2").stackName("stack2")
								.healthCheckTimeout(10).healthCheckType("process").healthCheckHttpEndpoint("/test")
								.isSshEnabled(true).build(),
						ImmutableStaging.builder().addBuildpack("buildpack-2").command("command2").stackName("stack2")
								.healthCheckTimeout(10).healthCheckType("process").healthCheckHttpEndpoint("/test")
								.isSshEnabled(true).build(),
						false),
				Arguments.of(ImmutableStaging.builder()
						.dockerInfo(ImmutableDockerInfo.builder().image("cloudfoundry/test-app").build()).build(),
						ImmutableStaging.builder()
								.dockerInfo(ImmutableDockerInfo.builder().image("cloudfoundry/test-app2").build())
								.build(),
						true),
				Arguments.of(ImmutableStaging.builder()
						.dockerInfo(ImmutableDockerInfo.builder().image("cloudfoundry/test-app").build()).build(),
						ImmutableStaging.builder()
								.dockerInfo(ImmutableDockerInfo.builder().image("cloudfoundry/test-app").build())
								.build(),
						false));
//@formatter:on
    }

    @ParameterizedTest
    @MethodSource
    void testHandleStagingApplicationAttributes(Staging existingStaging, Staging staging, boolean expectedPropertiesChanged) {
        CloudApplication existingApplication = getApplicationBuilder(false).staging(existingStaging)
                                                                           .build();
        if (staging.getCommand() == null) {
            staging = ImmutableStaging.copyOf(staging)
                                      .withCommand(DEFAULT_COMMAND);
        }
        if (staging.getStackName() == null) {
            staging = ImmutableStaging.copyOf(staging)
                                      .withStackName(DEFAULT_STACK);
        }
        CloudApplicationExtended application = getApplicationBuilder(false).staging(staging)
                                                                           .build();
        if (staging.getDockerInfo() != null) {
            application = ImmutableCloudApplicationExtended.copyOf(application)
                                                           .withLifecycle(ImmutableLifecycle.builder()
                                                                                            .type(LifecycleType.DOCKER)
                                                                                            .build());
        }
        prepareContext(application, false);
        prepareClientWithStaging(existingApplication, existingStaging);

        step.execute(execution);

        assertStepFinishedSuccessfully();
        assertEquals(expectedPropertiesChanged, context.getVariable(Variables.VCAP_APP_PROPERTIES_CHANGED));
        if (expectedPropertiesChanged) {
            verify(client).updateApplicationStaging(APP_NAME, staging);
            return;
        }
        verify(client, never()).updateApplicationStaging(eq(APP_NAME), any());
    }

    private ImmutableCloudApplicationExtended.Builder getApplicationBuilder(boolean shouldKeepExistingEnv) {
        return ImmutableCloudApplicationExtended.builder()
                                                .metadata(ImmutableCloudMetadata.builder()
                                                                                .guid(UUID.randomUUID())
                                                                                .build())
                                                .state(CloudApplication.State.STARTED)
                                                .lifecycle(ImmutableLifecycle.builder()
                                                                             .type(LifecycleType.BUILDPACK)
                                                                             .data(Map.of("buildpacks", Collections.<String> emptyList(),
                                                                                          "stack", DEFAULT_STACK))
                                                                             .build())
                                                .name(APP_NAME)
                                                .instances(1)
                                                .staging(ImmutableStaging.builder()
                                                                         .command(DEFAULT_COMMAND)
                                                                         .stackName(DEFAULT_STACK)
                                                                         .healthCheckType("port")
                                                                         .isSshEnabled(false)
                                                                         .build())
                                                .attributesUpdateStrategy(ImmutableCloudApplicationExtended.AttributeUpdateStrategy.builder()
                                                                                                                                   .shouldKeepExistingEnv(shouldKeepExistingEnv)
                                                                                                                                   .build());
    }

    private void prepareContext(CloudApplicationExtended application, boolean shouldSkipServiceRebinding) {
        context.setVariable(Variables.APP_TO_PROCESS, application);
        context.setVariable(Variables.SERVICE_KEYS_CREDENTIALS_TO_INJECT, Collections.emptyMap());
        context.setVariable(Variables.SHOULD_SKIP_SERVICE_REBINDING, shouldSkipServiceRebinding);
    }

    private void prepareClientWithServices(CloudApplication application, List<String> services) {
        prepareClient(application, Set.of(), services, Map.of(), DEFAULT_STAGING, null, null);
    }

    private void prepareClientWithEnv(CloudApplication application, Map<String, String> env) {
        prepareClient(application, Set.of(), List.of(), env, DEFAULT_STAGING, null, null);
    }

    private void prepareClientWithMemory(CloudApplication application, Integer memory) {
        prepareClient(application, Set.of(), List.of(), Map.of(), DEFAULT_STAGING, memory, null);
    }

    private void prepareClientWithDisk(CloudApplication application, Integer disk) {
        prepareClient(application, Set.of(), List.of(), Map.of(), DEFAULT_STAGING, null, disk);
    }

    private void prepareClientWithStaging(CloudApplication application, Staging staging) {
        prepareClient(application, Set.of(), List.of(), Map.of(), staging, null, null);
    }

    private void prepareClientWithRoutes(CloudApplication application, Set<CloudRoute> routes) {
        prepareClient(application, routes, List.of(), Map.of(), DEFAULT_STAGING, null, null);
    }

    private void prepareClient(CloudApplication application, Set<CloudRoute> routes, List<String> services, Map<String, String> env,
                               Staging staging, Integer memory, Integer disk) {
        when(appServicesGetter.getServiceInstanceNamesBoundToApp(any())).thenReturn(services);
        when(client.getApplicationEnvironment(application.getGuid())).thenReturn(env);
        application = prepareAppWithStaging(application, staging);
        when(client.getApplication(APP_NAME, false)).thenReturn(application);
        when(client.getApplicationSshEnabled(application.getGuid())).thenReturn(staging.isSshEnabled() != null && staging.isSshEnabled());
        when(client.getApplicationRoutes(application.getGuid())).thenReturn(List.copyOf(routes));
        String command = staging.getCommand() == null ? DEFAULT_COMMAND : staging.getCommand();
        var hcType = staging.getHealthCheckType();
        var hcTimeout = staging.getHealthCheckTimeout();
        var hcEndpoint = staging.getHealthCheckHttpEndpoint();
        when(client.getApplicationProcess(application.getGuid())).thenReturn(ImmutableCloudProcess.builder()
                                                                                                  .command(command)
                                                                                                  .diskInMb(disk == null ? 1024 : disk)
                                                                                                  .memoryInMb(memory == null ? 1024 : memory)
                                                                                                  .healthCheckType(hcType == null ? HealthCheckType.PORT : HealthCheckType.valueOf(hcType.toUpperCase()))
                                                                                                  .healthCheckTimeout(hcTimeout)
                                                                                                  .healthCheckHttpEndpoint(hcEndpoint)
                                                                                                  .instances(1)
                                                                                                  .build());
    }

    private CloudApplication prepareAppWithStaging(CloudApplication application, Staging staging) {
        if (staging.getBuildpacks() != null && !staging.getBuildpacks()
                                                       .isEmpty()) {
            var currentLifecycle = application.getLifecycle();
            var newData = new HashMap<>(currentLifecycle.getData());
            newData.put("buildpacks", staging.getBuildpacks());
            application = ImmutableCloudApplication.copyOf(application)
                                                   .withLifecycle(ImmutableLifecycle.builder()
                                                                                    .type(LifecycleType.BUILDPACK)
                                                                                    .data(newData)
                                                                                    .build());
        }
        if (staging.getStackName() != null && !staging.getStackName()
                                                      .isBlank()) {
            var currentLifecycle = application.getLifecycle();
            var newData = new HashMap<>(currentLifecycle.getData());
            newData.put("stack", staging.getStackName());
            application = ImmutableCloudApplication.copyOf(application)
                                                   .withLifecycle(ImmutableLifecycle.builder()
                                                                                    .type(LifecycleType.BUILDPACK)
                                                                                    .data(newData)
                                                                                    .build());
        }
        if (staging.getDockerInfo() != null) {
            application = ImmutableCloudApplication.copyOf(application)
                                                   .withLifecycle(ImmutableLifecycle.builder()
                                                                                    .type(LifecycleType.DOCKER)
                                                                                    .build());
            var packageGuid = UUID.randomUUID();
            var dropletInfo = ImmutableDropletInfo.builder()
                                                  .packageGuid(packageGuid)
                                                  .build();
            var cloudPackage = ImmutableCloudPackage.builder()
                                                    .data(ImmutableDockerData.builder()
                                                                             .image(staging.getDockerInfo()
                                                                                           .getImage())
                                                                             .build())
                                                    .build();
            when(client.getCurrentDropletForApplication(eq(application.getGuid()))).thenReturn(dropletInfo);
            when(client.getPackage(eq(packageGuid))).thenReturn(cloudPackage);
        }
        return application;
    }

    static Stream<Arguments> testHandleMemoryApplicationAttributes() {
        return Stream.of(Arguments.of(128, 256, true), Arguments.of(512, 128, true), Arguments.of(1024, 0, false),
                         Arguments.of(1024, 1024, false));
    }

    @ParameterizedTest
    @MethodSource
    void testHandleMemoryApplicationAttributes(int existingMemorySize, int memorySize, boolean expectedPropertiesChanged) {
        CloudApplication existingApplication = getApplicationBuilder(false).memory(existingMemorySize)
                                                                           .build();
        CloudApplicationExtended application = getApplicationBuilder(false).memory(memorySize)
                                                                           .build();
        prepareContext(application, false);
        prepareClientWithMemory(existingApplication, existingMemorySize);

        step.execute(execution);

        assertStepFinishedSuccessfully();
        assertEquals(expectedPropertiesChanged, context.getVariable(Variables.VCAP_APP_PROPERTIES_CHANGED));
        if (expectedPropertiesChanged) {
            verify(client).updateApplicationMemory(APP_NAME, memorySize);
            return;
        }
        verify(client, never()).updateApplicationMemory(eq(APP_NAME), anyInt());
    }

    static Stream<Arguments> testHandleDiskQuotaApplicationAttributes() {
        return Stream.of(Arguments.of(128, 256, true), Arguments.of(512, 128, true), Arguments.of(1024, 0, false),
                         Arguments.of(1024, 1024, false));
    }

    @ParameterizedTest
    @MethodSource
    void testHandleDiskQuotaApplicationAttributes(int existingDiskQuotaSize, int diskQuotaSize, boolean expectedPropertiesChanged) {
        CloudApplication existingApplication = getApplicationBuilder(false).diskQuota(existingDiskQuotaSize)
                                                                           .build();
        CloudApplicationExtended application = getApplicationBuilder(false).diskQuota(diskQuotaSize)
                                                                           .build();
        prepareContext(application, false);
        prepareClientWithDisk(existingApplication, existingDiskQuotaSize);

        step.execute(execution);

        assertStepFinishedSuccessfully();
        assertEquals(expectedPropertiesChanged, context.getVariable(Variables.VCAP_APP_PROPERTIES_CHANGED));
        if (expectedPropertiesChanged) {
            verify(client).updateApplicationDiskQuota(APP_NAME, diskQuotaSize);
            return;
        }
        verify(client, never()).updateApplicationDiskQuota(eq(APP_NAME), anyInt());
    }

    static Stream<Arguments> testHandleRoutesApplicationAttributes() {
        return Stream.of(Arguments.of(Collections.emptySet(), constructRoutes("example.com"), true),
                         Arguments.of(constructRoutes("example.com"), Collections.emptySet(), true),
                         Arguments.of(constructRoutes("example.com"), constructRoutes("example.com", "example1.com"), true),
                         Arguments.of(constructRoutes("example.com"), constructRoutes("example.com"), false));
    }

    @ParameterizedTest
    @MethodSource
    void testHandleRoutesApplicationAttributes(Set<CloudRoute> existingRoutes, Set<CloudRoute> routes,
                                               boolean expectedPropertiesChanged) {
        CloudApplication existingApplication = getApplicationBuilder(false).routes(existingRoutes)
                                                                           .build();
        CloudApplicationExtended application = getApplicationBuilder(false).routes(routes)
                                                                           .build();
        prepareContext(application, false);
        prepareClientWithRoutes(existingApplication, existingRoutes);

        step.execute(execution);

        assertStepFinishedSuccessfully();
        assertEquals(expectedPropertiesChanged, context.getVariable(Variables.VCAP_APP_PROPERTIES_CHANGED));
        if (expectedPropertiesChanged) {
            verify(client).updateApplicationRoutes(APP_NAME, routes);
            return;
        }
        verify(client, never()).updateApplicationRoutes(eq(APP_NAME), anySet());
    }

    private static Set<CloudRoute> constructRoutes(String... uriStrings) {
        return Stream.of(uriStrings)
                     .map(uri -> new ApplicationURI(uri, false).toCloudRoute())
                     .collect(Collectors.toSet());
    }

    static Stream<Arguments> testHandleApplicationServices() {
        return Stream.of(Arguments.of(List.of("service-1"), List.of("service-1", "service-2"), false, List.of("service-1", "service-2")),
                         Arguments.of(Collections.emptyList(), List.of("service-1"), false, List.of("service-1")),
                         Arguments.of(List.of("service-1", "service-2"), Collections.emptyList(), true, null),
                         Arguments.of(List.of("service-1"), List.of("service-2"), false, List.of("service-1", "service-2")),
                         Arguments.of(List.of("service-1"), Collections.emptyList(), false, List.of("service-1")),
                         Arguments.of(Collections.emptyList(), List.of("service-1", "service-2"), true, null));
    }

    @ParameterizedTest
    @MethodSource
    void testHandleApplicationServices(List<String> existingServices, List<String> services, boolean shouldSkipServiceRebinding,
                                       List<String> expectedServicesToUpdate) {
        CloudApplication existingApplication = getApplicationBuilder(false).services(existingServices)
                                                                           .build();
        CloudApplicationExtended application = getApplicationBuilder(false).services(services)
                                                                           .build();
        prepareContext(application, shouldSkipServiceRebinding);
        prepareClientWithServices(existingApplication, existingServices);

        step.execute(execution);

        assertStepFinishedSuccessfully();
        if (shouldSkipServiceRebinding) {
            assertTrue(context.getVariable(Variables.SERVICES_TO_UNBIND_BIND)
                              .isEmpty());
            return;
        }
        assertTrue(expectedServicesToUpdate.containsAll(context.getVariable(Variables.SERVICES_TO_UNBIND_BIND)));
    }

    static Stream<Arguments> testHandleApplicationEnv() {
        return Stream.of(Arguments.of(Map.of("foo", "bar"), Map.of("foo1", "bar2"), true, true),
                         Arguments.of(Map.of("foo", "bar"), Map.of("foo1", "bar2"), false, true),
                         Arguments.of(Map.of("foo", "bar"), Map.of("foo", "bar"), true, false),
                         Arguments.of(Map.of("foo", "bar"), Map.of("foo", "bar"), false, false));
    }

    @ParameterizedTest
    @MethodSource
    void testHandleApplicationEnv(Map<String, String> existingAppEnv, Map<String, String> newAppEnv, boolean keepExistingEnv,
                                  boolean expectedUserPropertiesChanged) {
        CloudApplication existingApplication = getApplicationBuilder(keepExistingEnv).env(existingAppEnv)
                                                                                     .build();
        CloudApplicationExtended application = getApplicationBuilder(keepExistingEnv).env(newAppEnv)
                                                                                     .build();
        prepareContext(application, false);
        prepareClientWithEnv(existingApplication, existingAppEnv);

        step.shouldPrettyPrint = () -> false;
        step.execute(execution);

        assertStepFinishedSuccessfully();
        assertEquals(expectedUserPropertiesChanged, context.getVariable(Variables.USER_PROPERTIES_CHANGED));
        if (expectedUserPropertiesChanged) {
            Map<String, String> expectedEnvMap = buildExpectedEnvMap(existingAppEnv, newAppEnv, keepExistingEnv);
            verify(client).updateApplicationEnv(eq(APP_NAME), eq(expectedEnvMap));
            return;
        }
        verify(client, never()).updateApplicationEnv(eq(APP_NAME), anyMap());
    }

    private Map<String, String> buildExpectedEnvMap(Map<String, String> existingAppEnv, Map<String, String> newAppEnv,
                                                    boolean keepExistingEnv) {
        if (!keepExistingEnv) {
            return newAppEnv;
        }
        Map<String, String> expectedEnvMap = new HashMap<>(existingAppEnv);
        expectedEnvMap.putAll(newAppEnv);
        return expectedEnvMap;
    }

    @Test
    void testAddExistingAppDigestToNewEnv() {
        when(appServicesGetter.getServiceInstanceNamesBoundToApp(any())).thenReturn(Collections.emptyList());
        String applicationDigestJsonEnv = JsonUtil.convertToJson(Map.of(Constants.ATTR_APP_CONTENT_DIGEST, APP_DIGEST));
        Map<String, String> existingEnv = Map.of(Constants.ENV_DEPLOY_ATTRIBUTES, applicationDigestJsonEnv);
        CloudApplication existingApplication = getApplicationBuilder(false).env(existingEnv)
                                                                           .build();
        Map<String, String> newApplicationEnv = Map.of(APP_NAME_ENV_KEY, APP_NAME);
        CloudApplicationExtended application = getApplicationBuilder(false).env(newApplicationEnv)
                                                                           .build();
        prepareContext(application, false);
        prepareClientWithEnv(existingApplication, existingEnv);

        step.shouldPrettyPrint = () -> false;
        step.execute(execution);

        assertStepFinishedSuccessfully();
        assertEquals(true, context.getVariable(Variables.USER_PROPERTIES_CHANGED));

        Map<String, String> expectedEnv = buildExpectedEnvWithDeployAttributes(newApplicationEnv, applicationDigestJsonEnv);
        verify(client).updateApplicationEnv(eq(APP_NAME), eq(expectedEnv));
    }

    private Map<String, String> buildExpectedEnvWithDeployAttributes(Map<String, String> newApplicationEnv,
                                                                     String applicationDigestJsonEnv) {
        Map<String, String> expectedEnvMap = new HashMap<>(newApplicationEnv);
        expectedEnvMap.put(Constants.ENV_DEPLOY_ATTRIBUTES, applicationDigestJsonEnv);
        return expectedEnvMap;
    }

    @Override
    protected CreateOrUpdateAppStep createStep() {
        return new CreateOrUpdateAppStep() {
            @Override
            protected AppBoundServiceInstanceNamesGetter getAppBoundServiceInstanceNamesGetter(CloudControllerClient client) {
                return appServicesGetter;
            }
        };
    }

}
