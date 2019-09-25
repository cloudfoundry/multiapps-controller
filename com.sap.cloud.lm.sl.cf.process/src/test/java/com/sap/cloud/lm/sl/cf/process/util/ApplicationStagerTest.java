package com.sap.cloud.lm.sl.cf.process.util;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudBuild;
import org.cloudfoundry.client.lib.domain.CloudBuild.DropletInfo;
import org.cloudfoundry.client.lib.domain.CloudMetadata;
import org.cloudfoundry.client.lib.domain.ImmutableCloudApplication;
import org.cloudfoundry.client.lib.domain.PackageState;
import org.cloudfoundry.client.lib.domain.UploadToken;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.steps.ExecutionWrapper;
import com.sap.cloud.lm.sl.cf.process.steps.StepPhase;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

public class ApplicationStagerTest {

    private static final UUID BUILD_GUID = UUID.fromString("8e4da443-f255-499c-8b47-b3729b5b7432");

    private static final UUID DROPLET_GUID = UUID.fromString("9e4da443-f255-499c-8b47-b3729b5b7439");

    private static final UUID APP_GUID = UUID.fromString("1e4da443-f255-499c-8b47-b3729b5b7431");

    private static final UUID PACKAGE_GUID = UUID.fromString("2e4da443-f255-499c-8b47-b3729b5b7432");

    private static final String APP_NAME = "anatz";

    private static final String TOKEN = "token";

    private ApplicationStager applicationStager;

    private CloudControllerClient client;

    private ExecutionWrapper execution;

    private DelegateExecution context;
    private StepLogger stepLogger;

    @BeforeEach
    public void setUp() {
        CloudBuild cloudBuild = Mockito.mock(CloudBuild.class);
        client = Mockito.mock(CloudControllerClient.class);
        execution = Mockito.mock(ExecutionWrapper.class);
        context = Mockito.mock(DelegateExecution.class);
        Mockito.when(client.getBuild(BUILD_GUID))
               .thenReturn(cloudBuild);
        applicationStager = new ApplicationStager(client);
        Mockito.when(context.getVariable(Constants.VAR_BUILD_GUID))
               .thenReturn(BUILD_GUID);
        Mockito.when(execution.getContext())
               .thenReturn(context);
        Mockito.when(execution.getControllerClient())
               .thenReturn(client);
        stepLogger = Mockito.mock(StepLogger.class);
        mockUploadToken();
    }

    @Test
    public void testBuildStateFailed() {
        Mockito.when(client.getBuild(BUILD_GUID)
                           .getState())
               .thenReturn(CloudBuild.State.FAILED);
        Mockito.when(client.getBuild(BUILD_GUID)
                           .getError())
               .thenReturn("Error occurred while creating a build!");
        StagingState stagingState = applicationStager.getStagingState(execution.getContext());
        assertEquals(PackageState.FAILED, stagingState.getState());
        assertEquals("Error occurred while creating a build!", stagingState.getError());
    }

    @Test
    public void testBuildStateNotFoundAppNotFound() {
        ImmutableCloudApplication application = ImmutableCloudApplication.builder()
                                                                         .name(APP_NAME)
                                                                         .build();
        Mockito.when(context.getVariable(Constants.VAR_APP_TO_PROCESS))
               .thenReturn(JsonUtil.toJson(application));
        Mockito.when(client.getBuild(BUILD_GUID))
               .thenThrow(new CloudOperationException(HttpStatus.NOT_FOUND));
        Mockito.when(client.getApplication(APP_NAME))
               .thenThrow(new CloudOperationException(HttpStatus.NOT_FOUND));
        try {
            applicationStager.getStagingState(execution.getContext());
            fail("staging should fail!");
        } catch (CloudOperationException e) {
            Mockito.verify(client, Mockito.times(1))
                   .getBuild(BUILD_GUID);
            Mockito.verify(client, Mockito.times(1))
                   .getApplication(APP_NAME);
        }
    }

    @Test
    public void testBuildStateNotFoundAppFound() {
        ImmutableCloudApplication application = ImmutableCloudApplication.builder()
                                                                         .name(APP_NAME)
                                                                         .build();
        Mockito.when(context.getVariable(Constants.VAR_APP_TO_PROCESS))
               .thenReturn(JsonUtil.toJson(application));
        Mockito.when(client.getBuild(BUILD_GUID))
               .thenThrow(new CloudOperationException(HttpStatus.NOT_FOUND));
        Mockito.when(client.getApplication(APP_NAME))
               .thenReturn(application);
        try {
            applicationStager.getStagingState(execution.getContext());
            fail("staging should fail!");
        } catch (CloudOperationException e) {
            Mockito.verify(client, Mockito.times(1))
                   .getBuild(BUILD_GUID);
            Mockito.verify(client, Mockito.times(1))
                   .getApplication(APP_NAME);
        }
    }

    @Test
    public void testBuildStateStaged() {
        Mockito.when(client.getBuild(BUILD_GUID)
                           .getState())
               .thenReturn(CloudBuild.State.STAGED);
        StagingState stagingState = applicationStager.getStagingState(execution.getContext());
        assertEquals(PackageState.STAGED, stagingState.getState());
        assertNull(stagingState.getError());
    }

