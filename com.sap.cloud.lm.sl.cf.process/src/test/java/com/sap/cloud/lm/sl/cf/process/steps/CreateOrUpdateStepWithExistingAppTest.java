package com.sap.cloud.lm.sl.cf.process.steps;

import static org.mockito.ArgumentMatchers.eq;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.ListUtils;
import org.cloudfoundry.client.lib.ApplicationServicesUpdateCallback;
import org.cloudfoundry.client.lib.domain.*;
import org.cloudfoundry.client.lib.domain.CloudApplication.State;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ImmutableCloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ImmutableCloudServiceExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ServiceKeyToInject;
import com.sap.cloud.lm.sl.cf.core.util.NameUtil;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.util.GenericArgumentMatcher;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

public class CreateOrUpdateStepWithExistingAppTest extends SyncFlowableStepTest<CreateOrUpdateAppStep> {

    private static final ApplicationServicesUpdateCallback CALLBACK = ApplicationServicesUpdateCallback.DEFAULT_APPLICATION_SERVICES_UPDATE_CALLBACK;

    private StepInput input;

    private List<String> notRequiredServices;
    private List<String> expectedServicesToBind;

// @formatter:off
    private static Stream<Arguments> testExecute() {
        return Stream.of(
            Arguments.of("update-app-step-input-1.json", null),
            Arguments.of("update-app-step-input-2.json", null),
            Arguments.of("update-app-step-input-3.json", null),
            Arguments.of("update-app-step-input-4.json", null),
            Arguments.of("update-app-step-input-5.json", null),
            Arguments.of("update-app-step-input-6.json", null),
            Arguments.of("update-app-step-input-7.json", null),
            Arguments.of("update-app-step-input-8.json", null),
            Arguments.of("update-app-step-input-9.json", null),
            Arguments.of("update-app-step-input-10.json", null),
            Arguments.of("update-app-step-input-11.json", null),
            // Existing app has binding with null parameters and defined service binding is without parameters
            Arguments.of("update-app-step-input-12.json", null),
            // Existing app has binding with empty parameters and defined service binding is without parameters
            Arguments.of("update-app-step-input-13.json", null),
             // Existing app has binding with parameters and defined service binding is without parameters
            Arguments.of("update-app-step-input-14.json", null),
            // Existing app has binding with null parameters and defined service binding is with defined parameters
            Arguments.of("update-app-step-input-15.json", null),
            // Service keys to inject are specified
            Arguments.of("update-app-step-input-16.json", null),
            // Service keys to inject are specified but does not exist
            Arguments.of("update-app-step-input-17.json", "Unable to retrieve required service key element \"expected-service-key\" for service \"existing-service-1\""),
            // Test enable-ssh parameter
            Arguments.of("update-app-step-input-18.json", null),
            // Test if healthCheckType parameter is updated
            Arguments.of("update-app-step-input-19.json", null),
            // Test if healthCheckHttpEndpoint parameter is updated
            Arguments.of("update-app-step-input-20.json", null),
            // Test if healthCheckHttpEndpoint parameter is updated
            Arguments.of("update-app-step-input-21.json", null)
        );
    }
// @formatter:on

    @BeforeEach
    public void setUp() {
        notRequiredServices = new ArrayList<>();
        expectedServicesToBind = new ArrayList<>();
    }

    @Test
    public void testSkipRebindOfServices() {
        this.input = JsonUtil.fromJson(TestUtil.getResourceAsString("update-app-step-input-1.json", getClass()), StepInput.class);
        prepareContext();
        context.setVariable(Constants.VAR_SHOULD_SKIP_SERVICE_REBINDING, true);
        prepareClient();

        Assertions.assertDoesNotThrow(() -> step.execute(context));

        assertStepFinishedSuccessfully();
        validateUpdateComponents();

        Mockito.verify(client, Mockito.never())
               .bindService(Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any());
        Mockito.verify(client, Mockito.never())
               .unbindService(Mockito.anyString(), Mockito.anyString());
    }

    @ParameterizedTest
    @MethodSource
    public void testExecute(String input, String expectedExceptionMessage) {
        this.input = JsonUtil.fromJson(TestUtil.getResourceAsString(input, getClass()), StepInput.class);
        prepareContext();
        prepareClient();

        if (expectedExceptionMessage != null) {
            Assertions.assertThrows(Exception.class, () -> step.execute(context), expectedExceptionMessage);
            return;
        }

        Assertions.assertDoesNotThrow(() -> step.execute(context));

        assertStepFinishedSuccessfully();

        validateUnbindServices();
        validateBindServices();
        validateUpdateComponents();
    }

