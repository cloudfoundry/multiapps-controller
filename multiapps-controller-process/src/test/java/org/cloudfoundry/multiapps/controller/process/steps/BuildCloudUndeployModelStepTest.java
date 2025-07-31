package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.type.TypeReference;
import org.cloudfoundry.multiapps.common.test.TestUtil;
import org.cloudfoundry.multiapps.common.test.Tester;
import org.cloudfoundry.multiapps.common.test.Tester.Expectation;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.api.model.ProcessType;
import org.cloudfoundry.multiapps.controller.client.facade.CloudControllerClient;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudApplication;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudEntity;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.cf.v2.ApplicationCloudModelBuilder;
import org.cloudfoundry.multiapps.controller.core.helpers.ModuleToDeployHelper;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMta;
import org.cloudfoundry.multiapps.controller.core.test.MockBuilder;
import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationSubscription;
import org.cloudfoundry.multiapps.controller.persistence.query.ConfigurationSubscriptionQuery;
import org.cloudfoundry.multiapps.controller.persistence.services.ConfigurationSubscriptionService;
import org.cloudfoundry.multiapps.controller.process.util.ProcessTypeParser;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class BuildCloudUndeployModelStepTest extends SyncFlowableStepTest<BuildCloudUndeployModelStep> {

    private static final String SPACE_ID = "sap";

    private final Tester tester = Tester.forClass(getClass());

    public static Stream<Arguments> testExecute() {
        return Stream.of(
            // @formatter:off
            // (1) No previously deployed MTA:
            Arguments.of(new StepInput(Collections.emptyList(), Collections.emptyList(), Set.of("a", "b", "c"), null, "empty-list.json", "empty-list.json", Set.of("a", "b", "c")),
                    new StepOutput(Collections.emptyList(), Collections.emptyList(), new Expectation(Expectation.Type.JSON, "empty-list.json"))),
            // (2) There are obsolete modules:
            Arguments.of(new StepInput(Collections.emptyList(), List.of("a", "b", "c", "d", "e"), Set.of("a", "b", "c"), "deployed-mta-03.json", "empty-list.json", "empty-list.json", Set.of("a", "b", "c", "d", "e")),
                    new StepOutput(List.of("d", "e"), Collections.emptyList(), new Expectation(Expectation.Type.JSON, "empty-list.json"))),
            // (3) All modules are obsolete:
            Arguments.of(new StepInput(Collections.emptyList(), List.of("d", "e"), Collections.emptySet(), "deployed-mta-04.json", "empty-list.json", "empty-list.json", Set.of("a", "b", "c", "d", "e")),
                    new StepOutput(List.of("d", "e"), Collections.emptyList(), new Expectation(Expectation.Type.JSON, "empty-list.json"))),
            // (4) There are obsolete services:
            Arguments.of(new StepInput(List.of("s-1", "s-2"), List.of("a", "b", "c", "d", "e"), Set.of("a", "b", "c", "d", "e"), "deployed-mta-05.json", "empty-list.json", "empty-list.json", Set.of("a", "b", "c", "d", "e")),
                    new StepOutput(Collections.emptyList(), List.of("s-3", "s-4"), new Expectation(Expectation.Type.JSON, "empty-list.json"))),
            // (5) All services are obsolete:
            Arguments.of(new StepInput(List.of("s-3", "s-4"), List.of("d", "e"), Set.of("a", "b", "c", "d", "e"), "deployed-mta-06.json", "empty-list.json", "empty-list.json", Set.of("a", "b", "c", "d", "e")),
                    new StepOutput(Collections.emptyList(), List.of("s-1", "s-2"), new Expectation(Expectation.Type.JSON, "empty-list.json"))),
            // (6) There are renamed applications:
            Arguments.of(new StepInput(Collections.emptyList(),List.of("namespace.a", "namespace.b", "namespace.c"), Set.of("a", "b", "c"), "deployed-mta-07.json", "empty-list.json", "empty-list.json", Set.of("a", "b", "c")),
                    new StepOutput(List.of("namespace.a", "namespace.b", "namespace.c"), Collections.emptyList(), new Expectation(Expectation.Type.JSON, "empty-list.json"))),
            // (7) There are no obsolete services:
            Arguments.of(new StepInput(List.of("s-1", "s-2", "s-3"), List.of("a", "b", "c", "d", "e"), Set.of("a", "b", "c", "d", "e"), "deployed-mta-08.json", "empty-list.json", "empty-list.json", Set.of("a", "b", "c", "d", "e")),
                    new StepOutput(Collections.emptyList(), Collections.emptyList(), new Expectation(Expectation.Type.JSON, "empty-list.json"))),
            // (8) There are no obsolete modules:
            Arguments.of(new StepInput(Collections.emptyList(), List.of("a", "b"), Set.of("a", "b", "c"), "deployed-mta-09.json", "empty-list.json", "empty-list.json", Set.of("a", "b", "c")),
                    new StepOutput(Collections.emptyList(), Collections.emptyList(), new Expectation(Expectation.Type.JSON, "empty-list.json"))),
            // (9) There are no obsolete published provided dependencies:
            Arguments.of(new StepInput(Collections.emptyList(), List.of("a", "b", "c", "d", "e"), Set.of("a", "b", "c", "d", "e"), "deployed-mta-10.json", "empty-list.json", "empty-list.json", Set.of("a", "b", "c", "d", "e")),
                    new StepOutput(Collections.emptyList(), Collections.emptyList(), new Expectation(Expectation.Type.JSON, "empty-list.json"))),
            // (10) There are no obsolete subscriptions:
            Arguments.of(new StepInput(Collections.emptyList(), List.of("a", "b", "c", "d", "e"), Set.of("a", "b", "c", "d", "e"), "deployed-mta-03.json", "subscriptions-to-create-01.json","empty-list.json", Set.of("a", "b", "c", "d", "e")),
                    new StepOutput(Collections.emptyList(), Collections.emptyList(), new Expectation(Expectation.Type.JSON, "empty-list.json"))),
            // (11) There are obsolete subscriptions:
            Arguments.of(new StepInput(Collections.emptyList(), List.of("a", "b", "c", "d", "e"), Set.of("a", "b", "c", "d", "e"), "deployed-mta-03.json", "subscriptions-to-create-01.json", "existing-subscriptions-01.json", Set.of("a", "b", "c", "d", "e")),
                    new StepOutput(Collections.emptyList(), Collections.emptyList(), new Expectation(Expectation.Type.JSON, "subscriptions-to-delete-01.json"))),
            // (12) There are obsolete subscriptions:
            Arguments.of(new StepInput(Collections.emptyList(), List.of("a", "b", "c", "d", "e"), Set.of("a", "b", "c", "d", "e"), "deployed-mta-03.json", "empty-list.json",  "existing-subscriptions-01.json", Set.of("a", "b", "c", "d", "e")),
                    new StepOutput(Collections.emptyList(), Collections.emptyList(), new Expectation(Expectation.Type.JSON, "subscriptions-to-delete-02.json"))),
            // (13) There are obsolete services because they are all unbound
            Arguments.of(new StepInput(Collections.emptyList(), List.of("a", "b", "c", "d", "e"), Set.of("a", "b", "c", "d", "e"), "deployed-mta-08.json", "empty-list.json", "empty-list.json", Set.of("a", "b", "c", "d", "e")),
                    new StepOutput(Collections.emptyList(), List.of("s-1", "s-2", "s-3"), new Expectation(Expectation.Type.JSON, "empty-list.json"))),
            // (14) There are modules with skip-deploy parameter set
            Arguments.of(
                new StepInput(Collections.emptyList(), List.of("a", "b", "c"), Set.of("a", "c"), "deployed-mta-13.json", "empty-list.json", "empty-list.json", Set.of("a", "b", "c")),
                new StepOutput(List.of("b"), Collections.emptyList(), new Expectation(Expectation.Type.JSON, "empty-list.json"))
            )
            // @formatter:on
        );
    }

    @Mock
    private ConfigurationSubscriptionService configurationSubscriptionService;
    @Mock(answer = Answers.RETURNS_SELF)
    private ConfigurationSubscriptionQuery configurationSubscriptionQuery;
    @Mock
    protected ModuleToDeployHelper moduleToDeployHelper;
    @Mock
    protected ApplicationCloudModelBuilder applicationCloudModelBuilder;
    @Mock
    private CloudControllerClient client;
    @Mock
    private ProcessTypeParser processTypeParser;

    protected List<Module> modulesToDeploy;
    private List<CloudApplicationExtended> deployedApps;
    private DeployedMta deployedMta;
    private List<ConfigurationSubscription> subscriptionsToCreate;
    private List<ConfigurationSubscription> existingSubscriptions;
    private DeploymentDescriptor deploymentDescriptor;

    private void prepareClient() {
        for (CloudApplicationExtended application : deployedApps) {
            Mockito.when(client.getApplication(application.getName(), false))
                   .thenReturn(application);
        }
        Mockito.when(clientProvider.getControllerClient(anyString(), anyString(), anyString(), anyString()))
               .thenReturn(client);
    }

    private void prepareDeploymentDescriptor(StepInput input) {
        List<Module> modules = input.deploymentDescriptorModules.stream()
                                                                .map(this::getModuleFromName)
                                                                .collect(Collectors.toList());
        deploymentDescriptor = DeploymentDescriptor.createV3()
                                                   .setModules(modules)
                                                   .setSchemaVersion("3")
                                                   .setId("id")
                                                   .setVersion("1");
    }

    private Module getModuleFromName(String moduleName) {
        return Module.createV3()
                     .setName(moduleName)
                     .setType("a");
    }

    private void loadParameters(StepInput input) {
        String subscriptionsToCreateString = TestUtil.getResourceAsString(input.subscriptionsToCreateLocation, getClass());
        String existingSubscriptionsString = TestUtil.getResourceAsString(input.existingSubscriptionsLocation, getClass());

        modulesToDeploy = input.mtaModules.stream()
                                          .map(this::getModuleFromName)
                                          .toList();
        deployedApps = input.deployedAppNames.stream()
                                             .map(deployedAppName -> ImmutableCloudApplicationExtended.builder()
                                                                                                      .name(deployedAppName)
                                                                                                      .build())
                                             .collect(Collectors.toList());

        subscriptionsToCreate = JsonUtil.fromJson(subscriptionsToCreateString, new TypeReference<>() {
        });
        existingSubscriptions = JsonUtil.fromJson(existingSubscriptionsString, new TypeReference<>() {
        });

        if (input.deployedMtaLocation != null) {
            String deployedMtaString = TestUtil.getResourceAsString(input.deployedMtaLocation, getClass());
            deployedMta = JsonUtil.fromJson(deployedMtaString, DeployedMta.class);
        }
        when(moduleToDeployHelper.isApplication(any())).thenReturn(true);
        when(moduleToDeployHelper.shouldSkipDeploy(any(Module.class))).thenAnswer(invocationModule -> {
            Module module = (Module) invocationModule.getArgument(0);
            return !input.mtaModules.contains(module.getName());
        });

        when(applicationCloudModelBuilder.getAllApplicationServices(any())).thenReturn(input.services);

    }

    private void prepareContext(StepInput input) {
        context.setVariable(Variables.DEPLOYED_MTA, deployedMta);
        context.setVariable(Variables.MODULES_TO_DEPLOY, modulesToDeploy);
        context.setVariable(Variables.APPS_TO_DEPLOY, input.mtaModules.stream()
                                                                      .toList());
        context.setVariable(Variables.SUBSCRIPTIONS_TO_CREATE, subscriptionsToCreate);
        context.setVariable(Variables.SPACE_GUID, SPACE_ID);
        context.setVariable(Variables.MTA_MODULES, input.mtaModules);
        context.setVariable(Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR, deploymentDescriptor);
    }

    private void prepareSubscriptionService() {
        when(configurationSubscriptionService.createQuery()).thenReturn(configurationSubscriptionQuery);
        if (deployedMta != null) {
            ConfigurationSubscriptionQuery mock = new MockBuilder<>(configurationSubscriptionQuery).on(query -> query.mtaId(
                                                                                                       deployedMta.getMetadata()
                                                                                                                  .getId()))
                                                                                                   .on(query -> query.spaceId(SPACE_ID))
                                                                                                   .build();
            when(mock.list()).thenReturn(filter(existingSubscriptions));
        }
    }

    private List<ConfigurationSubscription> filter(List<ConfigurationSubscription> existingSubscriptions) {
        return existingSubscriptions.stream()
                                    .filter(subscription -> SPACE_ID.equals(subscription.getSpaceId()))
                                    .collect(Collectors.toList());
    }

    @ParameterizedTest
    @MethodSource
    void testExecute(StepInput input, StepOutput output) {
        initialize(input);
        step.execute(execution);

        assertStepFinishedSuccessfully();

        assertEquals(output.servicesToDelete, context.getVariable(Variables.SERVICES_TO_DELETE));

        assertEquals(output.appsToUndeployNames, getNames(context.getVariable(Variables.APPS_TO_UNDEPLOY)));

        tester.test(() -> context.getVariable(Variables.SUBSCRIPTIONS_TO_DELETE), output.subscriptionsToDeleteExpectation);
    }

    private void initialize(StepInput input) {
        loadParameters(input);
        prepareClient();
        prepareDeploymentDescriptor(input);
        prepareContext(input);
        prepareSubscriptionService();
        when(processTypeParser.getProcessType(Mockito.eq(context.getExecution()))).thenReturn(ProcessType.DEPLOY);
    }

    private List<String> getNames(List<CloudApplication> appsToUndeploy) {
        if (appsToUndeploy == null) {
            return null;
        }
        return appsToUndeploy.stream()
                             .map(CloudEntity::getName)
                             .collect(Collectors.toList());
    }

    @Override
    protected BuildCloudUndeployModelStepMock createStep() {
        return new BuildCloudUndeployModelStepMock();
    }

    private static class StepInput {

        public final List<String> services;
        public final List<String> deployedAppNames;
        public final Set<String> mtaModules;
        public final String deployedMtaLocation;
        public final String subscriptionsToCreateLocation;
        public final String existingSubscriptionsLocation;
        public final Set<String> deploymentDescriptorModules;

        public StepInput(List<String> services, List<String> deployedAppNames, Set<String> mtaModules, String deployedMtaLocation,
                         String subscriptionsToCreateLocation, String existingSubscriptionsLocation,
                         Set<String> deploymentDescriptorModules) {
            this.services = services;
            this.deployedAppNames = deployedAppNames;
            this.mtaModules = mtaModules;
            this.deployedMtaLocation = deployedMtaLocation;
            this.subscriptionsToCreateLocation = subscriptionsToCreateLocation;
            this.existingSubscriptionsLocation = existingSubscriptionsLocation;
            this.deploymentDescriptorModules = deploymentDescriptorModules;
        }

    }

    private static class StepOutput {

        public final List<String> appsToUndeployNames;
        public final List<String> servicesToDelete;
        public final Expectation subscriptionsToDeleteExpectation;

        public StepOutput(List<String> appsToUndeployNames, List<String> servicesToDelete, Expectation subscriptionsToDeleteExpectation) {
            this.appsToUndeployNames = appsToUndeployNames;
            this.servicesToDelete = servicesToDelete;
            this.subscriptionsToDeleteExpectation = subscriptionsToDeleteExpectation;
        }

    }

    private class BuildCloudUndeployModelStepMock extends BuildCloudUndeployModelStep {
        @Override
        protected ApplicationCloudModelBuilder getApplicationCloudModelBuilder(ProcessContext context) {
            return applicationCloudModelBuilder;
        }
    }
}