    @Test
    public void testBuildStateStaging() {
        Mockito.when(client.getBuild(BUILD_GUID)
                           .getState())
               .thenReturn(CloudBuild.State.STAGING);
        StagingState stagingState = applicationStager.getStagingState(execution.getContext());
        assertEquals(PackageState.PENDING, stagingState.getState());
        assertNull(stagingState.getError());
    }

    @Test
    public void testIsApplicationStagedCorrectlyMetadataIsNull() {
        CloudApplication app = mockApplication();
        Mockito.when(client.getBuildsForApplication(any(UUID.class)))
               .thenReturn(Collections.singletonList(Mockito.mock(CloudBuild.class)));
        Assertions.assertFalse(applicationStager.isApplicationStagedCorrectly(stepLogger, app));
    }

    @Test
    public void testIsApplicationStagedCorrectlyNoLastBuild() {
        CloudApplication app = mockApplication();
        Mockito.when(client.getBuildsForApplication(any(UUID.class)))
               .thenReturn(Collections.emptyList());
        Assertions.assertFalse(applicationStager.isApplicationStagedCorrectly(stepLogger, app));
    }

    @Test
    public void testIsApplicationStagedCorrectlyValidBuild() {
        CloudApplication app = mockApplication();
        CloudBuild mockCloudBuild = mockBuild(CloudBuild.State.STAGED, Mockito.mock(DropletInfo.class), null);
        Mockito.when(client.getBuildsForApplication(any(UUID.class)))
               .thenReturn(Collections.singletonList(mockCloudBuild));
        Assertions.assertTrue(applicationStager.isApplicationStagedCorrectly(stepLogger, app));
    }

    @Test
    public void testIsApplicationStagedCorrectlyBuildStagedFailed() {
        CloudApplication app = mockApplication();
        CloudBuild mockCloudBuild = mockBuild(CloudBuild.State.FAILED, null, null);
        Mockito.when(client.getBuildsForApplication(any(UUID.class)))
               .thenReturn(Collections.singletonList(mockCloudBuild));
        Assertions.assertFalse(applicationStager.isApplicationStagedCorrectly(stepLogger, app));
    }

    @Test
    public void testIsApplicationStagedCorrectlyDropletInfoIsNull() {
        CloudApplication app = mockApplication();
        CloudBuild mockCloudBuild = mockBuild(CloudBuild.State.STAGED, null, null);
        Mockito.when(client.getBuildsForApplication(any(UUID.class)))
               .thenReturn(Collections.singletonList(mockCloudBuild));
        Assertions.assertFalse(applicationStager.isApplicationStagedCorrectly(stepLogger, app));
    }

    @Test
    public void testIsApplicationStagedCorrectlyCloudBuildErrorNotNull() {
        CloudApplication app = mockApplication();
        CloudBuild mockCloudBuildFirst = mockBuild(CloudBuild.State.STAGED, Mockito.mock(DropletInfo.class), "error");
        CloudBuild mockCloudBuildSecond = mockBuild(CloudBuild.State.FAILED, Mockito.mock(DropletInfo.class), "error");
        Mockito.when(client.getBuildsForApplication(any(UUID.class)))
               .thenReturn(Arrays.asList(mockCloudBuildFirst, mockCloudBuildSecond));
        Assertions.assertFalse(applicationStager.isApplicationStagedCorrectly(stepLogger, app));
    }

    @Test
    public void testBindDropletToApp() {
        CloudBuild.DropletInfo droplet = Mockito.mock(DropletInfo.class);
        Mockito.when(client.getBuild(BUILD_GUID)
                           .getDropletInfo())
               .thenReturn(droplet);
        Mockito.when(client.getBuild(BUILD_GUID)
                           .getDropletInfo()
                           .getGuid())
               .thenReturn(DROPLET_GUID);
        applicationStager.bindDropletToApp(execution.getContext(), APP_GUID);
        Mockito.verify(client)
               .bindDropletToApp(DROPLET_GUID, APP_GUID);
    }

    @Test
    public void testStageAppIfThereIsNoUploadToken() {
        Mockito.when(context.getVariable(Constants.VAR_UPLOAD_TOKEN))
               .thenReturn(null);
        assertEquals(StepPhase.DONE, applicationStager.stageApp(context, null, null));
    }

    @Test
    public void testStageAppWithValidParameters() {
        StepLogger stepLogger = Mockito.mock(StepLogger.class);
        CloudApplication app = Mockito.mock(CloudApplication.class);
        Mockito.when(app.getName())
               .thenReturn(APP_NAME);
        UploadToken uploadToken = new UploadToken(TOKEN, PACKAGE_GUID);
        Mockito.when(context.getVariable(Constants.VAR_UPLOAD_TOKEN))
               .thenReturn(JsonUtil.toJson(uploadToken));
        CloudBuild build = Mockito.mock(CloudBuild.class);
        Mockito.when(client.createBuild(PACKAGE_GUID))
               .thenReturn(build);
        CloudMetadata cloudMetadata = Mockito.mock(CloudMetadata.class);
        mockMetadata(cloudMetadata, client.createBuild(PACKAGE_GUID));
        mockBuildCreation(client.createBuild(PACKAGE_GUID));
        StepPhase stepPhase = applicationStager.stageApp(context, app, stepLogger);
        assertEquals(StepPhase.POLL, stepPhase);
        Mockito.verify(context)
               .setVariable(Constants.VAR_BUILD_GUID, BUILD_GUID);
        Mockito.verify(stepLogger)
               .info(Messages.STAGING_APP, APP_NAME);
    }