    private void validateUpdateComponents() {
        String appName = input.application.name;
        CloudApplicationExtended cloudApp = input.application.toCloudApp();
        if (input.updateStaging) {
            Mockito.verify(client)
                   .updateApplicationStaging(Mockito.eq(appName), Mockito.argThat(GenericArgumentMatcher.forObject(cloudApp.getStaging())));
        }
        if (input.updateMemory) {
            Mockito.verify(client)
                   .updateApplicationMemory(appName, cloudApp.getMemory());
        }
        if (input.updateDiskQuota) {
            Mockito.verify(client)
                   .updateApplicationDiskQuota(appName, cloudApp.getDiskQuota());
        }
        if (input.updateUris) {
            Mockito.verify(client)
                   .updateApplicationUris(appName, cloudApp.getUris());
        }
        if (input.updateEnv) {
            Mockito.verify(client)
                   .updateApplicationEnv(appName, cloudApp.getEnv());
        }
    }

    private void validateBindServices() {
        Map<String, Map<String, Object>> currentBindingParameters = input.application.toCloudApp()
                                                                                     .getBindingParameters();
        for (String serviceToBind : expectedServicesToBind) {
            Mockito.verify(client).bindService(input.application.toCloudApp().getName(), serviceToBind,
                                               getBindingParametersForService(currentBindingParameters, serviceToBind),
                                               step.getApplicationServicesUpdateCallback(context));
        }
    }

    private Map<String, Object> getBindingParametersForService(Map<String, Map<String, Object>> bindingParameters, String serviceName) {
        return bindingParameters == null ? Collections.emptyMap() : bindingParameters.getOrDefault(serviceName, Collections.emptyMap());
    }

    private void validateUnbindServices() {
        for (String notRequiredService : notRequiredServices) {
            Mockito.verify(client)
                   .unbindService(input.existingApplication.name, notRequiredService);
        }
    }

    private void prepareClient() {
        prepareDiscontinuedServices();

        expectedServicesToBind = prepareServicesToBind();

        prepareExistingServiceBindings();
        for (String service : input.application.services) {
            mockServiceRetrieval(service);
        }

        for (String service : input.existingApplication.services) {
            mockServiceRetrieval(service);
        }

        for (String service : expectedServicesToBind) {
            mockServiceRetrieval(service);
        }

    }

    private void mockServiceRetrieval(String service) {
        Mockito.when(client.getService(service))
               .thenReturn(mapToCloudService(service));
    }

    private void prepareExistingServiceBindings() {
        for (String serviceName : input.existingServiceBindings.keySet()) {
            CloudServiceInstance cloudServiceInstance = Mockito.mock(CloudServiceInstance.class);
            CloudService cloudService = Mockito.mock(CloudService.class);
            List<CloudServiceBinding> serviceBindings = new ArrayList<>();
            for (SimpleBinding simpleBinding : input.existingServiceBindings.get(serviceName)) {
                serviceBindings.add(simpleBinding.toCloudServiceBinding());
            }
            Mockito.when(cloudServiceInstance.getBindings())
                   .thenReturn(serviceBindings);
            Mockito.when(cloudServiceInstance.getService())
                    .thenReturn(cloudService);
            Mockito.when(cloudService.getName())
                    .thenReturn(serviceName);
            Mockito.when(cloudServiceInstance.getMetadata())
                    .thenReturn(ImmutableCloudMetadata.builder()
                                                      .guid(NameUtil.getUUID(serviceName))
                                                      .build());
            Mockito.when(client.getServiceInstance(serviceName))
                   .thenReturn(cloudServiceInstance);
        }

        for (String serviceName : input.existingServiceKeys.keySet()) {
            List<CloudServiceKey> serviceKeys = input.existingServiceKeys.get(serviceName);
            Mockito.when(client.getServiceKeys(eq(serviceName)))
                   .thenReturn(serviceKeys);
        }
    }

    private List<CloudServiceExtended> mapToCloudServices() {
        return input.application.services.stream()
                                         .map(this::mapToCloudService)
                                         .collect(Collectors.toList());
    }

    private CloudServiceExtended mapToCloudService(String serviceName) {
        return new SimpleService(serviceName).toCloudService();
    }

    private List<String> prepareServicesToBind() {
        if (input.application.shouldKeepServiceBindings) {
            return ListUtils.union(input.application.services, input.existingApplication.services);
        }
        return input.application.services;
    }

    private void prepareDiscontinuedServices() {
        List<String> discontinuedServices = input.existingApplication.services.stream()
                                                                              .filter((service) -> !input.application.services.contains(service))
                                                                              .collect(Collectors.toList());
        notRequiredServices.addAll(discontinuedServices);
    }

