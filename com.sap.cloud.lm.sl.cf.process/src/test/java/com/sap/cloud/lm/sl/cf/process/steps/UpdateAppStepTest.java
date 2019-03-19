package com.sap.cloud.lm.sl.cf.process.steps;

import static org.mockito.Matchers.eq;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;
import org.cloudfoundry.client.lib.ApplicationServicesUpdateCallback;
import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;
import org.cloudfoundry.client.lib.domain.CloudEntity.Meta;
import org.cloudfoundry.client.lib.domain.CloudServiceBinding;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.cloudfoundry.client.lib.domain.ServiceKey;
import org.cloudfoundry.client.lib.domain.Staging;
import org.flowable.engine.delegate.DelegateExecution;
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
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended.AttributeUpdateStrategy;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ServiceKeyToInject;
import com.sap.cloud.lm.sl.cf.core.cf.PlatformType;
import com.sap.cloud.lm.sl.cf.core.util.NameUtil;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.util.GenericArgumentMatcher;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.ListUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Parameterized.class)
public class UpdateAppStepTest extends SyncFlowableStepTest<UpdateAppStep> {

    private static final ApplicationServicesUpdateCallback CALLBACK = ApplicationServicesUpdateCallback.DEFAULT_APPLICATION_SERVICES_UPDATE_CALLBACK;

    private final StepInput input;
    private final String expectedExceptionMessage;

    private List<String> notRequiredServices = new ArrayList<>();
    private List<String> expectedServicesToBind = new ArrayList<>();

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
            Mockito.verify(client)
                .updateApplicationStaging(eq(cloudApp.getName()),
                    Matchers.argThat(GenericArgumentMatcher.forObject(cloudApp.getStaging())));
        }
    }

    private void validateBindServices() {
        Map<String, Map<String, Object>> currentBindingParameters = input.application.toCloudApp()
            .getBindingParameters();
        Map<String, Map<String, Object>> test = new HashMap<>();
        for (String serviceToBind : expectedServicesToBind) {
            test.put(mapToCloudService(serviceToBind).getName(), getBindingParametersForService(currentBindingParameters, serviceToBind));
        }
        Mockito.verify(client)
            .updateApplicationServices(input.existingApplication.name, test, step.getApplicationServicesUpdateCallback(context));
    }

    private Map<String, Object> getBindingParametersForService(Map<String, Map<String, Object>> bindingParameters, String serviceName) {
        return bindingParameters == null ? Collections.emptyMap() : bindingParameters.getOrDefault(serviceName, Collections.emptyMap());
    }

    private void validateUnbindServices() {
        for (String notRquiredService : notRequiredServices) {
            Mockito.verify(client)
                .unbindService(input.existingApplication.name, notRquiredService);
        }
    }

    private void prepareClient() {
        prepareDiscontinuedServices();

        expectedServicesToBind = prepareServicesToBind();

        Mockito.when(configuration.getPlatformType())
            .thenReturn(platform);

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
        StepsUtil.setExistingApp(context, input.existingApplication.toCloudApp());
        CloudApplicationExtended cloudApp = input.application.toCloudApp();
        cloudApp.setModuleName("test");
        //TODO
        StepsUtil.setAppsToDeploy(context, Collections.emptyList());
        StepsTestUtil.mockApplicationsToDeploy(Arrays.asList(cloudApp), context);
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
            binding.setApplicationGuid(NameUtil.getUUID(applicationName));
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
        boolean shouldKeepServiceBindings;

        CloudApplicationExtended toCloudApp() {
            AttributeUpdateStrategy applicationAttributeUpdateStrategy = new CloudApplicationExtended.AttributeUpdateStrategy.Builder()
                .shouldKeepExistingServiceBindings(shouldKeepServiceBindings)
                .build();
            CloudApplicationExtended cloudApp = new CloudApplicationExtended(name, command, buildpackUrl, memory, instances, uris, services,
                AppState.STARTED, Collections.emptyList(), Collections.emptyList(), null);
            cloudApp.setMeta(new Meta(NameUtil.getUUID(name), null, null));
            cloudApp.setDiskQuota(diskQuota);
            cloudApp.setApplicationAttributesUpdateBehavior(applicationAttributeUpdateStrategy);
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
        return new UpdateAppStepMock();
    }

    private class UpdateAppStepMock extends UpdateAppStep {
        @Override
        protected ApplicationServicesUpdateCallback getApplicationServicesUpdateCallback(DelegateExecution context) {
            return CALLBACK;
        }
    }

}
