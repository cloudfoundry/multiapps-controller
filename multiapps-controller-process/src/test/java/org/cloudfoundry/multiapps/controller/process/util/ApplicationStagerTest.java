package org.cloudfoundry.multiapps.controller.process.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientProvider;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.steps.StepPhase;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.CloudBuild;
import com.sap.cloudfoundry.client.facade.domain.DropletInfo;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudApplication;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudBuild;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudPackage;
import com.sap.cloudfoundry.client.facade.domain.ImmutableDropletInfo;
import com.sap.cloudfoundry.client.facade.domain.PackageState;

class ApplicationStagerTest {

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
    private ProcessContext context;
    private ApplicationStager applicationStager;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        this.context = new ProcessContext(MockDelegateExecution.createSpyInstance(), stepLogger, clientProvider);
        context.setVariable(Variables.USER, "whatever");
        Mockito.when(clientProvider.getControllerClient(Mockito.any(), Mockito.any(), Mockito.any()))
               .thenReturn(client);
        this.applicationStager = new ApplicationStager(context);
        setCloudPackage();
    }

    @Test
    void testBuildStateFailed() {
        context.setVariable(Variables.BUILD_GUID, BUILD_GUID);
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
    void testBuildStateNotFoundAppNotFound() {
        context.setVariable(Variables.BUILD_GUID, BUILD_GUID);
        CloudApplicationExtended application = ImmutableCloudApplicationExtended.builder()
                                                                                .name(APP_NAME)
                                                                                .build();
        context.setVariable(Variables.APP_TO_PROCESS, application);
        Mockito.when(client.getBuild(BUILD_GUID))
               .thenThrow(new CloudOperationException(HttpStatus.NOT_FOUND));
        Mockito.when(client.getApplicationGuid(APP_NAME))
               .thenThrow(new CloudOperationException(HttpStatus.NOT_FOUND));
        try {
            applicationStager.getStagingState();
            fail("Staging should fail!");
        } catch (CloudOperationException e) {
            Mockito.verify(client)
                   .getBuild(BUILD_GUID);
            Mockito.verify(client)
                   .getApplicationGuid(APP_NAME);
        }
    }

    @Test
    void testBuildStateNotFoundAppFound() {
        context.setVariable(Variables.BUILD_GUID, BUILD_GUID);
        CloudApplicationExtended application = ImmutableCloudApplicationExtended.builder()
                                                                                .name(APP_NAME)
                                                                                .build();
        context.setVariable(Variables.APP_TO_PROCESS, application);
        Mockito.when(client.getBuild(BUILD_GUID))
               .thenThrow(new CloudOperationException(HttpStatus.NOT_FOUND));
        Mockito.when(client.getApplicationGuid(APP_NAME))
               .thenReturn(APP_GUID);
        try {
            applicationStager.getStagingState();
            fail("Staging should fail!");
        } catch (CloudOperationException e) {
            Mockito.verify(client)
                   .getBuild(BUILD_GUID);
            Mockito.verify(client)
                   .getApplicationGuid(APP_NAME);
        }
    }

    @Test
    void testBuildStateStaged() {
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
    void testBuildStateStaging() {
        context.setVariable(Variables.BUILD_GUID, BUILD_GUID);
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
    void testIsApplicationStagedCorrectlyMetadataIsNull() {
        CloudApplication app = createApplication();
        Mockito.when(client.getBuildsForApplication(any(UUID.class)))
               .thenReturn(List.of(Mockito.mock(CloudBuild.class)));
        Assertions.assertFalse(applicationStager.isApplicationStagedCorrectly(app));
    }

    @Test
    void testIsApplicationStagedCorrectlyNoLastBuild() {
        CloudApplication app = createApplication();
        Mockito.when(client.getBuildsForApplication(any(UUID.class)))
               .thenReturn(Collections.emptyList());
        Assertions.assertFalse(applicationStager.isApplicationStagedCorrectly(app));
    }

    @Test
    void testIsApplicationStagedCorrectlyValidBuild() {
        CloudApplication app = createApplication();
        CloudBuild build = createBuild(CloudBuild.State.STAGED, Mockito.mock(DropletInfo.class), null);
        Mockito.when(client.getBuildsForApplication(any(UUID.class)))
               .thenReturn(List.of(build));
        Assertions.assertTrue(applicationStager.isApplicationStagedCorrectly(app));
    }

    @Test
    void testIsApplicationStagedCorrectlyBuildStagedFailed() {
        CloudApplication app = createApplication();
        CloudBuild build = createBuild(CloudBuild.State.FAILED, null, null);
        Mockito.when(client.getBuildsForApplication(any(UUID.class)))
               .thenReturn(List.of(build));
        Assertions.assertFalse(applicationStager.isApplicationStagedCorrectly(app));
    }

    @Test
    void testIsApplicationStagedCorrectlyDropletInfoIsNull() {
        CloudApplication app = createApplication();
        CloudBuild build = createBuild(CloudBuild.State.STAGED, null, null);
        Mockito.when(client.getBuildsForApplication(any(UUID.class)))
               .thenReturn(List.of(build));
        Assertions.assertFalse(applicationStager.isApplicationStagedCorrectly(app));
    }

    @Test
    void testIsApplicationStagedCorrectlyBuildErrorNotNull() {
        CloudApplication app = createApplication();
        ImmutableDropletInfo dropletInfo = ImmutableDropletInfo.of(DROPLET_GUID, null);
        CloudBuild build1 = createBuild(CloudBuild.State.STAGED, dropletInfo, null, new Date(0));
        CloudBuild build2 = createBuild(CloudBuild.State.FAILED, dropletInfo, "error", new Date(1));
        Mockito.when(client.getBuildsForApplication(any(UUID.class)))
               .thenReturn(List.of(build1, build2));
        Assertions.assertFalse(applicationStager.isApplicationStagedCorrectly(app));
    }

    @Test
    void testBindDropletToApp() {
        context.setVariable(Variables.BUILD_GUID, BUILD_GUID);
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
    void testStageAppIfThereIsNoCloudPacakge() {
        context.setVariable(Variables.CLOUD_PACKAGE, null);
        assertEquals(StepPhase.DONE, applicationStager.stageApp(null));
    }

    @Test
    void testStageAppWithValidParameters() {
        CloudApplication app = ImmutableCloudApplication.builder()
                                                        .name(APP_NAME)
                                                        .build();
        setCloudPackage();
        CloudBuild build = createBuild();
        Mockito.when(client.createBuild(PACKAGE_GUID))
               .thenReturn(build);
        StepPhase stepPhase = applicationStager.stageApp(app);
        assertEquals(StepPhase.POLL, stepPhase);
        assertEquals(BUILD_GUID, context.getVariable(Variables.BUILD_GUID));
        Mockito.verify(stepLogger)
               .info(Messages.STAGING_APP, APP_NAME);
    }

    @Test
    void testStageIfNotFoundExceptionIsThrown() {
        CloudApplication app = createApplication();
        CloudOperationException cloudOperationException = Mockito.mock(CloudOperationException.class);
        Mockito.when(cloudOperationException.getStatusCode())
               .thenReturn(HttpStatus.NOT_FOUND);
        Mockito.when(client.createBuild(any(UUID.class)))
               .thenThrow(cloudOperationException);
        Assertions.assertThrows(CloudOperationException.class, () -> applicationStager.stageApp(app));
    }

    @Test
    void testStageIfUnprocessableEntityExceptionIsThrownNoPreviousBuilds() {
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
    void testIfBuildGuidDoesNotExist() {
        StagingState stagingState = applicationStager.getStagingState();
        assertEquals(PackageState.STAGED, stagingState.getState());
        assertNull(stagingState.getError());
    }

    @Test
    void testStageIfUnprocessableEntityExceptionIsThrownSetPreviousBuildGuid() {
        CloudApplication app = createApplication();
        Mockito.when(client.createBuild(any(UUID.class)))
               .thenThrow(new CloudOperationException(HttpStatus.UNPROCESSABLE_ENTITY));
        CloudBuild build = createBuild(CloudBuild.State.STAGING, Mockito.mock(DropletInfo.class), null);
        Mockito.when(client.getBuildsForPackage(any(UUID.class)))
               .thenReturn(List.of(build));
        applicationStager.stageApp(app);
        assertEquals(build.getMetadata()
                          .getGuid(),
                     context.getVariable(Variables.BUILD_GUID));
    }

    private void setCloudPackage() {
        context.setVariable(Variables.CLOUD_PACKAGE, ImmutableCloudPackage.builder()
                                                                          .metadata(ImmutableCloudMetadata.builder()
                                                                                                          .guid(PACKAGE_GUID)
                                                                                                          .build())
                                                                          .build());
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