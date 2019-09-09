package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.CloudServiceBinding;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.cloudfoundry.client.lib.domain.ImmutableCloudMetadata;
import org.cloudfoundry.client.lib.domain.ImmutableCloudServiceBinding;
import org.cloudfoundry.client.lib.domain.ImmutableCloudServiceInstance;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ImmutableCloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.helpers.MapToEnvironmentConverter;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaModule;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaResource;
import com.sap.cloud.lm.sl.cf.core.util.NameUtil;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Parameterized.class)
public class CheckForCreationConflictsStepTest extends SyncFlowableStepTest<CheckForCreationConflictsStep> {

    private final StepInput stepInput;
    private final String expectedExceptionMessage;
    private Map<CloudServiceExtended, CloudServiceInstance> existingServiceInstances;
    private boolean shouldWarn;
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) Services to deploy don't exist; applications to deploy don't exist -> should be OK:
            {
                "check-for-creation-conflicts-step-input-1.json", null, false,
            },
            // (1) Services to deploy exist, not part of the deployed MTA, don't have bound applications -> should warn:
            {
                "check-for-creation-conflicts-step-input-2.json", null, true,
            },
            // (2) Services to deploy exist, not part of the deployed MTA, have bound applications -> expect exception:
            {
                "check-for-creation-conflicts-step-input-3.json", null, true,
            },
            // (3) Services to deploy exist, part of the deployed MTA, don't have bound applications -> should be OK:
            {
                "check-for-creation-conflicts-step-input-4.json", null, false,
            },
            // (4) Services to deploy exist, part of the deployed MTA, have bound applications -> should be OK:
            {
                "check-for-creation-conflicts-step-input-5.json", null, false,
            },
            // (5) Applications to deploy exist, not part of the deployed MTA, but stand-alone -> should warn:
            {
                "check-for-creation-conflicts-step-input-6.json", null, true,
            },
            // (6) Applications to deploy exist, part of the deployed MTA -> should be OK:
            {
                "check-for-creation-conflicts-step-input-7.json", null, false
            },
            // (7) Services to deploy exist, not part of the deployed MTA, have bound applications from another MTA -> expect exception:
            {
                "check-for-creation-conflicts-step-input-8.json", MessageFormat.format(Messages.SERVICE_ASSOCIATED_WITH_OTHER_MTAS, "service-1", "com.sap.example.mta-1, com.sap.example.mta-2"), false,
            },
            // (8) Services to deploy exist, not part of the deployed MTA, have bound applications from another MTA, but they do not claim to 'own' the service -> should be OK:
            {
                "check-for-creation-conflicts-step-input-9.json", null, false,
            },
