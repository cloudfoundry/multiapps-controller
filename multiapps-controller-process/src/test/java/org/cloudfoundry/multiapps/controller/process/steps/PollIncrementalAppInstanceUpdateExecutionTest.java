package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientFactory;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaApplication;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableDeployedMtaApplication;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableIncrementalAppInstanceUpdateConfiguration;
import org.cloudfoundry.multiapps.controller.core.security.token.TokenService;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;

class PollIncrementalAppInstanceUpdateExecutionTest extends AsyncStepOperationTest<IncrementalAppInstancesUpdateStep> {

    private static final String APP_TO_PROCESS_NAME = "app-to-process-idle";
    private static final UUID APP_TO_PROCESS_GUID = UUID.randomUUID();
    private static final String MODULE_NAME = "app-to-process-module";
    private static final String DEPLOYED_APP_NAME = "app-to-process-live";
    private static final UUID DEPLOYED_APP_GUID = UUID.randomUUID();

    private CloudControllerClientFactory clientFactory;
    private TokenService tokenService;

    private AsyncExecutionState expectedAsyncExecutionState;

    @Test
    void executeStepWhenTheApplicationIsAlreadyScaled() {
        prepareAppToProcess(3);
        prepareIncrementalAppInstanceUpdateConfiguration(3, 3);
        expectedAsyncExecutionState = AsyncExecutionState.FINISHED;
        testExecuteOperations();
    }

    private void prepareAppToProcess(int instances) {
        CloudApplicationExtended cloudApplicationExtended = buildCloudApplicationToProcess(instances);
        context.setVariable(Variables.APP_TO_PROCESS, cloudApplicationExtended);
    }

    private CloudApplicationExtended buildCloudApplicationToProcess(int instances) {
        return ImmutableCloudApplicationExtended.builder()
                                                .name(APP_TO_PROCESS_NAME)
                                                .instances(instances)
                                                .metadata(ImmutableCloudMetadata.builder()
                                                                                .guid(APP_TO_PROCESS_GUID)
                                                                                .build())
                                                .build();
    }

    private void prepareIncrementalAppInstanceUpdateConfiguration(int newAppInstances, int oldApplicationInstances) {
        var updateConfig = ImmutableIncrementalAppInstanceUpdateConfiguration.builder()
                                                                             .newApplication(buildCloudApplicationToProcess(newAppInstances))
                                                                             .newApplicationInstanceCount(newAppInstances)
                                                                             .oldApplicationInitialInstanceCount(4)
                                                                             .oldApplicationInstanceCount(oldApplicationInstances)
                                                                             .oldApplication(buildDeployedMtaApplication())
                                                                             .build();
        context.setVariable(Variables.INCREMENTAL_APP_INSTANCE_UPDATE_CONFIGURATION, updateConfig);
    }

    private DeployedMtaApplication buildDeployedMtaApplication() {
        return ImmutableDeployedMtaApplication.builder()
                                              .name(DEPLOYED_APP_NAME)
                                              .metadata(ImmutableCloudMetadata.builder()
                                                                              .guid(DEPLOYED_APP_GUID)
                                                                              .build())
                                              .moduleName(MODULE_NAME)
                                              .build();
    }

    @Test
    void executeStepWhenBothApplicationsNeedRescaling() {
        prepareAppToProcess(6);
        prepareIncrementalAppInstanceUpdateConfiguration(3, 3);
        context.setVariable(Variables.EXISTING_APP_TO_POLL, ImmutableCloudApplicationExtended.builder()
                                                                                             .name(APP_TO_PROCESS_NAME)
                                                                                             .build());
        expectedAsyncExecutionState = AsyncExecutionState.RUNNING;
        testExecuteOperations();
        verify(client).updateApplicationInstances(APP_TO_PROCESS_NAME, 4);
        verify(client).updateApplicationInstances(DEPLOYED_APP_NAME, 2);
    }

    @Test
    void executeStepWhenOnlyNewApplicationNeedsRescaling() {
        prepareAppToProcess(6);
        prepareIncrementalAppInstanceUpdateConfiguration(3, 1);
        context.setVariable(Variables.EXISTING_APP_TO_POLL, ImmutableCloudApplicationExtended.builder()
                                                                                             .name(APP_TO_PROCESS_NAME)
                                                                                             .build());
        expectedAsyncExecutionState = AsyncExecutionState.RUNNING;
        testExecuteOperations();
        verify(client).updateApplicationInstances(APP_TO_PROCESS_NAME, 4);
    }

    @Override
    protected List<AsyncExecution> getAsyncOperations(ProcessContext wrapper) {
        return List.of(new PollIncrementalAppInstanceUpdateExecution());
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
