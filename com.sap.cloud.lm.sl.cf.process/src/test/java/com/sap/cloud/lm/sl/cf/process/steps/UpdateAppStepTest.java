package com.sap.cloud.lm.sl.cf.process.steps;

import static org.mockito.Matchers.eq;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;
import org.cloudfoundry.client.lib.domain.CloudEntity.Meta;
import org.cloudfoundry.client.lib.domain.CloudServiceBinding;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.cloudfoundry.client.lib.domain.ServiceKey;
import org.cloudfoundry.client.lib.domain.Staging;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Matchers;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ServiceKeyToInject;
import com.sap.cloud.lm.sl.cf.core.cf.PlatformType;
import com.sap.cloud.lm.sl.cf.core.cf.clients.ApplicationStagingUpdater;
import com.sap.cloud.lm.sl.cf.core.util.NameUtil;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.util.GenericArgumentMatcher;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.ListUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Parameterized.class)
public class UpdateAppStepTest extends SyncFlowableStepTest<UpdateAppStep> {

    private final StepInput input;
    private final String expectedExceptionMessage;

    private List<String> notRequiredServices = new ArrayList<>();
    private List<String> expectedServicesToBind = new ArrayList<>();

    private ApplicationStagingUpdater applicationUpdaterMock = Mockito.mock(ApplicationStagingUpdater.class);