// @formatter:on
        });
    }

    public CheckForCreationConflictsStepTest(String stepInput, String expectedExceptionMessage, boolean shouldWarn) throws Exception {
        this.stepInput = JsonUtil.fromJson(TestUtil.getResourceAsString(stepInput, CheckForCreationConflictsStepTest.class),
                                           StepInput.class);
        this.expectedExceptionMessage = expectedExceptionMessage;
        this.shouldWarn = shouldWarn;
    }

    @Before
    public void setUp() throws Exception {
        prepareException();
        prepareDeployedMta();
        prepareContext();
        prepareClient();
    }

    @Test
    public void testExecute() throws Exception {
        step.execute(context);

        assertStepFinishedSuccessfully();
        if (shouldWarn) {
            Mockito.verify(stepLogger, Mockito.atLeastOnce())
                   .warn(Mockito.anyString());
        }
    }

    private void prepareException() {
        if (expectedExceptionMessage != null) {
            expectedException.expectMessage(expectedExceptionMessage);
            expectedException.expect(SLException.class);
        }
    }

    private void prepareDeployedMta() {
        DeployedMta deployedMta = new DeployedMta();
        prepareServices(deployedMta);
        prepareModules(deployedMta);
        StepsUtil.setDeployedMta(context, deployedMta);
    }

    private void prepareModules(DeployedMta deployedMta) {
        List<DeployedMtaModule> deployedModules = simpleAppListToModuleList(stepInput.appsFromDeployedMta);
        deployedMta.setModules(deployedModules);
    }

    private void prepareServices(DeployedMta deployedMta) {
        Set<String> servicesNames = new HashSet<>();
        stepInput.servicesFromDeployedMta.forEach(service -> servicesNames.add(service.getName()));
        List<DeployedMtaResource> deployedServices = servicesNames.stream()
                                                                 .map(s -> DeployedMtaResource.builder().withServiceName(s).build())
                                                                 .collect(Collectors.toList());
        deployedMta.setResources(deployedServices);
    }

    private void prepareContext() {
        StepsUtil.setServicesToCreate(context, stepInput.servicesToDeploy);
        List<String> appsToDeploy = new ArrayList<>();
        stepInput.appsToDeploy.forEach(app -> appsToDeploy.add(app.name));
        StepsUtil.setAppsToDeploy(context, appsToDeploy);
        List<CloudApplication> existingApps = new ArrayList<>();
        stepInput.existingApps.forEach(app -> existingApps.add(app.toCloudApplication()));
        StepsUtil.setDeployedApps(context, existingApps);
    }

    private List<DeployedMtaModule> simpleAppListToModuleList(List<SimpleApplication> simpleApps) {
        List<DeployedMtaModule> modulesList = new ArrayList<>();
        simpleApps.forEach(app -> modulesList.add(DeployedMtaModule.builder().withAppName(app.name).withModuleName(app.name).build()));
        return modulesList;
    }

    private void prepareClient() throws Exception {
        existingServiceInstances = createServiceInstances(stepInput);
        prepareExistingServices();
    }

    private Map<CloudServiceExtended, CloudServiceInstance> createServiceInstances(StepInput stepInput) throws Exception {
        Map<CloudServiceExtended, CloudServiceInstance> result = new HashMap<>();
        for (CloudServiceExtended service : stepInput.existingServices) {
            List<SimpleApplication> boundApplications = findBoundApplications(service.getName(), stepInput.existingApps);
            result.put(service, createServiceInstance(service, boundApplications));
        }
        return result;
    }

    private List<SimpleApplication> findBoundApplications(String serviceName, List<SimpleApplication> applications) {
        return applications.stream()
                           .filter((application) -> application.boundServices.contains(serviceName))
                           .collect(Collectors.toList());
    }

    private CloudServiceInstance createServiceInstance(CloudServiceExtended service, List<SimpleApplication> boundApplications) {
        return ImmutableCloudServiceInstance.builder()
                                            .bindings(createServiceBindings(boundApplications))
                                            .credentials(service.getCredentials())
                                            .build();
    }

    private List<CloudServiceBinding> createServiceBindings(List<SimpleApplication> boundApplications) {
        return boundApplications.stream()
                                .map(boundApplication -> createServiceBinding(boundApplication))
                                .collect(Collectors.toList());
    }

    private CloudServiceBinding createServiceBinding(SimpleApplication boundApplication) {
        return ImmutableCloudServiceBinding.builder()
                                           .applicationGuid(NameUtil.getUUID(boundApplication.name))
                                           .build();
    }

    private void prepareExistingServices() {
        List<CloudService> existingServices = new ArrayList<>();
        stepInput.existingServices.forEach(service -> existingServices.add(service));
        Mockito.when(client.getServices())
               .thenReturn(existingServices);
        prepareServiceInstances();

    }

    private void prepareServiceInstances() {
        existingServiceInstances.forEach((service, instance) -> prepareServiceInstance(service, instance));
    }

    private void prepareServiceInstance(CloudServiceExtended service, CloudServiceInstance instance) {
        Mockito.when(client.getServiceInstance(service.getName()))
               .thenReturn(instance);
    }

    @Override
    protected CheckForCreationConflictsStep createStep() {
        return new CheckForCreationConflictsStep();
    }

    private static class StepInput {
        List<CloudServiceExtended> servicesToDeploy = Collections.emptyList();
        List<CloudServiceExtended> existingServices = Collections.emptyList();
        List<CloudServiceExtended> servicesFromDeployedMta = Collections.emptyList();

        List<SimpleApplication> appsToDeploy = Collections.emptyList();
        List<SimpleApplication> existingApps = Collections.emptyList();
        List<SimpleApplication> appsFromDeployedMta = Collections.emptyList();
    }

    private static final MapToEnvironmentConverter ENV_CONVERTER = new MapToEnvironmentConverter(false);

    private static class SimpleApplication {

        String name;
        List<String> boundServices = Collections.emptyList();
        Map<String, Object> env = Collections.emptyMap();

        CloudApplicationExtended toCloudApplication() {
            return ImmutableCloudApplicationExtended.builder()
                                                    .metadata(ImmutableCloudMetadata.builder()
                                                                                    .guid(NameUtil.getUUID(name))
                                                                                    .build())
                                                    .name(name)
                                                    .env(ENV_CONVERTER.asEnv(env))
                                                    .build();
        }
    }

}
