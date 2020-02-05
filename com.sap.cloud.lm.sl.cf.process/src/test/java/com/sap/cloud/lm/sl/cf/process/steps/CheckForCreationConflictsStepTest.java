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
import org.cloudfoundry.client.lib.domain.CloudServiceBinding;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.cloudfoundry.client.lib.domain.ImmutableCloudMetadata;
import org.cloudfoundry.client.lib.domain.ImmutableCloudServiceBinding;
import org.cloudfoundry.client.lib.domain.ImmutableCloudServiceInstance;
import org.cloudfoundry.client.v3.Metadata;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;
import org.mockito.Spy;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ImmutableCloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.ImmutableMtaMetadata;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.MtaMetadata;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.processor.EnvMtaMetadataParser;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.processor.EnvMtaMetadataValidator;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.processor.MtaMetadataParser;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.processor.MtaMetadataValidator;
import com.sap.cloud.lm.sl.cf.core.helpers.MapToEnvironmentConverter;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaApplication;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaService;
import com.sap.cloud.lm.sl.cf.core.model.ImmutableDeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.ImmutableDeployedMtaApplication;
import com.sap.cloud.lm.sl.cf.core.model.ImmutableDeployedMtaService;
import com.sap.cloud.lm.sl.cf.core.util.NameUtil;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Parameterized.class)
public class CheckForCreationConflictsStepTest extends SyncFlowableStepTest<CheckForCreationConflictsStep> {

    private final StepInput stepInput;
    private final String expectedExceptionMessage;
    private final boolean shouldWarn;
    @Rule
    public final ExpectedException expectedException = ExpectedException.none();
    private MtaMetadataValidator mtaMetadataValidator = new MtaMetadataValidator();
    private EnvMtaMetadataValidator envMtaMetadataValidator = new EnvMtaMetadataValidator();
    @Spy
    private MtaMetadataParser mtaMetadataParser = new MtaMetadataParser(mtaMetadataValidator);
    @Spy
    private EnvMtaMetadataParser envMtaMetadataParser = new EnvMtaMetadataParser(envMtaMetadataValidator);

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

    public CheckForCreationConflictsStepTest(String stepInput, String expectedExceptionMessage, boolean shouldWarn) {
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
    public void testExecute() {
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
        MtaMetadata mtaMetadata = ImmutableMtaMetadata.builder()
                                                      .id("test")
                                                      .build();
        DeployedMta deployedMta = ImmutableDeployedMta.builder()
                                                      .metadata(mtaMetadata)
                                                      .applications(prepareApps())
                                                      .services(prepareServices())
                                                      .build();
        StepsUtil.setDeployedMta(context, deployedMta);
    }

    private List<DeployedMtaApplication> prepareApps() {
        return simpleAppListToAppList(stepInput.appsFromDeployedMta);
    }

    private List<DeployedMtaService> prepareServices() {
        Set<String> servicesNames = new HashSet<>();
        stepInput.servicesFromDeployedMta.forEach(service -> servicesNames.add(service.getName()));
        return servicesNames.stream()
                            .map(this::createDeployedMtaService)
                            .collect(Collectors.toList());
    }

    private DeployedMtaService createDeployedMtaService(String serviceName) {
        return ImmutableDeployedMtaService.builder()
                                          .name(serviceName)
                                          .build();
    }

    private void prepareContext() {
        StepsUtil.setServicesToCreate(context, stepInput.servicesToDeploy);
        List<String> appsToDeploy = new ArrayList<>();
        stepInput.appsToDeploy.forEach(app -> appsToDeploy.add(app.name));
        StepsUtil.setAppsToDeploy(context, appsToDeploy);
    }

    private List<DeployedMtaApplication> simpleAppListToAppList(List<SimpleApplication> simpleApps) {
        List<DeployedMtaApplication> modulesList = new ArrayList<>();
        simpleApps.forEach(app -> modulesList.add(createDeployedMtaApplication(app.name)));
        return modulesList;
    }

    private DeployedMtaApplication createDeployedMtaApplication(String appName) {
        return ImmutableDeployedMtaApplication.builder()
                                              .name(appName)
                                              .moduleName(appName)
                                              .build();
    }

    private void prepareClient() {
        Map<CloudServiceExtended, CloudServiceInstance> existingServiceInstances = createServiceInstances(stepInput);
        existingServiceInstances.forEach(this::prepareServiceInstance);
        stepInput.existingApps.stream()
                              .map(SimpleApplication::toCloudApplication)
                              .forEach(this::prepareExistingApplication);
    }

    private void prepareExistingApplication(CloudApplication application) {
        Mockito.when(client.getApplication(NameUtil.getUUID(application.getName())))
               .thenReturn(application);
        Mockito.when(client.getApplication(application.getName(), false))
               .thenReturn(application);
    }

    private Map<CloudServiceExtended, CloudServiceInstance> createServiceInstances(StepInput stepInput) {
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
                                .map(this::createServiceBinding)
                                .collect(Collectors.toList());
    }

    private CloudServiceBinding createServiceBinding(SimpleApplication boundApplication) {
        return ImmutableCloudServiceBinding.builder()
                                           .applicationGuid(NameUtil.getUUID(boundApplication.name))
                                           .build();
    }

    private void prepareServiceInstance(CloudServiceExtended service, CloudServiceInstance instance) {
        Mockito.when(client.getServiceInstance(service.getName(), false))
               .thenReturn(instance);
    }

    @Override
    protected CheckForCreationConflictsStep createStep() {
        return new CheckForCreationConflictsStep();
    }

    private static class StepInput {
        final List<CloudServiceExtended> servicesToDeploy = Collections.emptyList();
        final List<CloudServiceExtended> existingServices = Collections.emptyList();
        final List<CloudServiceExtended> servicesFromDeployedMta = Collections.emptyList();

        final List<SimpleApplication> appsToDeploy = Collections.emptyList();
        final List<SimpleApplication> existingApps = Collections.emptyList();
        final List<SimpleApplication> appsFromDeployedMta = Collections.emptyList();
    }

    private static final MapToEnvironmentConverter ENV_CONVERTER = new MapToEnvironmentConverter(false);

    private static class SimpleApplication {

        String name;
        final List<String> boundServices = Collections.emptyList();
        final Map<String, Object> env = Collections.emptyMap();
        Metadata metadata;

        CloudApplicationExtended toCloudApplication() {
            return ImmutableCloudApplicationExtended.builder()
                                                    .metadata(ImmutableCloudMetadata.builder()
                                                                                    .guid(NameUtil.getUUID(name))
                                                                                    .build())
                                                    .name(name)
                                                    .env(ENV_CONVERTER.asEnv(env))
                                                    .v3Metadata(metadata)
                                                    .build();
        }
    }

}