    private PlatformType platform;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            {
                "update-app-step-input-1.json", null, PlatformType.XS2
            },
            {
                "update-app-step-input-2.json", null, PlatformType.XS2
            },
            {
                "update-app-step-input-3.json", null, PlatformType.CF
            },
            {
                "update-app-step-input-4.json", null, PlatformType.XS2
            },
            {
                "update-app-step-input-5.json", null, PlatformType.CF
            },
            {
                "update-app-step-input-6.json", null, PlatformType.XS2
            },
            {
                "update-app-step-input-7.json", null, PlatformType.XS2
            },
            {
                "update-app-step-input-8.json", null, PlatformType.CF
            },
            {
                "update-app-step-input-9.json", null, PlatformType.XS2
            },
            {
                "update-app-step-input-10.json", null, PlatformType.XS2
            },
            {
                "update-app-step-input-11.json", null, PlatformType.CF
            },
            // Existing app has binding with null parameters and defined service binding is without parameters
            {
                "update-app-step-input-12.json", null, PlatformType.CF
            },
            // Existing app has binding with empty parameters and defined service binding is without parameters
            {
                "update-app-step-input-13.json", null, PlatformType.CF
            },
            // Existing app has binding with parameters and defined service binding is without parameters
            {
                "update-app-step-input-14.json", null, PlatformType.CF
            },
            // Existing app has binding with null parameters and defined service binding is with defined parameters
            {
                "update-app-step-input-15.json", null, PlatformType.CF
            },
            // Service keys to inject are specified
            {
                "update-app-step-input-16.json", null, PlatformType.XS2
            },
            // Service keys to inject are specified but does not exist
            {
                "update-app-step-input-17.json", "nable to retrieve required service key element \"expected-service-key\" for service \"existing-service-1\"", PlatformType.XS2
            },
            // Test enable-ssh parameter
            {
                "update-app-step-input-18.json", null, PlatformType.CF
            },
            // Test if healthCheckType parameter is updated
            {
                "update-app-step-input-19.json", null, PlatformType.CF
            },
            // Test if healthCheckHttpEndpoint parameter is updated
            {
                "update-app-step-input-20.json", null, PlatformType.CF
            }
// @formatter:on
        });
    }

    public UpdateAppStepTest(String input, String expectedExceptionMessage, PlatformType platform) throws Exception {
        this.input = JsonUtil.fromJson(TestUtil.getResourceAsString(input, UpdateAppStepTest.class), StepInput.class);
        this.platform = platform;
        this.expectedExceptionMessage = expectedExceptionMessage;
    }

    @Before
    public void setUp() throws Exception {
        if (expectedExceptionMessage != null) {
            expectedException.expectMessage(expectedExceptionMessage);
        }
        prepareContext();
        prepareClient();
    }

    @Test
    public void testExecute() throws Exception {
        step.execute(context);

        assertStepFinishedSuccessfully();

        validateUnbindServices();
        validateBindServices();
        validateUpdateComponents();
    }

    private void validateUpdateComponents() {
        String appName = input.application.name;
        CloudApplicationExtended cloudApp = input.application.toCloudApp();
        if (input.updateStaging && platform == PlatformType.XS2) {
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
                .updateApplicationEnv(appName, cloudApp.getEnvAsMap());
        }
        if (platform == PlatformType.CF) {
            Mockito.verify(applicationUpdaterMock)
                .updateApplicationStaging(eq(client), eq(cloudApp.getName()),
                    Matchers.argThat(GenericArgumentMatcher.forObject(cloudApp.getStaging())));
        }
    }

    private void validateBindServices() {
        Map<String, Map<String, Object>> currentBindingParameters = input.application.toCloudApp()
            .getBindingParameters();
        for (String serviceToBind : expectedServicesToBind) {
            if (currentBindingParameters != null && currentBindingParameters.get(serviceToBind) != null) {
                Mockito.verify(client)
                    .bindService(input.existingApplication.name, serviceToBind, currentBindingParameters.get(serviceToBind));
            } else {
                Mockito.verify(client)
                    .bindService(input.existingApplication.name, serviceToBind);
            }
        }
    }

    private void validateUnbindServices() {
        for (String notRquiredService : notRequiredServices) {
            Mockito.verify(client)
                .unbindService(input.existingApplication.name, notRquiredService);
        }
    }

    private void prepareClient() {
        prepareDiscontinuedServices();

        prepareServicesToBind();

        Mockito.when(configuration.getPlatformType())
            .thenReturn(platform);

        prepareExistingServiceBindings();
    }

    private void prepareExistingServiceBindings() {
        for (String serviceName : input.existingServiceBindings.keySet()) {
            CloudServiceInstance cloudServiceInstance = Mockito.mock(CloudServiceInstance.class);
            List<CloudServiceBinding> serviceBindings = new ArrayList<CloudServiceBinding>();
            for (SimpleBinding simpleBinding : input.existingServiceBindings.get(serviceName)) {
                serviceBindings.add(simpleBinding.toCloudServiceBinding());
            }
            Mockito.when(cloudServiceInstance.getBindings())
                .thenReturn(serviceBindings);
            Mockito.when(client.getServiceInstance(serviceName))
                .thenReturn(cloudServiceInstance);
        }

        for (String serviceName : input.existingServiceKeys.keySet()) {
            List<ServiceKey> serviceKeys = input.existingServiceKeys.get(serviceName);
            Mockito.when(client.getServiceKeys(eq(serviceName)))
                .thenReturn(ListUtil.upcast(serviceKeys));
        }
    }

    private List<CloudServiceExtended> mapToCloudServices() {
        return input.application.services.stream()
            .map(serviceName -> mapToCloudService(serviceName))
            .collect(Collectors.toList());
    }

    private CloudServiceExtended mapToCloudService(String serviceName) {
        return new SimpleService(serviceName).toCloudService();
    }

    private void prepareServicesToBind() {
        for (String service : this.input.application.services) {
            if (!this.input.existingApplication.services.contains(service)) {
                expectedServicesToBind.add(service);
                continue;
            }
            SimpleBinding existingBindingForApplication = getExistingBindingForApplication(service, this.input.existingApplication.name);
            if (existingBindingForApplication == null) {
                expectedServicesToBind.add(service);
                continue;
            }

            Map<String, Map<String, Object>> currentBindingParameters = input.application.toCloudApp()
                .getBindingParameters();

            boolean existingBindingParametersAreEmptyOrNull = existingBindingForApplication.bindingOptions == null
                || existingBindingForApplication.bindingOptions.isEmpty();

            boolean currentBindingParametersAreNull = currentBindingParameters == null || currentBindingParameters.get(service) == null;

            if (!existingBindingParametersAreEmptyOrNull && (currentBindingParametersAreNull
                || !existingBindingForApplication.bindingOptions.equals(currentBindingParameters.get(service)))) {
                expectedServicesToBind.add(service);
                continue;
            }
            if (!currentBindingParametersAreNull && !currentBindingParameters.get(service)
                .equals(existingBindingForApplication.bindingOptions)) {
                expectedServicesToBind.add(service);
                continue;
            }
        }
    }

    private void prepareDiscontinuedServices() {
        List<String> discontinuedServices = input.existingApplication.services.stream()
            .filter((service) -> !input.application.services.contains(service))
            .collect(Collectors.toList());
        notRequiredServices.addAll(discontinuedServices);
    }

    private SimpleBinding getExistingBindingForApplication(String service, String application) {
        for (SimpleBinding simpleBinding : this.input.existingServiceBindings.get(service)) {
            if (application.equals(simpleBinding.applicationName)) {
                return simpleBinding;
            }
        }
        return null;
    }

    private void prepareContext() {
        StepsUtil.setExistingApp(context, input.existingApplication.toCloudApp());
        CloudApplicationExtended cloudApp = input.application.toCloudApp();
        cloudApp.setModuleName("test");
        StepsUtil.setAppsToDeploy(context, Arrays.asList(cloudApp));
        StepsTestUtil.mockApplicationsToDeploy(Arrays.asList(cloudApp), context);
        StepsUtil.setServicesToBind(context, mapToCloudServices());
        StepsUtil.setTriggeredServiceOperations(context, Collections.emptyMap());
        context.setVariable(Constants.VAR_MODULES_INDEX, 0);
        context.setVariable(Constants.PARAM_APP_ARCHIVE_ID, "dummy");
        byte[] serviceKeysToInjectByteArray = JsonUtil.toBinaryJson(new HashMap<>());
        context.setVariable(Constants.VAR_SERVICE_KEYS_CREDENTIALS_TO_INJECT, serviceKeysToInjectByteArray);
    }

    private static class StepInput {
        SimpleApplication application;
        SimpleApplication existingApplication;
        Map<String, List<SimpleBinding>> existingServiceBindings;
        Map<String, List<ServiceKey>> existingServiceKeys = new HashMap<>();
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
            CloudServiceBinding binding = new CloudServiceBinding();
            binding.setAppGuid(NameUtil.getUUID(applicationName));
            binding.setBindingOptions(bindingOptions);
            return binding;
        }
    }

    private static class SimpleApplication {
        String name;
        List<String> services;
        Map<String, Map<String, Object>> bindingParameters;
        List<ServiceKeyToInject> serviceKeysToInject;
        String command;
        List<String> uris;
        String buildpackUrl;
        int memory;
        int instances;
        int diskQuota;
        String healthCheckType;
        String healthCheckHttpEndpoint;
        Boolean sshEnabled;

        CloudApplicationExtended toCloudApp() {
            CloudApplicationExtended cloudApp = new CloudApplicationExtended(name, command, buildpackUrl, memory, instances, uris, services,
                AppState.STARTED, Collections.emptyList(), Collections.emptyList(), null);
            cloudApp.setMeta(new Meta(NameUtil.getUUID(name), null, null));
            cloudApp.setDiskQuota(diskQuota);
            cloudApp.setStaging(new Staging.StagingBuilder().command(command)
                .buildpackUrl(buildpackUrl)
                .stack(null)
                .healthCheckTimeout(0)
                .detectedBuildpack("none")
                .healthCheckType(healthCheckType)
                .healthCheckHttpEndpoint(healthCheckHttpEndpoint)
                .sshEnabled(sshEnabled)
                .build());
            cloudApp.setBindingParameters(bindingParameters);
            cloudApp.setServiceKeysToInject(serviceKeysToInject);
            return cloudApp;
        }

    }

    private static class SimpleService {
        String name;

        SimpleService(String name) {
            this.name = name;
        }

        CloudServiceExtended toCloudService() {
            return new CloudServiceExtended(new Meta(NameUtil.getUUID(name), null, null), name);
        }
    }

    @Override
    protected UpdateAppStep createStep() {
        return new UpdateAppStep();
    }

}
