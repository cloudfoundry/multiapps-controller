package org.cloudfoundry.multiapps.controller.process.steps;

import static org.mockito.ArgumentMatchers.any;

import java.text.MessageFormat;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.sap.cloudfoundry.client.facade.domain.CloudBuild;
import com.sap.cloudfoundry.client.facade.domain.CloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.CloudPackage;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudPackage;

class StageAppStepTest extends SyncFlowableStepTest<StageAppStep> {

    private static final UUID PACKAGE_GUID = UUID.fromString("e4c6c550-1e95-40a1-9f23-d92e0cbc7ab5");
    private static final UUID BUILD_GUID = UUID.fromString("86b98328-6ce4-4369-8bb9-46554a20e5f5");

    @Test
    void testExecuteStep() {
        mockApplication("demo-app");
        mockCloudPackage(PACKAGE_GUID);
        mockClient();
        step.execute(execution);
        Assertions.assertEquals(StepPhase.POLL.toString(), getExecutionStatus());
    }

    private void mockApplication(String applicationName) {
        ImmutableCloudApplicationExtended cloudApplicationExtended = ImmutableCloudApplicationExtended.builder()
                                                                                                      .name(applicationName)
                                                                                                      .build();
        context.setVariable(Variables.APP_TO_PROCESS, cloudApplicationExtended);
    }

    private void mockCloudPackage(UUID packageGuid) {
        CloudPackage cloudPackage = ImmutableCloudPackage.builder()
                                                         .metadata(ImmutableCloudMetadata.builder()
                                                                                         .guid(packageGuid)
                                                                                         .build())
                                                         .build();
        context.setVariable(Variables.CLOUD_PACKAGE, cloudPackage);
    }

    private void mockClient() {
        CloudBuild cloudBuild = Mockito.mock(CloudBuild.class);
        CloudMetadata cloudMetadata = Mockito.mock(CloudMetadata.class);
        Mockito.when(cloudMetadata.getGuid())
               .thenReturn(BUILD_GUID);
        Mockito.when(cloudBuild.getMetadata())
               .thenReturn(cloudMetadata);
        Mockito.when(client.createBuild(any()))
               .thenReturn(cloudBuild);
    }

    @Test
    void testGetErrorMessage() {
        String applicationName = "another-app";
        mockApplication(applicationName);
        Assertions.assertEquals(MessageFormat.format(Messages.ERROR_STAGING_APP_0, applicationName), step.getStepErrorMessage(context));
    }

    @Test
    void testAsyncExecutionStatus() {
        List<AsyncExecution> asyncStepExecutions = step.getAsyncStepExecutions(context);
        Assertions.assertEquals(1, asyncStepExecutions.size());
        Assertions.assertTrue(asyncStepExecutions.get(0) instanceof PollStageAppStatusExecution);
    }

    @Test
    void testGetTimeoutDefaultValue() {
        Assertions.assertEquals(Variables.START_TIMEOUT.getDefaultValue(), step.getTimeout(context));
    }

    @Test
    void testGetTimeoutCustomValue() {
        var timeout = Duration.ofSeconds(10);
        context.setVariable(Variables.START_TIMEOUT, timeout);
        Assertions.assertEquals(timeout, step.getTimeout(context));
    }

    @Override
    protected StageAppStep createStep() {
        return new StageAppStep();
    }

}