    @Test
    public void testStageIfNotFoundExceptionIsThrown() {
        CloudApplication app = mockApplication();
        CloudOperationException cloudOperationException = Mockito.mock(CloudOperationException.class);
        Mockito.when(cloudOperationException.getStatusCode())
               .thenReturn(HttpStatus.NOT_FOUND);
        Mockito.when(client.createBuild(any(UUID.class)))
               .thenThrow(cloudOperationException);
        Assertions.assertThrows(CloudOperationException.class, () -> applicationStager.stageApp(context, app, stepLogger));
    }

    @Test
    public void testStageIfUnprocessableEntityExceptionIsThrownNoPreviousBuilds() {
        CloudApplication app = mockApplication();
        CloudOperationException cloudOperationException = Mockito.mock(CloudOperationException.class);
        Mockito.when(cloudOperationException.getStatusCode())
               .thenReturn(HttpStatus.UNPROCESSABLE_ENTITY);
        Mockito.when(client.createBuild(any(UUID.class)))
               .thenThrow(cloudOperationException);
        CloudOperationException thrownException = Assertions.assertThrows(CloudOperationException.class,
                                                                          () -> applicationStager.stageApp(context, app, stepLogger));
        Assertions.assertEquals(HttpStatus.NOT_FOUND, thrownException.getStatusCode());
    }

    @Test
    public void testIfBuildGuidDoesNotExist() {
        Mockito.when(execution.getContext()
                              .getVariable(Constants.VAR_BUILD_GUID))
               .thenReturn(null);
        StagingState stagingState = applicationStager.getStagingState(execution.getContext());
        assertEquals(PackageState.STAGED, stagingState.getState());
        assertNull(stagingState.getError());
    }

    @Test
    public void testStageIfUnprocessableEntityExceptionIsThrownSetPreviousBuildGuid() {
        CloudApplication app = mockApplication();
        CloudOperationException cloudOperationException = Mockito.mock(CloudOperationException.class);
        Mockito.when(cloudOperationException.getStatusCode())
               .thenReturn(HttpStatus.UNPROCESSABLE_ENTITY);
        Mockito.when(client.createBuild(any(UUID.class)))
               .thenThrow(cloudOperationException);
        CloudBuild cloudBuild = mockBuild(CloudBuild.State.STAGING, Mockito.mock(DropletInfo.class), null);
        Mockito.when(client.getBuildsForPackage(any(UUID.class)))
               .thenReturn(Collections.singletonList(cloudBuild));
        applicationStager.stageApp(context, app, stepLogger);
        Mockito.verify(context)
               .setVariable(Constants.VAR_BUILD_GUID, cloudBuild.getMetadata()
                                                                .getGuid());
    }

    private void mockUploadToken() {
        String uploadTokenJson = JsonUtil.toJson(new UploadToken("/" + PACKAGE_GUID, PACKAGE_GUID));
        Mockito.when(context.getVariable(Constants.VAR_UPLOAD_TOKEN))
               .thenReturn(uploadTokenJson);
    }

    private void mockBuildCreation(CloudBuild build) {
        Mockito.when(build.getMetadata()
                          .getGuid())
               .thenReturn(BUILD_GUID);
    }

    private void mockMetadata(CloudMetadata cloudMetadata, CloudBuild build) {
        Mockito.when(build.getMetadata())
               .thenReturn(cloudMetadata);
    }

    private CloudApplication mockApplication() {
        CloudApplication app = Mockito.mock(CloudApplication.class);
        CloudMetadata appCloudMetadata = Mockito.mock(CloudMetadata.class);
        Mockito.when(appCloudMetadata.getGuid())
               .thenReturn(APP_GUID);
        Mockito.when(app.getMetadata())
               .thenReturn(appCloudMetadata);
        Mockito.when(app.getName())
               .thenReturn(APP_NAME);
        return app;
    }

    private CloudBuild mockBuild(CloudBuild.State state, DropletInfo dropletInfo, String error) {
        CloudBuild cloudBuild = Mockito.mock(CloudBuild.class);
        CloudMetadata cloudMetadata = Mockito.mock(CloudMetadata.class);
        Mockito.when(cloudMetadata.getCreatedAt())
               .thenReturn(Mockito.mock(Date.class));
        mockMetadata(cloudMetadata, cloudBuild);
        mockBuildCreation(cloudBuild);
        Mockito.when(cloudBuild.getState())
               .thenReturn(state);
        Mockito.when(cloudBuild.getDropletInfo())
               .thenReturn(dropletInfo);
        Mockito.when(cloudBuild.getError())
               .thenReturn(error);
        return cloudBuild;
    }
}