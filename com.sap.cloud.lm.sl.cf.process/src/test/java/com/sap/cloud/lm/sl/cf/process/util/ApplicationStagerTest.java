package com.sap.cloud.lm.sl.cf.process.util;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudBuild;
import org.cloudfoundry.client.lib.domain.CloudEntity;
import org.cloudfoundry.client.lib.domain.PackageState;
import org.cloudfoundry.client.lib.domain.UploadToken;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.steps.ExecutionWrapper;
import com.sap.cloud.lm.sl.cf.process.steps.StepPhase;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.Pair;

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

    @Before
    public void initFakeDependencies() {
        CloudBuild cloudBuild = Mockito.mock(CloudBuild.class);
        client = Mockito.mock(CloudControllerClient.class);
        execution = Mockito.mock(ExecutionWrapper.class);
        context = Mockito.mock(DelegateExecution.class);
        Mockito.when(client.getBuild(BUILD_GUID))
            .thenReturn(cloudBuild);
        applicationStager = new ApplicationStager();

        Mockito.when(context.getVariable(Constants.VAR_BUILD_GUID))
            .thenReturn(BUILD_GUID);
        Mockito.when(execution.getContext())
            .thenReturn(context);

        Mockito.when(execution.getControllerClient())
            .thenReturn(client);
    }

    @Test
    public void testBuildStateFailed() {
        Mockito.when(client.getBuild(BUILD_GUID)
            .getState())
            .thenReturn(CloudBuild.BuildState.FAILED);
        Mockito.when(client.getBuild(BUILD_GUID)
            .getError())
            .thenReturn("Error occured while creating a build!");

        StagingState stagingState = applicationStager.getStagingState(execution, client);
        assertEquals(PackageState.FAILED, stagingState.getState());
        assertEquals("Error occured while creating a build!", stagingState.getError());
    }

    @Test
    public void testBuildStateStaging() {
        Mockito.when(client.getBuild(BUILD_GUID)
            .getState())
            .thenReturn(CloudBuild.BuildState.STAGING);

        StagingState stagingState = applicationStager.getStagingState(execution, client);
        assertEquals(PackageState.PENDING, stagingState.getState());
        assertNull(stagingState.getError());
    }

    @Test
    public void testBuildStateStaged() {
        Mockito.when(client.getBuild(BUILD_GUID)
            .getState())
            .thenReturn(CloudBuild.BuildState.STAGED);

        StagingState stagingState = applicationStager.getStagingState(execution, client);
        assertEquals(PackageState.STAGED, stagingState.getState());
        assertNull(stagingState.getError());
    }

    @Test
    public void testBindDropletToApp() {
        CloudBuild.Droplet droplet = Mockito.mock(CloudBuild.Droplet.class);
        Mockito.when(client.getBuild(BUILD_GUID)
            .getDroplet())
            .thenReturn(droplet);
        Mockito.when(client.getBuild(BUILD_GUID)
            .getDroplet()
            .getGuid())
            .thenReturn(DROPLET_GUID);

        applicationStager.bindDropletToApp(execution, APP_GUID, client);

        Mockito.verify(client)
            .bindDropletToApp(DROPLET_GUID, APP_GUID);
    }

    @Test
    public void testStageAppIfThereIsNoUploadToken() {
        assertEquals(applicationStager.stageApp(context, client, null, null), StepPhase.DONE);
    }

    @Test
    public void testStageAppWithValidParameters() {
        StepLogger stepLogger = Mockito.mock(StepLogger.class);
        CloudApplication app = Mockito.mock(CloudApplication.class);
        Mockito.when(app.getName())
            .thenReturn(APP_NAME);
        String uploadTokenJson = JsonUtil.toJson(new UploadToken(TOKEN, PACKAGE_GUID));
        Mockito.when(context.getVariable(Constants.VAR_UPLOAD_TOKEN))
            .thenReturn(uploadTokenJson);
        CloudBuild build = Mockito.mock(CloudBuild.class);
        Mockito.when(client.createBuild(PACKAGE_GUID))
            .thenReturn(build);
        CloudEntity.Meta meta = Mockito.mock(CloudEntity.Meta.class);
        Mockito.when(client.createBuild(PACKAGE_GUID)
            .getMeta())
            .thenReturn(meta);
        Mockito.when(client.createBuild(PACKAGE_GUID)
            .getMeta()
            .getGuid())
            .thenReturn(BUILD_GUID);

        StepPhase stepPhase = applicationStager.stageApp(context, client, app, stepLogger);

        assertEquals(stepPhase, StepPhase.POLL);
        Mockito.verify(context)
            .setVariable(Constants.VAR_BUILD_GUID, BUILD_GUID);
        Mockito.verify(stepLogger)
            .info(Messages.STAGING_APP, APP_NAME);
    }

    @Test
    public void testIfBuildGuidDoesNotExist() {
        Mockito.when(execution.getContext()
            .getVariable(Constants.VAR_BUILD_GUID))
            .thenReturn(null);
        
        StagingState stagingState = applicationStager.getStagingState(execution, client);
        
        assertEquals(PackageState.STAGED, stagingState.getState());
        assertNull(stagingState.getError());
    }

}