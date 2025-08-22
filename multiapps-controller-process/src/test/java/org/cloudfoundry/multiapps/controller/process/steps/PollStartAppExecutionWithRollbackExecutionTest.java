package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.List;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudApplication;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudMetadata;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableInstanceInfo;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableInstancesInfo;
import org.cloudfoundry.multiapps.controller.client.facade.domain.InstanceInfo;
import org.cloudfoundry.multiapps.controller.client.facade.domain.InstanceState;
import org.cloudfoundry.multiapps.controller.client.facade.domain.InstancesInfo;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientFactory;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaApplication;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableDeployedMtaApplication;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableIncrementalAppInstanceUpdateConfiguration;
import org.cloudfoundry.multiapps.controller.core.model.IncrementalAppInstanceUpdateConfiguration;
import org.cloudfoundry.multiapps.controller.core.security.token.TokenService;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.cloudfoundry.multiapps.controller.process.steps.StepsTestUtil.testIfEnabledOrDisabledAutoscaler;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PollStartAppExecutionWithRollbackExecutionTest extends AsyncStepOperationTest<IncrementalAppInstancesUpdateStep> {

    private static final String LIVE_APP_NAME = "app-to-process-live";
    private static final UUID LIVE_APP_GUID = UUID.randomUUID();
    private static final String MODULE_NAME = "app-to-process-module";
    private static final String APP_TO_PROCESS_NAME = "app-to-process-idle";
    private static final UUID APP_TO_PROCESS_GUID = UUID.randomUUID();

    private CloudControllerClientFactory clientFactory;
    private TokenService tokenService;

    private AsyncExecutionState expectedAsyncExecutionState;

    @Test
    void testRollbackWhenOldAppDoesNotExist() {
        CloudApplicationExtended cloudApplicationExtended = buildCloudApplication(3);
        prepareAppToProcess(cloudApplicationExtended);
        context.setVariable(Variables.INCREMENTAL_APP_INSTANCE_UPDATE_CONFIGURATION, buildConfigWithoutOldApp(cloudApplicationExtended));
        expectedAsyncExecutionState = AsyncExecutionState.ERROR;
        testExecuteOperations();
        verify(client).updateApplicationInstances(APP_TO_PROCESS_NAME, 1);
    }

    private CloudApplicationExtended buildCloudApplication(int instances) {
        return ImmutableCloudApplicationExtended.builder()
                                                .name(APP_TO_PROCESS_NAME)
                                                .instances(instances)
                                                .metadata(ImmutableCloudMetadata.builder()
                                                                                .guid(APP_TO_PROCESS_GUID)
                                                                                .build())
                                                .build();
    }

    private void prepareAppToProcess(CloudApplicationExtended cloudApplicationExtended) {
        InstanceInfo instanceInfo = ImmutableInstanceInfo.builder()
                                                         .index(0)
                                                         .state(InstanceState.CRASHED)
                                                         .isRoutable(true)
                                                         .build();
        InstancesInfo instancesInfo = ImmutableInstancesInfo.builder()
                                                            .instances(List.of(instanceInfo))
                                                            .build();
        when(client.getApplicationInstances(any(CloudApplication.class))).thenReturn(instancesInfo);
        context.setVariable(Variables.EXISTING_APP_TO_POLL, cloudApplicationExtended);
        context.setVariable(Variables.APP_TO_PROCESS, cloudApplicationExtended);
        when(client.getApplicationGuid(LIVE_APP_NAME)).thenReturn(LIVE_APP_GUID);
    }

    private IncrementalAppInstanceUpdateConfiguration buildConfigWithoutOldApp(CloudApplicationExtended cloudApplicationExtended) {
        return ImmutableIncrementalAppInstanceUpdateConfiguration.builder()
                                                                 .newApplication(cloudApplicationExtended)
                                                                 .newApplicationInstanceCount(5)
                                                                 .build();
    }

    @Test
    void testRollbackWhenOldApplicationWasDownscaled() {
        CloudApplicationExtended cloudApplicationExtended = buildCloudApplication(3);
        prepareAppToProcess(cloudApplicationExtended);
        context.setVariable(Variables.INCREMENTAL_APP_INSTANCE_UPDATE_CONFIGURATION,
                            ImmutableIncrementalAppInstanceUpdateConfiguration.builder()
                                                                              .newApplication(cloudApplicationExtended)
                                                                              .newApplicationInstanceCount(5)
                                                                              .oldApplication(buildDeployedMtaApplication())
                                                                              .oldApplicationInitialInstanceCount(10)
                                                                              .oldApplicationInstanceCount(3)
                                                                              .build());
        expectedAsyncExecutionState = AsyncExecutionState.ERROR;
        testExecuteOperations();
        verify(client).updateApplicationInstances(APP_TO_PROCESS_NAME, 1);
        verify(client).updateApplicationInstances(LIVE_APP_NAME, 10);
        testIfEnabledOrDisabledAutoscaler(client, null, LIVE_APP_GUID);
    }

    private DeployedMtaApplication buildDeployedMtaApplication() {
        return ImmutableDeployedMtaApplication.builder()
                                              .metadata(ImmutableCloudMetadata.builder()
                                                                              .guid(LIVE_APP_GUID)
                                                                              .build())
                                              .moduleName(MODULE_NAME)
                                              .name(LIVE_APP_NAME)
                                              .build();
    }

    @Test
    void testOnSuccessWhenNewAppStillNotStarted() {
        CloudApplicationExtended cloudApplicationExtended = buildCloudApplication(3);
        prepareAppToProcess(cloudApplicationExtended);
        executeOnSuccess(cloudApplicationExtended);
        verify(stepLogger, never()).info(any(), any());
    }

    private IncrementalAppInstanceUpdateConfiguration buildUpdateConfig(CloudApplicationExtended cloudApplicationExtended) {
        return ImmutableIncrementalAppInstanceUpdateConfiguration.builder()
                                                                 .newApplication(cloudApplicationExtended)
                                                                 .newApplicationInstanceCount(5)
                                                                 .oldApplication(buildDeployedMtaApplication())
                                                                 .oldApplicationInitialInstanceCount(10)
                                                                 .oldApplicationInstanceCount(3)
                                                                 .build();
    }

    private void executeOnSuccess(CloudApplicationExtended cloudApplicationExtended) {
        var updateConfig = buildUpdateConfig(cloudApplicationExtended);
        context.setVariable(Variables.INCREMENTAL_APP_INSTANCE_UPDATE_CONFIGURATION, updateConfig);
        step.initializeStepLogger(execution);
        ProcessContext wrapper = step.createProcessContext(execution);
        PollStartAppExecutionWithRollbackExecution asyncExecution = (PollStartAppExecutionWithRollbackExecution) getAsyncOperations(
            wrapper).get(0);
        asyncExecution.onSuccess(wrapper, "App started");
    }

    @Test
    void testOnSuccessWhenNewAppStillIsStarted() {
        CloudApplicationExtended cloudApplicationExtended = buildCloudApplication(5);
        prepareAppToProcess(cloudApplicationExtended);
        executeOnSuccess(cloudApplicationExtended);
        verify(stepLogger).info(any());
    }

    @Override
    protected List<AsyncExecution> getAsyncOperations(ProcessContext wrapper) {
        return List.of(new PollStartAppExecutionWithRollbackExecution(clientFactory, tokenService));
    }

    @Override
    protected void validateOperationExecutionResult(AsyncExecutionState result) {
        assertEquals(expectedAsyncExecutionState, result);
    }

    @Override
    protected IncrementalAppInstancesUpdateStep createStep() {
        clientFactory = Mockito.mock(CloudControllerClientFactory.class);
        tokenService = Mockito.mock(TokenService.class);
        return new IncrementalAppInstancesUpdateStep(clientFactory, tokenService);

    }
}
