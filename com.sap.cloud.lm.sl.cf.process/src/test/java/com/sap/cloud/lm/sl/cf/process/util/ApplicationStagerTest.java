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
import org.cloudfoundry.client.lib.domain.ImmutableCloudApplication;
import org.cloudfoundry.client.lib.domain.ImmutableCloudBuild;
import org.cloudfoundry.client.lib.domain.ImmutableCloudBuild.ImmutableDropletInfo;
import org.cloudfoundry.client.lib.domain.ImmutableCloudMetadata;
import org.cloudfoundry.client.lib.domain.ImmutableUploadToken;
import org.cloudfoundry.client.lib.domain.PackageState;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ImmutableCloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.cf.CloudControllerClientProvider;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.mock.MockDelegateExecution;
import com.sap.cloud.lm.sl.cf.process.steps.ExecutionWrapper;
import com.sap.cloud.lm.sl.cf.process.steps.StepPhase;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

public class ApplicationStagerTest {

    private static final UUID BUILD_GUID = UUID.fromString("8e4da443-f255-499c-8b47-b3729b5b7432");
    private static final UUID DROPLET_GUID = UUID.fromString("9e4da443-f255-499c-8b47-b3729b5b7439");
    private static final UUID APP_GUID = UUID.fromString("1e4da443-f255-499c-8b47-b3729b5b7431");
    private static final UUID PACKAGE_GUID = UUID.fromString("2e4da443-f255-499c-8b47-b3729b5b7432");
    private static final String APP_NAME = "anatz";