    private void prepareContext() {
        Mockito.when(client.getApplication(eq(input.existingApplication.name), eq(false)))
               .thenReturn(input.existingApplication.toCloudApp());
        Mockito.when(client.getApplication(eq(input.existingApplication.name)))
               .thenReturn(input.existingApplication.toCloudApp());
        CloudApplicationExtended cloudApp = input.application.toCloudApp();
        // TODO
        StepsUtil.setAppsToDeploy(context, Collections.emptyList());
        StepsTestUtil.mockApplicationsToDeploy(Collections.singletonList(cloudApp), context);
        StepsUtil.setServicesToBind(context, mapToCloudServices());
        StepsUtil.setTriggeredServiceOperations(context, Collections.emptyMap());
        context.setVariable(Constants.VAR_MODULES_INDEX, 0);
        context.setVariable(Constants.PARAM_APP_ARCHIVE_ID, "dummy");
        byte[] serviceKeysToInjectByteArray = JsonUtil.toJsonBinary(new HashMap<>());
        context.setVariable(Constants.VAR_SERVICE_KEYS_CREDENTIALS_TO_INJECT, serviceKeysToInjectByteArray);
    }

    private static class StepInput {
        SimpleApplication application;
        SimpleApplication existingApplication;
        Map<String, List<SimpleBinding>> existingServiceBindings;
        final Map<String, List<CloudServiceKey>> existingServiceKeys = new HashMap<>();
        boolean updateStaging;
        boolean updateMemory;
        boolean updateDiskQuota;
        boolean updateUris;
        boolean updateEnv;
    }

    private static class SimpleBinding {
        String applicationName;
        Map<String, Object> bindingOptions;

        CloudServiceBinding toCloudServiceBinding() {
            return ImmutableCloudServiceBinding.builder()
                                               .applicationGuid(NameUtil.getUUID(applicationName))
                                               .bindingOptions(bindingOptions)
                                               .build();
        }
    }

    private static class SimpleApplication {
        String name;
        final List<String> services = Collections.emptyList();
        final Map<String, Map<String, Object>> bindingParameters = Collections.emptyMap();
        final Map<String, String> env = Collections.emptyMap();
        final List<ServiceKeyToInject> serviceKeysToInject = Collections.emptyList();
        String command;
        final List<String> uris = Collections.emptyList();
        String buildpackUrl;
        int memory;
        int instances;
        int diskQuota;
        String healthCheckType;
        String healthCheckHttpEndpoint;
        Boolean sshEnabled;
        boolean shouldKeepServiceBindings;

        CloudApplicationExtended toCloudApp() {
            return ImmutableCloudApplicationExtended.builder()
                                                    .attributesUpdateStrategy(ImmutableCloudApplicationExtended.ImmutableAttributeUpdateStrategy.builder()
                                                                                                                                                .shouldKeepExistingServiceBindings(shouldKeepServiceBindings)
                                                                                                                                                .build())
                                                    .name(name)
                                                    .moduleName("test")
                                                    .staging(ImmutableStaging.builder()
                                                                             .command(command)
                                                                             .buildpacks(Collections.singletonList(buildpackUrl))
                                                                             .healthCheckTimeout(0)
                                                                             .detectedBuildpack("none")
                                                                             .healthCheckType(healthCheckType)
                                                                             .healthCheckHttpEndpoint(healthCheckHttpEndpoint)
                                                                             .isSshEnabled(sshEnabled)
                                                                             .build())
                                                    .memory(memory)
                                                    .instances(instances)
                                                    .uris(uris)
                                                    .env(env)
                                                    .services(services)
                                                    .state(State.STARTED)
                                                    .diskQuota(diskQuota)
                                                    .bindingParameters(bindingParameters)
                                                    .serviceKeysToInject(serviceKeysToInject)
                                                    .metadata(ImmutableCloudMetadata.builder()
                                                                                    .guid(NameUtil.getUUID(name))
                                                                                    .build())
                                                    .build();
        }

    }

    private static class SimpleService {
        final String name;

        SimpleService(String name) {
            this.name = name;
        }

        CloudServiceExtended toCloudService() {
            return ImmutableCloudServiceExtended.builder()
                                                .metadata(ImmutableCloudMetadata.builder()
                                                                                .guid(NameUtil.getUUID(name))
                                                                                .build())
                                                .name(name)
                                                .build();
        }
    }

    @Override
    protected CreateOrUpdateAppStep createStep() {
        return new CreateAppStepMock();
    }

    private static class CreateAppStepMock extends CreateOrUpdateAppStep {
        @Override
        protected ApplicationServicesUpdateCallback getApplicationServicesUpdateCallback(DelegateExecution context) {
            return CALLBACK;
        }
    }

}
