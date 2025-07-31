package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudRoute;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceKey;
import org.cloudfoundry.multiapps.controller.client.facade.domain.DockerInfo;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudServiceKey;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableDockerCredentials;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableDockerInfo;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableServiceCredentialBindingOperation;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableStaging;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ServiceCredentialBindingOperation;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Staging;
import org.cloudfoundry.multiapps.controller.client.facade.dto.ApplicationToCreateDto;
import org.cloudfoundry.multiapps.controller.client.facade.dto.ImmutableApplicationToCreateDto;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ServiceKeyToInject;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.cloudfoundry.multiapps.controller.process.steps.StepsTestUtil.prepareDisablingAutoscaler;
import static org.cloudfoundry.multiapps.controller.process.steps.StepsTestUtil.testIfEnabledOrDisabledAutoscaler;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateOrUpdateAppStepTest extends SyncFlowableStepTest<CreateOrUpdateAppStep> {

    private static final String APP_NAME = "test-application";
    private static final String SERVICE_NAME = "test-service";
    private static final String SERVICE_KEY_NAME = "test-service-key";
    private static final String SERVICE_KEY_ENV_NAME = "test-service-key-env";
    private static final Staging DEFAULT_STAGING = ImmutableStaging.builder()
                                                                   .build();
    private static final UUID APP_TO_PROCESS_GUID = UUID.randomUUID();

    static Stream<Arguments> testHandleApplicationAttributes() {
        return Stream.of(
            //@formatter:off
                         // (1) Everything is specified properly and should apply incremental instances update:
                         Arguments.of(ImmutableStaging.builder().command("command1").healthCheckType("none").addBuildpack("buildpackUrl").build(),
                                      128, 256, TestData.routeSet("example.com", "foo-bar.xyz"), Map.of("env-key", "env-value"), true),
                         // (2) Disk quota is 0 and should apply incremental instances update:
                         Arguments.of(DEFAULT_STAGING, 0, 256, Collections.emptySet(), Collections.emptyMap(), true),
                         // (3) Memory is 0 and should apply incremental instances update:
                         Arguments.of(DEFAULT_STAGING, 1024, 0, Collections.emptySet(), Collections.emptyMap(), true),
                // (4) Everything is specified properly and should not apply incremental instances update:
                Arguments.of(ImmutableStaging.builder().command("command1").healthCheckType("none").addBuildpack("buildpackUrl").build(),
                        128, 256, TestData.routeSet("example.com", "foo-bar.xyz"), Map.of("env-key", "env-value"), false),
                // (5) Disk quota is 0 and should not apply incremental instances update:
                Arguments.of(DEFAULT_STAGING, 0, 256, Collections.emptySet(), Collections.emptyMap(), false),
                // (6) Memory is 0 and should not apply incremental instances update:
                Arguments.of(DEFAULT_STAGING, 1024, 0, Collections.emptySet(), Collections.emptyMap(), false)
//@formatter:on             
        );
    }

    @ParameterizedTest
    @MethodSource
    void testHandleApplicationAttributes(Staging staging, int diskQuota, int memory, Set<CloudRoute> routes, Map<String, String> env,
                                         boolean shouldApplyIncrementalInstancesUpdate) {
        CloudApplicationExtended application = buildApplication(staging, diskQuota, memory, routes, env);
        context.setVariable(Variables.APP_TO_PROCESS, application);
        context.setVariable(Variables.SHOULD_APPLY_INCREMENTAL_INSTANCES_UPDATE, shouldApplyIncrementalInstancesUpdate);
        prepareDisablingAutoscaler(context, client, application, APP_TO_PROCESS_GUID);

        step.execute(execution);

        assertStepFinishedSuccessfully();
        Integer expectedDiskQuota = diskQuota == 0 ? null : diskQuota;
        Integer expectedMemory = memory == 0 ? null : memory;
        ApplicationToCreateDto applicationToCreateDto = ImmutableApplicationToCreateDto.builder()
                                                                                       .name(APP_NAME)
                                                                                       .staging(staging)
                                                                                       .diskQuotaInMb(expectedDiskQuota)
                                                                                       .memoryInMb(expectedMemory)
                                                                                       .routes(routes)
                                                                                       .env(env)
                                                                                       .build();
        verify(client).createApplication(applicationToCreateDto);

        if (shouldApplyIncrementalInstancesUpdate) {
            testIfEnabledOrDisabledAutoscaler(client, MessageFormat.format(Messages.DISABLE_AUTOSCALER_LABEL_CONTENT, ""),
                                              APP_TO_PROCESS_GUID);
        }

        assertTrue(context.getVariable(Variables.VCAP_APP_PROPERTIES_CHANGED));
    }

    private CloudApplicationExtended buildApplication(Staging staging, int diskQuota, int memory, Set<CloudRoute> routes,
                                                      Map<String, String> env) {
        return ImmutableCloudApplicationExtended.builder()
                                                .name(APP_NAME)
                                                .staging(staging)
                                                .diskQuota(diskQuota)
                                                .memory(memory)
                                                .routes(routes)
                                                .env(env)
                                                .build();
    }

    @Test
    void testCreateApplicationFromDockerImage() {
        DockerInfo dockerInfo = ImmutableDockerInfo.builder()
                                                   .image("cloudfoundry/test-app")
                                                   .credentials(ImmutableDockerCredentials.builder()
                                                                                          .username("someUser")
                                                                                          .password("somePassword")
                                                                                          .build())
                                                   .build();
        Staging dockerStaging = ImmutableStaging.builder()
                                                .dockerInfo(dockerInfo)
                                                .build();

        CloudApplicationExtended application = buildApplication(dockerStaging, 128, 256, Collections.emptySet(), Collections.emptyMap());
        application = ImmutableCloudApplicationExtended.copyOf(application)
                                                       .withDockerInfo(dockerInfo);
        context.setVariable(Variables.APP_TO_PROCESS, application);

        step.execute(execution);

        assertStepFinishedSuccessfully();
        ApplicationToCreateDto applicationToCreateDto = ImmutableApplicationToCreateDto.builder()
                                                                                       .name(APP_NAME)
                                                                                       .staging(dockerStaging)
                                                                                       .diskQuotaInMb(128)
                                                                                       .memoryInMb(256)
                                                                                       .routes(Collections.emptySet())
                                                                                       .env(Collections.emptyMap())
                                                                                       .build();
        verify(client).createApplication((applicationToCreateDto));
        verify(stepLogger).info(Messages.CREATING_APP_FROM_DOCKER_IMAGE, APP_NAME, dockerInfo.getImage());
    }

    @Test
    void testHandleApplicationServices() {
        CloudApplicationExtended application = buildApplication(null, 0, 0, Collections.emptySet(), Collections.emptyMap());
        List<String> services = List.of("service-1", "service-2");
        application = ImmutableCloudApplicationExtended.copyOf(application)
                                                       .withServices(services);
        context.setVariable(Variables.APP_TO_PROCESS, application);

        step.execute(execution);

        assertStepFinishedSuccessfully();
        assertTrue(services.containsAll(context.getVariable(Variables.SERVICES_TO_UNBIND_BIND)));
    }

    @Test
    void testInjectServiceKeysCredentialsInAppEnv() {
        CloudApplicationExtended application = buildApplication(null, 0, 0, Collections.emptySet(), Collections.emptyMap());
        Map<String, String> applicationEnv = Map.of("restart-policy", "always");
        ServiceKeyToInject serviceKey = new ServiceKeyToInject(SERVICE_KEY_ENV_NAME, SERVICE_NAME, SERVICE_KEY_NAME);
        application = ImmutableCloudApplicationExtended.copyOf(application)
                                                       .withEnv(applicationEnv)
                                                       .withServiceKeysToInject(serviceKey);
        Map<String, String> serviceKeyCredentials = Map.of("user", "service-key-user", "password", "service-key-password");
        when(client.getServiceKey(SERVICE_NAME, serviceKey.getServiceKeyName())).thenReturn(buildCloudServiceKey(serviceKeyCredentials));
        context.setVariable(Variables.APP_TO_PROCESS, application);

        step.shouldPrettyPrint = () -> false;
        step.execute(execution);

        assertStepFinishedSuccessfully();
        assertEquals(JsonUtil.toJson(serviceKeyCredentials), context.getVariable(Variables.APP_TO_PROCESS)
                                                                    .getEnv()
                                                                    .get(serviceKey.getEnvVarName()));
    }

    private CloudServiceKey buildCloudServiceKey(Map<String, String> serviceKeyCredentials) {
        return ImmutableCloudServiceKey.builder()
                                       .name(SERVICE_KEY_NAME)
                                       .credentials(serviceKeyCredentials)
                                       .serviceKeyOperation(ImmutableServiceCredentialBindingOperation.builder()
                                                                                                      .type(
                                                                                                          ServiceCredentialBindingOperation.Type.CREATE)
                                                                                                      .state(
                                                                                                          ServiceCredentialBindingOperation.State.SUCCEEDED)
                                                                                                      .build())
                                       .build();
    }

    @Test
    void testThrowExceptionWhenSpecifiedServiceKeyNotExist() {
        CloudApplicationExtended application = buildApplication(null, 0, 0, Collections.emptySet(), Collections.emptyMap());
        Map<String, String> applicationEnv = Map.of("restart-policy", "always");
        ServiceKeyToInject serviceKey = new ServiceKeyToInject(SERVICE_KEY_ENV_NAME, SERVICE_NAME, SERVICE_KEY_NAME);
        application = ImmutableCloudApplicationExtended.copyOf(application)
                                                       .withEnv(applicationEnv)
                                                       .withServiceKeysToInject(serviceKey);
        when(client.getServiceKeys(SERVICE_NAME)).thenReturn(Collections.emptyList());
        context.setVariable(Variables.APP_TO_PROCESS, application);

        assertThrows(SLException.class, () -> step.execute(execution));
    }

    @Override
    protected CreateOrUpdateAppStep createStep() {
        return new CreateOrUpdateAppStep();
    }

}