    @Mock
    private CloudControllerClient client;
    @Mock
    private CloudControllerClientProvider clientProvider;
    @Mock
    private StepLogger stepLogger;
    private DelegateExecution context = MockDelegateExecution.createSpyInstance();
    private ExecutionWrapper execution;
    private ApplicationStager applicationStager;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        context.setVariable(Constants.VAR_USER, "whatever");
        Mockito.when(clientProvider.getControllerClient(Mockito.any(), Mockito.any()))
               .thenReturn(client);
        this.execution = new ExecutionWrapper(context, stepLogger, clientProvider);
        this.applicationStager = new ApplicationStager(execution);
        setUploadTokenVariable();
    }

    @Test
    public void testBuildStateFailed() {
        context.setVariable(Constants.VAR_BUILD_GUID, BUILD_GUID);
        CloudBuild build = ImmutableCloudBuild.builder()
                                              .state(CloudBuild.State.FAILED)
                                              .error("Error occurred while creating a build!")
                                              .build();
        Mockito.when(client.getBuild(BUILD_GUID))
               .thenReturn(build);
        StagingState stagingState = applicationStager.getStagingState();
        assertEquals(PackageState.FAILED, stagingState.getState());
        assertEquals("Error occurred while creating a build!", stagingState.getError());
    }

    @Test
    public void testBuildStateNotFoundAppNotFound() {
        context.setVariable(Constants.VAR_BUILD_GUID, BUILD_GUID);
        CloudApplicationExtended application = ImmutableCloudApplicationExtended.builder()
                                                                                .name(APP_NAME)
                                                                                .build();
        execution.setVariable(Variables.APP_TO_PROCESS, application);
        Mockito.when(client.getBuild(BUILD_GUID))
               .thenThrow(new CloudOperationException(HttpStatus.NOT_FOUND));
        Mockito.when(client.getApplication(APP_NAME))
               .thenThrow(new CloudOperationException(HttpStatus.NOT_FOUND));
        try {
            applicationStager.getStagingState();
            fail("Staging should fail!");
        } catch (CloudOperationException e) {
            Mockito.verify(client)
                   .getBuild(BUILD_GUID);
            Mockito.verify(client)
                   .getApplication(APP_NAME);
        }
    }

    @Test
    public void testBuildStateNotFoundAppFound() {
        context.setVariable(Constants.VAR_BUILD_GUID, BUILD_GUID);
        CloudApplicationExtended application = ImmutableCloudApplicationExtended.builder()
                                                                                .name(APP_NAME)
                                                                                .build();
        execution.setVariable(Variables.APP_TO_PROCESS, application);
        Mockito.when(client.getBuild(BUILD_GUID))
               .thenThrow(new CloudOperationException(HttpStatus.NOT_FOUND));
        Mockito.when(client.getApplication(APP_NAME))
               .thenReturn(application);
        try {
            applicationStager.getStagingState();
            fail("Staging should fail!");
        } catch (CloudOperationException e) {
            Mockito.verify(client)
                   .getBuild(BUILD_GUID);
            Mockito.verify(client)
                   .getApplication(APP_NAME);
        }
    }

    @Test
    public void testBuildStateStaged() {
        CloudBuild build = ImmutableCloudBuild.builder()
                                              .state(CloudBuild.State.STAGED)
                                              .build();
        Mockito.when(client.getBuild(BUILD_GUID))
               .thenReturn(build);
        StagingState stagingState = applicationStager.getStagingState();
        assertEquals(PackageState.STAGED, stagingState.getState());
        assertNull(stagingState.getError());
    }

    @Test
    public void testBuildStateStaging() {
        context.setVariable(Constants.VAR_BUILD_GUID, BUILD_GUID);
        CloudBuild build = ImmutableCloudBuild.builder()
                                              .state(CloudBuild.State.STAGING)
                                              .build();
        Mockito.when(client.getBuild(BUILD_GUID))
               .thenReturn(build);
        StagingState stagingState = applicationStager.getStagingState();
        assertEquals(PackageState.PENDING, stagingState.getState());
        assertNull(stagingState.getError());
    }

    @Test
    public void testIsApplicationStagedCorrectlyMetadataIsNull() {
        CloudApplication app = createApplication();
        Mockito.when(client.getBuildsForApplication(any(UUID.class)))
               .thenReturn(Collections.singletonList(Mockito.mock(CloudBuild.class)));
        Assertions.assertFalse(applicationStager.isApplicationStagedCorrectly(app));
    }

    @Test
    public void testIsApplicationStagedCorrectlyNoLastBuild() {
        CloudApplication app = createApplication();
        Mockito.when(client.getBuildsForApplication(any(UUID.class)))
               .thenReturn(Collections.emptyList());
        Assertions.assertFalse(applicationStager.isApplicationStagedCorrectly(app));
    }

    @Test
    public void testIsApplicationStagedCorrectlyValidBuild() {
        CloudApplication app = createApplication();
        CloudBuild build = createBuild(CloudBuild.State.STAGED, Mockito.mock(DropletInfo.class), null);
        Mockito.when(client.getBuildsForApplication(any(UUID.class)))
               .thenReturn(Collections.singletonList(build));
        Assertions.assertTrue(applicationStager.isApplicationStagedCorrectly(app));
    }

    @Test
    public void testIsApplicationStagedCorrectlyBuildStagedFailed() {
        CloudApplication app = createApplication();
        CloudBuild build = createBuild(CloudBuild.State.FAILED, null, null);
        Mockito.when(client.getBuildsForApplication(any(UUID.class)))
               .thenReturn(Collections.singletonList(build));
        Assertions.assertFalse(applicationStager.isApplicationStagedCorrectly(app));
    }

    @Test
    public void testIsApplicationStagedCorrectlyDropletInfoIsNull() {
        CloudApplication app = createApplication();
        CloudBuild build = createBuild(CloudBuild.State.STAGED, null, null);
        Mockito.when(client.getBuildsForApplication(any(UUID.class)))
               .thenReturn(Collections.singletonList(build));
        Assertions.assertFalse(applicationStager.isApplicationStagedCorrectly(app));
    }

    @Test
    public void testIsApplicationStagedCorrectlyBuildErrorNotNull() {
        CloudApplication app = createApplication();
        ImmutableDropletInfo dropletInfo = ImmutableDropletInfo.of(DROPLET_GUID);
        CloudBuild build1 = createBuild(CloudBuild.State.STAGED, dropletInfo, null, new Date(0));
        CloudBuild build2 = createBuild(CloudBuild.State.FAILED, dropletInfo, "error", new Date(1));
        Mockito.when(client.getBuildsForApplication(any(UUID.class)))
               .thenReturn(Arrays.asList(build1, build2));
        Assertions.assertFalse(applicationStager.isApplicationStagedCorrectly(app));
    }

    @Test
    public void testBindDropletToApp() {
        context.setVariable(Constants.VAR_BUILD_GUID, BUILD_GUID);
        CloudBuild build = ImmutableCloudBuild.builder()
                                              .dropletInfo(ImmutableDropletInfo.builder()
                                                                               .guid(DROPLET_GUID)
                                                                               .build())
                                              .build();
        Mockito.when(client.getBuild(BUILD_GUID))
               .thenReturn(build);
        applicationStager.bindDropletToApplication(APP_GUID);
        Mockito.verify(client)
               .bindDropletToApp(DROPLET_GUID, APP_GUID);
    }

    @Test
    public void testStageAppIfThereIsNoUploadToken() {
        context.setVariable(Constants.VAR_UPLOAD_TOKEN, null);
        assertEquals(StepPhase.DONE, applicationStager.stageApp(null));
    }

    @Test
    public void testStageAppWithValidParameters() {
        CloudApplication app = ImmutableCloudApplication.builder()
                                                        .name(APP_NAME)
                                                        .build();
        setUploadTokenVariable();
        CloudBuild build = createBuild();
        Mockito.when(client.createBuild(PACKAGE_GUID))
               .thenReturn(build);
        StepPhase stepPhase = applicationStager.stageApp(app);
        assertEquals(StepPhase.POLL, stepPhase);
        Mockito.verify(context)
               .setVariable(Constants.VAR_BUILD_GUID, BUILD_GUID);
        Mockito.verify(stepLogger)
               .info(Messages.STAGING_APP, APP_NAME);
    }

    @Test
    public void testStageIfNotFoundExceptionIsThrown() {
        CloudApplication app = createApplication();
        CloudOperationException cloudOperationException = Mockito.mock(CloudOperationException.class);
        Mockito.when(cloudOperationException.getStatusCode())
               .thenReturn(HttpStatus.NOT_FOUND);
        Mockito.when(client.createBuild(any(UUID.class)))
               .thenThrow(cloudOperationException);
        Assertions.assertThrows(CloudOperationException.class, () -> applicationStager.stageApp(app));
    }

    @Test
    public void testStageIfUnprocessableEntityExceptionIsThrownNoPreviousBuilds() {
        CloudApplication app = createApplication();
        CloudOperationException cloudOperationException = Mockito.mock(CloudOperationException.class);
        Mockito.when(cloudOperationException.getStatusCode())
               .thenReturn(HttpStatus.UNPROCESSABLE_ENTITY);
        Mockito.when(client.createBuild(any(UUID.class)))
               .thenThrow(cloudOperationException);
        CloudOperationException thrownException = Assertions.assertThrows(CloudOperationException.class,
                                                                          () -> applicationStager.stageApp(app));
        Assertions.assertEquals(HttpStatus.NOT_FOUND, thrownException.getStatusCode());
    }

    @Test
    public void testIfBuildGuidDoesNotExist() {
        StagingState stagingState = applicationStager.getStagingState();
        assertEquals(PackageState.STAGED, stagingState.getState());
        assertNull(stagingState.getError());
    }

    @Test
    public void testStageIfUnprocessableEntityExceptionIsThrownSetPreviousBuildGuid() {
        CloudApplication app = createApplication();
        Mockito.when(client.createBuild(any(UUID.class)))
               .thenThrow(new CloudOperationException(HttpStatus.UNPROCESSABLE_ENTITY));
        CloudBuild build = createBuild(CloudBuild.State.STAGING, Mockito.mock(DropletInfo.class), null);
        Mockito.when(client.getBuildsForPackage(any(UUID.class)))
               .thenReturn(Collections.singletonList(build));
        applicationStager.stageApp(app);
        Mockito.verify(context)
               .setVariable(Constants.VAR_BUILD_GUID, build.getMetadata()
                                                           .getGuid());
    }

    private void setUploadTokenVariable() {
        String uploadTokenJson = JsonUtil.toJson(ImmutableUploadToken.builder()
                                                                     .packageGuid(PACKAGE_GUID)
                                                                     .build());
        context.setVariable(Constants.VAR_UPLOAD_TOKEN, uploadTokenJson);
    }

    private CloudApplication createApplication() {
        return ImmutableCloudApplication.builder()
                                        .metadata(ImmutableCloudMetadata.builder()
                                                                        .guid(APP_GUID)
                                                                        .createdAt(new Date())
                                                                        .updatedAt(new Date())
                                                                        .build())
                                        .name(APP_NAME)
                                        .build();
    }

    private CloudBuild createBuild() {
        return createBuild(null, null, null);
    }

    private CloudBuild createBuild(CloudBuild.State state, DropletInfo dropletInfo, String error) {
        return createBuild(state, dropletInfo, error, new Date());
    }

    private CloudBuild createBuild(CloudBuild.State state, DropletInfo dropletInfo, String error, Date timestamp) {
        return ImmutableCloudBuild.builder()
                                  .metadata(ImmutableCloudMetadata.builder()
                                                                  .guid(BUILD_GUID)
                                                                  .createdAt(timestamp)
                                                                  .updatedAt(timestamp)
                                                                  .build())
                                  .state(state)
                                  .error(error)
                                  .dropletInfo(dropletInfo)
                                  .build();
    }
}