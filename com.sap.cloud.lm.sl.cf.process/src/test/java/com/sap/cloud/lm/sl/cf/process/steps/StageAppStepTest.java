package com.sap.cloud.lm.sl.cf.process.steps;

import static org.mockito.ArgumentMatchers.any;

import java.text.MessageFormat;
import java.util.List;
import java.util.UUID;

import org.cloudfoundry.client.lib.domain.CloudBuild;
import org.cloudfoundry.client.lib.domain.CloudMetadata;
import org.cloudfoundry.client.lib.domain.ImmutableUploadToken;
import org.cloudfoundry.client.lib.domain.UploadToken;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.client.lib.domain.ImmutableCloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;

public class StageAppStepTest extends SyncFlowableStepTest<StageAppStep> {

    private static final UUID PACKAGE_GUID = UUID.fromString("e4c6c550-1e95-40a1-9f23-d92e0cbc7ab5");
    private static final UUID BUILD_GUID = UUID.fromString("86b98328-6ce4-4369-8bb9-46554a20e5f5");

    @Test
    public void testExecuteStep() {
        mockApplication("demo-app");
        mockUploadToken(PACKAGE_GUID);
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

    private void mockUploadToken(UUID packageGuid) {
        UploadToken uploadToken = ImmutableUploadToken.builder()
                                                      .packageGuid(packageGuid)
                                                      .build();
        context.setVariable(Variables.UPLOAD_TOKEN, uploadToken);
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
    public void testGetErrorMessage() {
        String applicationName = "another-app";
        mockApplication(applicationName);
        Assertions.assertEquals(MessageFormat.format(Messages.ERROR_STAGING_APP_0, applicationName), step.getStepErrorMessage(context));
    }

    @Test
    public void testAsyncExecutionStatus() {
        List<AsyncExecution> asyncStepExecutions = step.getAsyncStepExecutions(context);
        Assertions.assertEquals(1, asyncStepExecutions.size());
        Assertions.assertTrue(asyncStepExecutions.get(0) instanceof PollStageAppStatusExecution);
    }

    @Test
    public void testGetTimeoutDefaultValue() {
        Assertions.assertEquals(Constants.DEFAULT_START_TIMEOUT, step.getTimeout(context));
    }

    @Test
    public void testGetTimeoutCustomValue() {
        int timeout = 10;
        context.setVariable(Variables.START_TIMEOUT, timeout);
        Assertions.assertEquals(timeout, step.getTimeout(context));
    }

    @Override
    protected StageAppStep createStep() {
        return new StageAppStep();
    }
}
