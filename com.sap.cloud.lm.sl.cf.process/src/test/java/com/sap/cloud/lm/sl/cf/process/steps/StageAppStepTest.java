package com.sap.cloud.lm.sl.cf.process.steps;

import static org.mockito.ArgumentMatchers.any;

import java.text.MessageFormat;
import java.util.List;
import java.util.UUID;

import org.cloudfoundry.client.lib.domain.CloudBuild;
import org.cloudfoundry.client.lib.domain.CloudMetadata;
import org.cloudfoundry.client.lib.domain.UploadToken;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.client.lib.domain.ImmutableCloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

public class StageAppStepTest extends SyncFlowableStepTest<StageAppStep> {

    private static final UUID PACKAGE_GUID = UUID.fromString("e4c6c550-1e95-40a1-9f23-d92e0cbc7ab5");
    private static final UUID BUILD_GUID = UUID.fromString("86b98328-6ce4-4369-8bb9-46554a20e5f5");

    @Test
    public void testExecuteStep() {
        mockApplication("demo-app");
        mockUploadToken(PACKAGE_GUID);
        mockClient();
        step.execute(context);
        Assertions.assertEquals(StepPhase.POLL.toString(), getExecutionStatus());
    }

    private void mockApplication(String applicationName) {
        ImmutableCloudApplicationExtended cloudApplicationExtended = ImmutableCloudApplicationExtended.builder()
                                                                                                      .name(applicationName)
                                                                                                      .build();
        Mockito.when(context.getVariable(Constants.VAR_APP_TO_PROCESS))
               .thenReturn(JsonUtil.toJson(cloudApplicationExtended));
    }

    private void mockUploadToken(UUID packageGuid) {
        UploadToken uploadToken = new UploadToken();
        uploadToken.setPackageGuid(packageGuid);
        Mockito.when(context.getVariable(Constants.VAR_UPLOAD_TOKEN))
               .thenReturn(JsonUtil.toJson(uploadToken));
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
        Assertions.assertEquals(MessageFormat.format(Messages.ERROR_STAGING_APP_1, applicationName), step.getStepErrorMessage(context));
    }

    @Test
    public void testAsyncExecutionStatus() {
        List<AsyncExecution> asyncStepExecutions = step.getAsyncStepExecutions(execution);
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
        Mockito.when(context.getVariable(Constants.PARAM_START_TIMEOUT))
               .thenReturn(timeout);
        Assertions.assertEquals(timeout, step.getTimeout(context));
    }

    @Override
    protected StageAppStep createStep() {
        return new StageAppStep();
    }
}
