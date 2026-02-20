package org.cloudfoundry.multiapps.controller.process.steps;

import com.fasterxml.jackson.core.type.TypeReference;
import org.cloudfoundry.multiapps.common.test.TestUtil;
import org.cloudfoundry.multiapps.common.test.Tester.Expectation;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.client.facade.CloudCredentials;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceKey;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudMetadata;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudServiceInstance;
import org.cloudfoundry.multiapps.controller.core.cf.clients.CustomServiceKeysClient;
import org.cloudfoundry.multiapps.controller.core.cf.clients.WebClientFactory;
import org.cloudfoundry.multiapps.controller.core.cf.util.ModulesCloudModelBuilderContentCalculator;
import org.cloudfoundry.multiapps.controller.core.cf.v2.ApplicationCloudModelBuilder;
import org.cloudfoundry.multiapps.controller.core.cf.v2.ResourceType;
import org.cloudfoundry.multiapps.controller.core.cf.v2.ServiceKeysCloudModelBuilder;
import org.cloudfoundry.multiapps.controller.core.helpers.ModuleToDeployHelper;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMta;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaServiceKey;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableDeployedMtaServiceKey;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.core.security.token.TokenService;
import org.cloudfoundry.multiapps.controller.core.test.DescriptorTestUtil;
import org.cloudfoundry.multiapps.controller.process.util.DeprecatedBuildpackChecker;
import org.cloudfoundry.multiapps.controller.process.util.ProcessTypeParser;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BuildCloudDeployModelStepTest extends SyncFlowableStepTest<BuildCloudDeployModelStep> {

    private static final Integer MTA_MAJOR_SCHEMA_VERSION = 2;

    private static final String TEST_MTA_ID = "mta-id";
    private static final String TEST_MTA_NAMESPACE = "mta-namespace";
    private static final String TEST_SPACE_GUID = "space-guid";

    private static final String TEST_RESOURCE_NAME = "test-resource-name";
    private static final String TEST_RESOURCE_NAME_2 = "test-resource-name-2";

    private static final DeploymentDescriptor DEPLOYMENT_DESCRIPTOR = DescriptorTestUtil.loadDeploymentDescriptor("build-cloud-model.yaml",
                                                                                                                  BuildCloudDeployModelStepTest.class);

    protected List<Module> modulesToDeploy;
    protected DeployedMta deployedMta;
    protected Map<String, List<CloudServiceKey>> serviceKeys;

    @Mock
    protected ApplicationCloudModelBuilder applicationCloudModelBuilder;
    @Mock
    protected ModulesCloudModelBuilderContentCalculator modulesCloudModelBuilderContentCalculator;
    @Mock
    protected ServiceKeysCloudModelBuilder serviceKeysCloudModelBuilder;
    @Mock
    protected ModuleToDeployHelper moduleToDeployHelper;
    @Mock
    private ProcessTypeParser processTypeParser;
    @Mock
    private TokenService tokenService;
    @Mock
    private WebClientFactory webClientFactory;

    @Mock
    private DeprecatedBuildpackChecker deprecatedBuildpackChecker;

    public static Stream<Arguments> testExecute() {
        return Stream.of(
            // @formatter:off
                Arguments.of(new StepInput("modules-to-deploy-01.json", "services-to-create-01.json", "service-keys-01.json", List.of("api.cf.neo.ondemand.com"), "deployed-mta-12.json")),
                Arguments.of(new StepInput("modules-to-deploy-01.json", "services-to-create-01.json", "service-keys-01.json", List.of("api.cf.neo.ondemand.com"), null))
// @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    void testExecute(StepInput input) {
        loadParameters(input);
        prepareContext();
        step.execute(execution);

        assertStepFinishedSuccessfully();

        tester.test(() -> context.getVariable(Variables.SERVICE_KEYS_TO_CREATE),
                    new Expectation(Expectation.Type.JSON, input.serviceKeysLocation));
        tester.test(() -> context.getVariable(Variables.MODULES_TO_DEPLOY),
                    new Expectation(Expectation.Type.JSON, input.modulesToDeployLocation));
        tester.test(() -> context.getVariable(Variables.ALL_MODULES_TO_DEPLOY),
                    new Expectation(Expectation.Type.JSON, input.modulesToDeployLocation));
        assertFalse(context.getVariable(Variables.USE_IDLE_URIS));
        assertEquals(input.customDomains, context.getVariable(Variables.CUSTOM_DOMAINS));
    }

    protected void loadParameters(StepInput input) {
        String modulesToDeployString = TestUtil.getResourceAsString(input.modulesToDeployLocation, getClass());
        modulesToDeploy = JsonUtil.fromJson(modulesToDeployString, new TypeReference<>() {
        });

        String serviceKeysString = TestUtil.getResourceAsString(input.serviceKeysLocation, getClass());
        serviceKeys = JsonUtil.fromJson(serviceKeysString, new TypeReference<>() {
        });

        if (input.deployedMtaLocation != null) {
            String deployedMtaString = TestUtil.getResourceAsString(input.deployedMtaLocation, getClass());
            deployedMta = JsonUtil.fromJson(deployedMtaString, DeployedMta.class);
        }
        when(moduleToDeployHelper.isApplication(any())).thenReturn(true);
        when(modulesCloudModelBuilderContentCalculator.calculateContentForBuilding(any())).thenReturn(modulesToDeploy);
        when(applicationCloudModelBuilder.getApplicationDomains(any(), any())).thenReturn(input.customDomains);
        when(serviceKeysCloudModelBuilder.build()).thenReturn(serviceKeys);
        context.setVariable(Variables.DEPLOYED_MTA, deployedMta);
    }

    protected void prepareContext() {
        context.setVariable(Variables.MTA_MAJOR_SCHEMA_VERSION, MTA_MAJOR_SCHEMA_VERSION);
        context.setVariable(Variables.MTA_MODULES, Collections.emptySet());
        context.setVariable(Variables.MTA_ARCHIVE_MODULES, Collections.emptySet());
        context.setVariable(Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR, DEPLOYMENT_DESCRIPTOR);
    }

    @Override
    protected BuildCloudDeployModelStep createStep() {
        return new BuildCloudDeployModelStepMock();
    }

    protected static class StepInput {

        public final String servicesToBindLocation;
        public final String deployedMtaLocation;
        public final String serviceKeysLocation;
        public final String modulesToDeployLocation;
        public final List<String> customDomains;

        public StepInput(String modulesToDeployLocation, String servicesToBindLocation, String serviceKeysLocation,
                         List<String> customDomains, String deployedMtaLocation) {
            this.servicesToBindLocation = servicesToBindLocation;
            this.deployedMtaLocation = deployedMtaLocation;
            this.serviceKeysLocation = serviceKeysLocation;
            this.modulesToDeployLocation = modulesToDeployLocation;
            this.customDomains = customDomains;
        }

    }

    private class BuildCloudDeployModelStepMock extends BuildCloudDeployModelStep {
        @Override
        protected ApplicationCloudModelBuilder getApplicationCloudModelBuilder(ProcessContext context) {
            return applicationCloudModelBuilder;
        }

        @Override
        protected ModulesCloudModelBuilderContentCalculator getModulesContentCalculator(ProcessContext context,
                                                                                        List<Module> mtaDescriptorModules,
                                                                                        Set<String> mtaManifestModuleNames,
                                                                                        Set<String> deployedModuleNames,
                                                                                        Set<String> allMtaModuleNames) {
            return modulesCloudModelBuilderContentCalculator;
        }

        @Override
        protected ServiceKeysCloudModelBuilder getServiceKeysCloudModelBuilder(ProcessContext context) {
            return serviceKeysCloudModelBuilder;
        }
    }

    @Test
    void testServiceKeysOfExistingServicesAreAdded() {
        DeploymentDescriptor deploymentDescriptor = createDescriptorWithExistingServicesForKeysTest();
        context.setVariable(Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR, deploymentDescriptor);

        context.setVariable(Variables.MTA_ID, TEST_MTA_ID);
        context.setVariable(Variables.MTA_NAMESPACE, TEST_MTA_NAMESPACE);
        context.setVariable(Variables.SPACE_GUID, TEST_SPACE_GUID);
        context.setVariable(Variables.MTA_MAJOR_SCHEMA_VERSION, 3);

        ProcessContext contextSpy = spy(context);
        doReturn(client).when(contextSpy)
                        .getControllerClient();

        ImmutableCloudServiceInstance instance1 = createCfInstance(TEST_RESOURCE_NAME);
        ImmutableCloudServiceInstance instance2 = createCfInstance(TEST_RESOURCE_NAME_2);

        when(client.getServiceInstance(TEST_RESOURCE_NAME)).thenReturn(instance1);
        when(client.getServiceInstance(TEST_RESOURCE_NAME_2)).thenReturn(instance2);

        CustomServiceKeysClient mockedServiceKeysClient = mock(CustomServiceKeysClient.class);

        ImmutableDeployedMtaServiceKey deployedKey1 = createDeployedKey(TEST_RESOURCE_NAME);
        ImmutableDeployedMtaServiceKey deployedKey2 = createDeployedKey(TEST_RESOURCE_NAME_2);

        when(mockedServiceKeysClient.getServiceKeysByMetadataAndExistingGuids(
            eq(TEST_SPACE_GUID), eq(TEST_MTA_ID), eq(TEST_MTA_NAMESPACE), anyList()
        )).thenReturn(List.of(deployedKey1, deployedKey2));

        BuildCloudDeployModelStep spyStep = spy(step);
        doReturn(mockedServiceKeysClient)
            .when(spyStep)
            .getCustomServiceKeysClient(any(CloudCredentials.class), anyString());

        spyStep.execute(execution);

        List<DeployedMtaServiceKey> all = context.getVariable(Variables.DEPLOYED_MTA_SERVICE_KEYS);

        assertEquals(2, all.size());
        assertTrue(all.contains(deployedKey1));
        assertTrue(all.contains(deployedKey2));

    }

    @Test
    void testServiceKeysOnlyDetectedForResourcesSpecifiedForDeployment() {
        prepareContextForSelectiveDeployment();

        UUID guid1 = UUID.randomUUID();
        UUID guid2 = UUID.randomUUID();
        prepareServiceInstances(guid1, guid2);

        CustomServiceKeysClient mockedServiceKeysClient = mock(CustomServiceKeysClient.class);
        DeployedMtaServiceKey deployedKey1 = createDeployedKey(TEST_RESOURCE_NAME);
        when(mockedServiceKeysClient.getServiceKeysByMetadataAndExistingGuids(
            eq(TEST_SPACE_GUID), eq(TEST_MTA_ID), eq(TEST_MTA_NAMESPACE), anyList()
        )).thenReturn(List.of(deployedKey1));

        BuildCloudDeployModelStep spyStep = prepareStepWithMockedServiceKeysClient(mockedServiceKeysClient);
        spyStep.execute(execution);

        List<String> capturedServiceGuids = captureServiceGuidsPassedToClient(mockedServiceKeysClient);
        assertEquals(1, capturedServiceGuids.size(), "Only the resource specified for deployment should be queried");
        assertEquals(guid1.toString(), capturedServiceGuids.get(0), "The GUID should match the resource specified for deployment");

        List<DeployedMtaServiceKey> detectedServiceKeysFromExistingServices = context.getVariable(Variables.DEPLOYED_MTA_SERVICE_KEYS);
        assertEquals(1, detectedServiceKeysFromExistingServices.size());
        assertTrue(detectedServiceKeysFromExistingServices.contains(deployedKey1));
    }

    private void prepareContextForSelectiveDeployment() {
        DeploymentDescriptor deploymentDescriptor = createDescriptorWithExistingServicesForKeysTest();
        context.setVariable(Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR, deploymentDescriptor);
        context.setVariable(Variables.MTA_ID, TEST_MTA_ID);
        context.setVariable(Variables.MTA_NAMESPACE, TEST_MTA_NAMESPACE);
        context.setVariable(Variables.SPACE_GUID, TEST_SPACE_GUID);
        context.setVariable(Variables.MTA_MAJOR_SCHEMA_VERSION, 3);
        context.setVariable(Variables.RESOURCES_FOR_DEPLOYMENT, List.of(TEST_RESOURCE_NAME));
    }

    private void prepareServiceInstances(UUID guid1, UUID guid2) {
        var instance1 = ImmutableCloudServiceInstance.builder()
                                                     .name(TEST_RESOURCE_NAME)
                                                     .metadata(ImmutableCloudMetadata.of(guid1))
                                                     .build();
        var instance2 = ImmutableCloudServiceInstance.builder()
                                                     .name(TEST_RESOURCE_NAME_2)
                                                     .metadata(ImmutableCloudMetadata.of(guid2))
                                                     .build();
        when(client.getServiceInstance(TEST_RESOURCE_NAME)).thenReturn(instance1);
        when(client.getServiceInstance(TEST_RESOURCE_NAME_2)).thenReturn(instance2);
    }

    private BuildCloudDeployModelStep prepareStepWithMockedServiceKeysClient(CustomServiceKeysClient mockedServiceKeysClient) {
        BuildCloudDeployModelStep spyStep = spy(step);
        doReturn(mockedServiceKeysClient)
            .when(spyStep)
            .getCustomServiceKeysClient(any(CloudCredentials.class), anyString());
        return spyStep;
    }

    @SuppressWarnings("unchecked")
    private List<String> captureServiceGuidsPassedToClient(CustomServiceKeysClient mockedServiceKeysClient) {
        ArgumentCaptor<List<String>> guidsCaptor = ArgumentCaptor.forClass(List.class);
        verify(mockedServiceKeysClient).getServiceKeysByMetadataAndExistingGuids(
            eq(TEST_SPACE_GUID), eq(TEST_MTA_ID), eq(TEST_MTA_NAMESPACE), guidsCaptor.capture()
        );
        return guidsCaptor.getValue();
    }

    private DeploymentDescriptor createDescriptorWithExistingServicesForKeysTest() {
        Resource resource1 = createExistingServiceResource(TEST_RESOURCE_NAME);
        Resource resource2 = createExistingServiceResource(TEST_RESOURCE_NAME_2);

        DeploymentDescriptor descriptor = DeploymentDescriptor.createV3();
        descriptor.setResources(List.of(resource1, resource2));

        return descriptor;
    }

    private Resource createExistingServiceResource(String name) {
        Resource resource = Resource.createV3();
        resource.setName(name);

        resource.setParameters(Map.of(
            SupportedParameters.TYPE, ResourceType.EXISTING_SERVICE.toString(),
            SupportedParameters.SERVICE_NAME, name
        ));
        return resource;
    }

    private ImmutableDeployedMtaServiceKey createDeployedKey(String resourceName) {
        return ImmutableDeployedMtaServiceKey.builder()
                                             .resourceName(resourceName)
                                             .metadata(ImmutableCloudMetadata.of(UUID.randomUUID()))
                                             .build();
    }

    private ImmutableCloudServiceInstance createCfInstance(String name) {
        return ImmutableCloudServiceInstance.builder()
                                            .name(name)
                                            .metadata(ImmutableCloudMetadata.of(UUID.randomUUID()))
                                            .build();
    }
}
