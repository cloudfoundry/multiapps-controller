package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.cloudfoundry.client.lib.domain.CloudEntity.Meta;
import org.cloudfoundry.client.lib.domain.CloudServiceBinding;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.cloudfoundry.client.lib.domain.ServiceKey;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.clients.ServiceCreator;
import com.sap.cloud.lm.sl.cf.core.cf.clients.ServiceUpdater;
import com.sap.cloud.lm.sl.cf.core.util.NameUtil;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.GenericArgumentMatcher;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.ListUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Parameterized.class)
public class CreateOrUpdateServicesStepTest extends SyncFlowableStepTest<CreateOrUpdateServicesStep> {

    private static final String TEST_SPACE_ID = "test-space-id";
    private final StepInput stepInput;
    private final String expectedExceptionMessage;
    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    private Map<CloudServiceExtended, CloudServiceInstance> existingServiceInstances;
    @Mock
    private ServiceCreator serviceCreator;
    @Mock
    private ServiceUpdater serviceUpdater;

    public CreateOrUpdateServicesStepTest(String stepInput, String expectedExceptionMessage) throws Exception {
        this.stepInput = JsonUtil.fromJson(TestUtil.getResourceAsString(stepInput, CreateOrUpdateServicesStepTest.class), StepInput.class);
        this.expectedExceptionMessage = expectedExceptionMessage;
    }

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) No existing services:
            {
                "create-or-update-services-step-input-1.json", null,
            },
            // (1) Existing services are part of the MTA, there are external applications bound to them, however there's no need to update them:
            {
                "create-or-update-services-step-input-2.json", null,
            },
            // (2) Test created service type (user-provided or managed):
            {
                "create-or-update-services-step-input-3.json", null,
            },
            // (3) Existing services are part of the MTA, there are external applications bound to them and they should be updated:
            {
                "create-or-update-services-step-input-4.json", null,
            },
            // (4) Existing services are not relevant to the MTA:
            {
                "create-or-update-services-step-input-5.json", null,
            },
            // (5) Existing services are part of the MTA, there are discontinued applications bound to them and they should be updated:
            {
                "create-or-update-services-step-input-6.json", null,
            },
            // (6) New services have more service tags than the existing (tags should be updated):
            {
                "create-or-update-services-step-input-7.json", null,
            },
            // (7) New services have more service tags than the existing (tags should be updated):
            {
                "create-or-update-services-step-input-8.json", null,
            },
            // (8) New services have less service tags than the existing (tags should be updated):
            {
                "create-or-update-services-step-input-9.json", null,
            },
            // (9)
            {
                "create-or-update-services-step-input-10.json", null,
            },
            // (10)
            {
                "create-or-update-services-step-input-11.json", null,
            },
            // (11) Existing services contain default tags, which are not specified in the deployment descriptor, but there are no other differences between the services (tags should not be updated):
            {
                "create-or-update-services-step-input-12.json", null,
            },
            // (12) Test update of service keys:
            {
                "create-or-update-services-step-input-13.json", null,
            },
            // (13) Test delete of service keys:
            {
                "create-or-update-services-step-input-14.json", null,
            },
            // (14) Test delete of service keys:
            {
                "create-or-update-services-step-input-15.json", null,
            },
