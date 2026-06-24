package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudMetadata;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableInstanceInfo;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableInstancesInfo;
import org.cloudfoundry.multiapps.controller.client.facade.domain.InstanceInfo;
import org.cloudfoundry.multiapps.controller.client.facade.domain.InstanceState;
import org.cloudfoundry.multiapps.controller.client.facade.domain.InstancesInfo;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientFactory;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.ImmutableMtaMetadata;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMta;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaApplication;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableDeployedMta;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableDeployedMtaApplication;
import org.cloudfoundry.multiapps.controller.core.model.IncrementalAppInstanceUpdateConfiguration;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.core.security.token.TokenService;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import static org.cloudfoundry.multiapps.controller.process.steps.StepsTestUtil.prepareDisablingAutoscaler;
import static org.cloudfoundry.multiapps.controller.process.steps.StepsTestUtil.testIfEnabledOrDisabledAutoscaler;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IncrementalAppInstanceUpdateStepTest extends SyncFlowableStepTest<IncrementalAppInstancesUpdateStep> {

    private static final UUID MTA_GUID = UUID.randomUUID();
    private static final UUID LIVE_APP_GUID = UUID.randomUUID();
    private static final String APP_TO_PROCESS_NAME = "app-to-process-idle";
    private static final UUID APP_TO_PROCESS_GUID = UUID.randomUUID();
    private static final String MODULE_NAME = "app-to-process-module";
    private static final String LIVE_APPLICATION_NAME = "app-to-process-live";

    private CloudControllerClientFactory clientFactory;
    private TokenService tokenService;

    @BeforeEach
    public void setUp() {
        context.setVariable(Variables.DEPLOYMENT_DESCRIPTOR, descriptor);
    }

    @Test
    void executeStepWithMissingOldApplication() {
        prepareAppToProcess(5);
        step.execute(execution);
        verify(client).updateApplicationInstances(APP_TO_PROCESS_NAME, 5);
        IncrementalAppInstanceUpdateConfiguration incrementalAppInstanceUpdateConfiguration = context.getVariable(
            Variables.INCREMENTAL_APP_INSTANCE_UPDATE_CONFIGURATION);
        assertEquals(5, incrementalAppInstanceUpdateConfiguration.getNewApplicationInstanceCount());
        assertEquals(1, context.getVariable(Variables.ASYNC_STEP_EXECUTION_INDEX));
        assertExecutionStepStatus(StepPhase.POLL.toString());
    }

    private void prepareAppToProcess(int instancesCount) {
        CloudApplicationExtended cloudApplication = buildAppToProcessApplication(instancesCount);
        when(client.getApplicationGuid(APP_TO_PROCESS_NAME)).thenReturn(APP_TO_PROCESS_GUID);
        InstancesInfo instancesInfo = buildInstancesInfo(InstanceState.RUNNING);
        when(client.getApplicationInstances(APP_TO_PROCESS_GUID)).thenReturn(instancesInfo);
        context.setVariable(Variables.APP_TO_PROCESS, cloudApplication);
        when(client.getApplicationGuid(cloudApplication.getName())).thenReturn(APP_TO_PROCESS_GUID);
    }

    private CloudApplicationExtended buildAppToProcessApplication(int instancesCount) {
        return ImmutableCloudApplicationExtended.builder()
                                                .name(APP_TO_PROCESS_NAME)
                                                .instances(instancesCount)
                                                .moduleName(MODULE_NAME)
                                                .metadata(ImmutableCloudMetadata.builder()
                                                                                .guid(APP_TO_PROCESS_GUID)
                                                                                .build())
                                                .build();
    }

    @Test
    void executeStepWithRunningOldApplication() {
        prepareRunningOldApplication();
        prepareAppToProcess(5);
        step.execute(execution);
        IncrementalAppInstanceUpdateConfiguration incrementalAppInstanceUpdateConfiguration = context.getVariable(
            Variables.INCREMENTAL_APP_INSTANCE_UPDATE_CONFIGURATION);
        assertEquals(1, incrementalAppInstanceUpdateConfiguration.getNewApplicationInstanceCount());
        assertEquals(2, context.getVariable(Variables.ASYNC_STEP_EXECUTION_INDEX));
        assertEquals(1, incrementalAppInstanceUpdateConfiguration.getOldApplicationInstanceCount());
        assertEquals(1, incrementalAppInstanceUpdateConfiguration.getOldApplicationInitialInstanceCount());
        assertExecutionStepStatus(StepPhase.POLL.toString());
        testIfEnabledOrDisabledAutoscaler(client, MessageFormat.format(Messages.DISABLE_AUTOSCALER_LABEL_CONTENT, ""), APP_TO_PROCESS_GUID);
    }

    private void prepareRunningOldApplication() {
        DeployedMtaApplication deployedMtaApplication = buildDeployedMtaApplication(LIVE_APPLICATION_NAME);
        DeployedMta deployedMta = buildDeployedMta(deployedMtaApplication);
        InstancesInfo instancesInfo = buildInstancesInfo(InstanceState.RUNNING);
        context.setVariable(Variables.DEPLOYED_MTA, deployedMta);
        when(client.getApplicationInstances(deployedMtaApplication)).thenReturn(instancesInfo);
        prepareDisablingAutoscaler(context, client, deployedMtaApplication, APP_TO_PROCESS_GUID);
    }

    private InstancesInfo buildInstancesInfo(InstanceState state) {
        List<InstanceInfo> instances = List.of(ImmutableInstanceInfo.builder()
                                                                    .state(state)
                                                                    .index(0)
                                                                    .build());
        return ImmutableInstancesInfo.builder()
                                     .instances(instances)
                                     .build();
    }

    private DeployedMta buildDeployedMta(DeployedMtaApplication deployedMtaApplication) {
        return ImmutableDeployedMta.builder()
                                   .metadata(ImmutableMtaMetadata.builder()
                                                                 .id(MTA_GUID.toString())
                                                                 .build())
                                   .applications(List.of(deployedMtaApplication))
                                   .build();
    }

    @Test
    void executeStepWithFailingOldApplication() {
        prepareFailingOldApplication();
        prepareAppToProcess(5);
        step.execute(execution);
        IncrementalAppInstanceUpdateConfiguration incrementalAppInstanceUpdateConfiguration = context.getVariable(
            Variables.INCREMENTAL_APP_INSTANCE_UPDATE_CONFIGURATION);
        assertEquals(1, incrementalAppInstanceUpdateConfiguration.getNewApplicationInstanceCount());
        assertEquals(0, context.getVariable(Variables.ASYNC_STEP_EXECUTION_INDEX));
        assertEquals(1, incrementalAppInstanceUpdateConfiguration.getOldApplicationInstanceCount());
        assertEquals(1, incrementalAppInstanceUpdateConfiguration.getOldApplicationInitialInstanceCount());
        assertExecutionStepStatus(StepPhase.POLL.toString());
        testIfEnabledOrDisabledAutoscaler(client, MessageFormat.format(Messages.DISABLE_AUTOSCALER_LABEL_CONTENT, ""), APP_TO_PROCESS_GUID);
    }

    private void prepareFailingOldApplication() {
        DeployedMtaApplication deployedMtaApplication = buildDeployedMtaApplication(LIVE_APPLICATION_NAME);
        DeployedMta deployedMta = buildDeployedMta(deployedMtaApplication);
        InstancesInfo instancesInfo = buildInstancesInfo(InstanceState.CRASHED);
        context.setVariable(Variables.DEPLOYED_MTA, deployedMta);
        when(client.getApplicationInstances(deployedMtaApplication)).thenReturn(instancesInfo);
        prepareDisablingAutoscaler(context, client, deployedMtaApplication, APP_TO_PROCESS_GUID);
    }

    @Test
    void executeStepWithAlreadyScaledApplication() {
        prepareFailingOldApplication();
        prepareAppToProcess(1);
        step.execute(execution);
        IncrementalAppInstanceUpdateConfiguration incrementalAppInstanceUpdateConfiguration = context.getVariable(
            Variables.INCREMENTAL_APP_INSTANCE_UPDATE_CONFIGURATION);
        assertEquals(1, incrementalAppInstanceUpdateConfiguration.getNewApplicationInstanceCount());
        assertEquals(0, context.getVariable(Variables.ASYNC_STEP_EXECUTION_INDEX));
        assertEquals(1, incrementalAppInstanceUpdateConfiguration.getOldApplicationInstanceCount());
        assertEquals(1, incrementalAppInstanceUpdateConfiguration.getOldApplicationInitialInstanceCount());
        assertExecutionStepStatus(StepPhase.DONE.toString());
        testIfEnabledOrDisabledAutoscaler(client, MessageFormat.format(Messages.DISABLE_AUTOSCALER_LABEL_CONTENT, ""), APP_TO_PROCESS_GUID);
        testIfEnabledOrDisabledAutoscaler(client, null, APP_TO_PROCESS_GUID);
    }

    @Test
    void executeStepWithoutOldApplicationsDetected() {
        prepareDeployedMtaWithoutApplications();
        prepareAppToProcess(5);
        step.execute(execution);
        IncrementalAppInstanceUpdateConfiguration incrementalAppInstanceUpdateConfiguration = context.getVariable(
            Variables.INCREMENTAL_APP_INSTANCE_UPDATE_CONFIGURATION);
        assertEquals(5, incrementalAppInstanceUpdateConfiguration.getNewApplicationInstanceCount());
        assertEquals(1, context.getVariable(Variables.ASYNC_STEP_EXECUTION_INDEX));
        assertExecutionStepStatus(StepPhase.POLL.toString());
    }

    private void prepareDeployedMtaWithoutApplications() {
        DeployedMta deployedMta = ImmutableDeployedMta.builder()
                                                      .metadata(ImmutableMtaMetadata.builder()
                                                                                    .id(MTA_GUID.toString())
                                                                                    .build())
                                                      .build();
        context.setVariable(Variables.DEPLOYED_MTA, deployedMta);
    }

    @Test
    void executeStepWithOtherOldApplicationsDetected() {
        DeployedMtaApplication deployedMtaApplication = buildDeployedMtaApplication(APP_TO_PROCESS_NAME);
        DeployedMta deployedMta = ImmutableDeployedMta.builder()
                                                      .metadata(ImmutableMtaMetadata.builder()
                                                                                    .id(MTA_GUID.toString())
                                                                                    .build())
                                                      .applications(List.of(deployedMtaApplication))
                                                      .build();
        InstancesInfo instancesInfo = buildInstancesInfo(InstanceState.RUNNING);
        when(client.getApplicationInstances(deployedMtaApplication)).thenReturn(instancesInfo);
        context.setVariable(Variables.DEPLOYED_MTA, deployedMta);
        prepareAppToProcess(5);
        step.execute(execution);
        IncrementalAppInstanceUpdateConfiguration incrementalAppInstanceUpdateConfiguration = context.getVariable(
            Variables.INCREMENTAL_APP_INSTANCE_UPDATE_CONFIGURATION);
        assertEquals(5, incrementalAppInstanceUpdateConfiguration.getNewApplicationInstanceCount());
        assertEquals(1, context.getVariable(Variables.ASYNC_STEP_EXECUTION_INDEX));
        assertNull(incrementalAppInstanceUpdateConfiguration.getOldApplication());
        assertExecutionStepStatus(StepPhase.POLL.toString());
    }

    private DeployedMtaApplication buildDeployedMtaApplication(String name) {
        return ImmutableDeployedMtaApplication.builder()
                                              .metadata(ImmutableCloudMetadata.builder()
                                                                              .guid(LIVE_APP_GUID)
                                                                              .build())
                                              .moduleName(MODULE_NAME)
                                              .name(name)
                                              .build();
    }

    @ParameterizedTest
    @MethodSource("testValidatePriority")
    void testGetTimeout(Integer timeoutProcessVariable, Integer timeoutModuleLevel, Integer timeoutGlobalLevel, int expectedTimeout) {
        step.initializeStepLogger(execution);
        setUpContext(timeoutProcessVariable, timeoutModuleLevel, timeoutGlobalLevel, Variables.APPS_START_TIMEOUT_PROCESS_VARIABLE,
                     SupportedParameters.START_TIMEOUT, SupportedParameters.APPS_START_TIMEOUT);

        Duration actualTimeout = step.getTimeout(context);
        assertEquals(Duration.ofSeconds(expectedTimeout * 9L), actualTimeout);
    }

    @Test
    void testGetTimeout() {
        step.initializeStepLogger(execution);
        setUpContext(null, null, 3 * 3600, Variables.APPS_START_TIMEOUT_PROCESS_VARIABLE, SupportedParameters.START_TIMEOUT,
                     SupportedParameters.APPS_START_TIMEOUT);

        Duration actualTimeout = step.getTimeout(context);
        assertEquals(Duration.ofSeconds(24 * 3600), actualTimeout);
    }

    @Test
    void testGetTimeoutWithZeroInstance() {
        step.initializeStepLogger(execution);
        var app = ImmutableCloudApplicationExtended.builder()
                                                   .name("app-zero-instances")
                                                   .instances(0)
                                                   .build();
        context.setVariable(Variables.APP_TO_PROCESS, app);
        Duration actualTimeout = step.getTimeout(context);
        assertEquals(Duration.ofSeconds(3600), actualTimeout);
    }

    @Override
    protected IncrementalAppInstancesUpdateStep createStep() {
        clientFactory = Mockito.mock(CloudControllerClientFactory.class);
        tokenService = Mockito.mock(TokenService.class);
        return new IncrementalAppInstancesUpdateStep(clientFactory, tokenService);
    }
}