// @formatter:on
        });
    }

    @Before
    public void setUp() throws Exception {
        loadParameters();
        prepareContext();
        prepareClient();
    }

    @Test
    public void testExecute() throws Exception {
        step.execute(context);

        assertStepIsRunning();

        validateClient();
    }

    private void assertStepIsRunning() {
        assertEquals(StepPhase.POLL.toString(), getExecutionStatus());
    }

    private void loadParameters() throws Exception {
        if (expectedExceptionMessage != null) {
            expectedException.expectMessage(expectedExceptionMessage);
            expectedException.expect(SLException.class);
        }
        existingServiceInstances = createServiceInstances(stepInput);
    }

    private void prepareContext() throws Exception {
        StepsUtil.setServicesToCreate(context, ListUtil.upcastUnmodifiable(stepInput.services));
        StepsUtil.setServicesToDelete(context, Collections.emptyList());
        StepsUtil.setAppsToDeploy(context, toCloudApplications(stepInput.applications));
        StepsUtil.setAppsToUndeploy(context, ListUtil.upcastUnmodifiable(toCloudApplications(stepInput.discontinuedApplications)));
        StepsUtil.setServiceKeysToCreate(context, stepInput.getServiceKeysToCreate());

        context.setVariable(com.sap.cloud.lm.sl.cf.process.Constants.PARAM_DELETE_SERVICES, true);
        context.setVariable(com.sap.cloud.lm.sl.cf.process.Constants.PARAM_DELETE_SERVICE_KEYS, true);
        context.setVariable(com.sap.cloud.lm.sl.cf.persistence.message.Constants.VARIABLE_NAME_SPACE_ID, TEST_SPACE_ID);
        context.setVariable("servicesToCreateCount", 0);
    }

    private void prepareClient() throws Exception {
        Mockito.when(client.getServices())
               .thenReturn(ListUtil.upcastUnmodifiable(stepInput.existingServices));

        prepareServiceInstances();
        prepareBoundApplications();

        Map<String, List<ServiceKey>> existingServiceKeys = stepInput.getExistingServiceKeys();
        for (String serviceName : existingServiceKeys.keySet()) {
            Mockito.when(client.getServiceKeys(serviceName))
                   .thenReturn(existingServiceKeys.get(serviceName));
        }
        Mockito.doNothing()
               .when(client)
               .deleteServiceKey(Mockito.any(), Mockito.any());
        Mockito.doNothing()
               .when(client)
               .createServiceKey(Mockito.any(), Mockito.any(), Mockito.any());
    }

    private Map<CloudServiceExtended, CloudServiceInstance> createServiceInstances(StepInput stepInput) throws Exception {
        Map<CloudServiceExtended, CloudServiceInstance> result = new HashMap<>();
        for (CloudServiceExtended service : getRelevantServices()) {
            List<SimpleApplication> boundApplications = findBoundApplications(service.getName(), stepInput.existingApplications);
            result.put(service, createServiceInstance(service, boundApplications));
        }
        return result;
    }

    private List<CloudServiceExtended> getRelevantServices() {
        return stepInput.existingServices.stream()
                                         .filter((service) -> exists(stepInput.services, service.getName()))
                                         .collect(Collectors.toList());
    }

    private boolean exists(List<CloudServiceExtended> services, String serviceName) {
        return services.stream()
                       .anyMatch((service) -> service.getName()
                                                     .equals(serviceName));
    }

    private List<SimpleApplication> findBoundApplications(String serviceName, List<SimpleApplication> applications) {
        return applications.stream()
                           .filter((application) -> application.boundServices.contains(serviceName))
                           .collect(Collectors.toList());
    }

    private CloudServiceInstance createServiceInstance(CloudServiceExtended service, List<SimpleApplication> boundApplications) {
        CloudServiceInstance instance = new CloudServiceInstance();
        instance.setBindings(createServiceBindings(boundApplications));
        instance.setCredentials(service.getCredentials());
        return instance;
    }

    private List<CloudServiceBinding> createServiceBindings(List<SimpleApplication> boundApplications) {
        return boundApplications.stream()
                                .map((boundApplication) -> createServiceBinding(boundApplication))
                                .collect(Collectors.toList());
    }

    private CloudServiceBinding createServiceBinding(SimpleApplication boundApplication) {
        CloudServiceBinding binding = new CloudServiceBinding();
        binding.setAppGuid(NameUtil.getUUID(boundApplication.name));
        return binding;
    }

    private List<CloudApplicationExtended> toCloudApplications(List<SimpleApplication> applications) {
        return applications.stream()
                           .map((application) -> application.toCloudApplication())
                           .collect(Collectors.toList());
    }

    private void prepareServiceInstances() {
        existingServiceInstances.forEach((service, instance) -> prepareServiceInstance(service, instance));
    }

    private void prepareServiceInstance(CloudServiceExtended service, CloudServiceInstance instance) {
        Mockito.when(client.getServiceInstance(service.getName()))
               .thenReturn(instance);
    }

    private void prepareBoundApplications() {
        Mockito.when(client.getApplications())
               .thenReturn(stepInput.existingApplications.stream()
                                                         .map(app -> app.toCloudApplication())
                                                         .collect(Collectors.toList()));
        stepInput.existingApplications.forEach((application) -> prepareBoundApplication(application));
    }

    private void prepareBoundApplication(SimpleApplication application) {
        Mockito.when(client.getApplication(NameUtil.getUUID(application.name)))
               .thenReturn(application.toCloudApplication());
    }

    private void validateClient() {
        validateServicesToDelete();
        validateServicesToCreate();
        validateServicesToBeUpdated();
        validateServiceKeysToCreate();
        validateServiceKeysToDelete();
    }

    private void validateServicesToBeUpdated() {
        for (CloudServiceExtended service : existingServiceInstances.keySet()) {
            if (shouldHavePlanBeenUpdated(service)) {
                Mockito.verify(serviceUpdater)
                       .updateServicePlan(client, service.getName(), findService(service.getName(), stepInput.services).getPlan());
            }

            if (shouldHaveTagsBeenUpdated(service)) {
                Mockito.verify(serviceUpdater)
                       .updateServiceTags(client, service.getName(), findService(service.getName(), stepInput.services).getTags());
            }
        }
    }

    private boolean shouldHavePlanBeenUpdated(CloudServiceExtended existingService) {
        return !areEqual(existingService.getPlan(), findService(existingService.getName(), stepInput.services).getPlan());
    }

    private boolean shouldHaveTagsBeenUpdated(CloudServiceExtended existingService) {
        CloudServiceExtended serviceToCreate = findService(existingService.getName(), stepInput.services);
        List<String> tags = new ArrayList<>(serviceToCreate.getTags());
        return !areEqual(tags, existingService.getTags());
    }

    private void validateServicesToDelete() {
        for (CloudServiceExtended service : existingServiceInstances.keySet()) {
            if (shouldHaveBeenRecreated(service)) {
                validateServiceWasDeleted(service, existingServiceInstances.get(service));
            }
        }
    }

    private void validateServiceKeysToCreate() {
        Map<String, List<ServiceKey>> serviceKeysToCreate = stepInput.getServiceKeysToCreate();
        for (String serviceName : stepInput.expectedCreatedServiceKeys.keySet()) {
            for (String keyName : stepInput.expectedCreatedServiceKeys.get(serviceName)) {
                ServiceKey keyToCreate = findKey(serviceKeysToCreate, serviceName, keyName);
                Mockito.verify(client)
                       .createServiceKey(serviceName, keyToCreate.getName(), keyToCreate.getParameters());
            }
        }
    }

    private void validateServiceKeysToDelete() {
        for (String serviceName : stepInput.expectedDeletedServiceKeys.keySet()) {
            for (String keyName : stepInput.expectedDeletedServiceKeys.get(serviceName)) {
                Mockito.verify(client)
                       .deleteServiceKey(serviceName, keyName);
            }
        }
    }

    private ServiceKey findKey(Map<String, List<ServiceKey>> serviceKeysToCreate, String serviceName, String keyName) {
        return serviceKeysToCreate.get(serviceName)
                                  .stream()
                                  .filter(key -> key.getName()
                                                    .equals(keyName))
                                  .findAny()
                                  .orElse(null);
    }

    private void validateServiceWasDeleted(CloudServiceExtended service, CloudServiceInstance instance) {
        for (CloudServiceBinding binding : instance.getBindings()) {
            String applicationName = client.getApplication(binding.getAppGuid())
                                           .getName();
            Mockito.verify(client, Mockito.times(1))
                   .unbindService(applicationName, service.getName());
        }
        Mockito.verify(client, Mockito.times(1))
               .deleteService(service.getName());
    }

    private boolean shouldHaveBeenRecreated(CloudServiceExtended existingService) {
        return !areServicesEqual(existingService, findService(existingService.getName(), stepInput.services));
    }

    private boolean areServicesEqual(CloudServiceExtended existingService, CloudServiceExtended service) {

        if (!areEqual(existingService.getLabel(), service.getLabel())) {
            return false;
        }
        if (!areEqual(existingService.getName(), service.getName())) {
            return false;
        }
        if (!areEqual(existingService.getProvider(), service.getProvider())) {
            return false;
        }
        return true;
    }

    private <T> boolean areEqual(T existingServiceValue, T serviceValue) {
        if (existingServiceValue != null && serviceValue != null && !existingServiceValue.equals(serviceValue)) {
            return false;
        }
        return true;
    }

    private CloudServiceExtended findService(String serviceName, List<CloudServiceExtended> services) {
        return services.stream()
                       .filter(service -> service.getName()
                                                 .equals(serviceName))
                       .findAny()
                       .orElse(null);
    }

    private void validateServicesToCreate() {
        for (CloudServiceExtended service : stepInput.services) {
            if (exists(service) && !shouldHaveBeenRecreated(service)) {
                continue;
            }
            if (service.isUserProvided()) {
                Mockito.verify(client, Mockito.times(1))
                       .createUserProvidedService(Matchers.argThat(GenericArgumentMatcher.forObject(service)),
                                                  Matchers.eq(service.getCredentials()));
            } else {
                Mockito.verify(serviceCreator, Mockito.times(1))
                       .createService(Mockito.eq(client), Matchers.argThat(GenericArgumentMatcher.forObject(service)),
                                      Mockito.eq(TEST_SPACE_ID));
            }
        }
    }

    private boolean exists(CloudServiceExtended service) {
        return findService(service.getName(), stepInput.existingServices) != null;
    }

    @Override
    protected CreateOrUpdateServicesStep createStep() {
        return new CreateOrUpdateServicesStep();
    }

    private static class StepInput {

        List<CloudServiceExtended> services = Collections.emptyList();
        List<SimpleApplication> applications = Collections.emptyList();
        List<CloudServiceExtended> existingServices = Collections.emptyList();
        List<SimpleApplication> existingApplications = Collections.emptyList();
        List<SimpleApplication> discontinuedApplications = Collections.emptyList();
        Map<String, List<ServiceKey>> serviceKeysToCreate = Collections.emptyMap();
        Map<String, List<ServiceKey>> existingServiceKeys = Collections.emptyMap();
        Map<String, List<String>> expectedCreatedServiceKeys = Collections.emptyMap();
        Map<String, List<String>> expectedDeletedServiceKeys = Collections.emptyMap();

        Map<String, List<ServiceKey>> getServiceKeysToCreate() {
            Map<String, List<ServiceKey>> result = new HashMap<>();
            for (Map.Entry<String, List<ServiceKey>> entry : serviceKeysToCreate.entrySet()) {
                result.put(entry.getKey(), ListUtil.upcastUnmodifiable(entry.getValue()));
            }
            return result;
        }

        Map<String, List<ServiceKey>> getExistingServiceKeys() {
            Map<String, List<ServiceKey>> result = new HashMap<>();
            for (Map.Entry<String, List<ServiceKey>> entry : existingServiceKeys.entrySet()) {
                result.put(entry.getKey(), ListUtil.upcastUnmodifiable(entry.getValue()));
            }
            return result;
        }

    }

    private static class SimpleApplication {

        String name;
        List<String> boundServices;

        CloudApplicationExtended toCloudApplication() {
            return new CloudApplicationExtended(new Meta(NameUtil.getUUID(name), null, null), name);
        }
    }

}
