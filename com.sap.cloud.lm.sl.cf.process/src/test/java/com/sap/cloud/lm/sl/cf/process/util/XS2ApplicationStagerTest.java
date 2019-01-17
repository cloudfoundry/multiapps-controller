package com.sap.cloud.lm.sl.cf.process.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.StartingInfo;
import org.cloudfoundry.client.lib.domain.PackageState;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.steps.ExecutionWrapper;
import com.sap.cloud.lm.sl.cf.process.steps.StepsUtil;
import com.sap.cloud.lm.sl.cf.process.util.ApplicationStager;
import com.sap.cloud.lm.sl.cf.process.util.StagingState;
import com.sap.cloud.lm.sl.cf.process.util.StagingState.StagingLogs;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.Pair;

public class XS2ApplicationStagerTest {

    private ApplicationStager applicationStager;

    private CloudControllerClient client;

    private ExecutionWrapper execution;

    @Before
    public void initFakeDependencies() {
        client = Mockito.mock(CloudControllerClient.class);
        applicationStager = new XS2ApplicationStager();
        execution = Mockito.mock(ExecutionWrapper.class);
        DelegateExecution context = Mockito.mock(DelegateExecution.class);
        Mockito.when(context.getVariable(Constants.VAR_STARTING_INFO_CLASSNAME))
            .thenReturn("org.cloudfoundry.client.lib.StartingInfo");
        Mockito.when(context.getVariable(Constants.VAR_OFFSET))
            .thenReturn(0);
        StartingInfo startingInfo = new StartingInfo("");
        Mockito.when(context.getVariable(Constants.VAR_STARTING_INFO))
            .thenReturn(JsonUtil.toJson(startingInfo)
                .getBytes());
        Mockito.when(execution.getContext())
            .thenReturn(context);

        Mockito.when(execution.getControllerClient())
            .thenReturn(client);
    }

    @Test
    public void testStagingLogsAreNullPackageStateStagedExpected() {
        StagingState stagingState = applicationStager.getStagingState(execution, client);

        assertEquals(PackageState.STAGED, stagingState.getState());
    }

    @Test
    public void testStagingLogsAreNullPackageStatePendingExpected() {
        Mockito.when(client.getStagingLogs(Mockito.any(StartingInfo.class), Mockito.any(Integer.class)))
            .thenReturn("Test");

        StagingState stagingState = applicationStager.getStagingState(execution, client);

        assertEquals(PackageState.PENDING, stagingState.getState());
        assertEquals("Test", stagingState.getStagingLogs()
            .getLogs());
        assertEquals(0, stagingState.getStagingLogs()
            .getOffset());
    }

    @Test
    public void testStagingLogsAreNullPackageStateFailedExpected() {
        Mockito.when(client.getStagingLogs(Mockito.any(StartingInfo.class), Mockito.any(Integer.class)))
            .thenThrow(new CloudOperationException(HttpStatus.INTERNAL_SERVER_ERROR));

        StagingState stagingState = applicationStager.getStagingState(execution, client);

        assertEquals(PackageState.FAILED, stagingState.getState());
    }

    @Test
    public void testStagingLogsAreNullPackageStateStagedExpectedWithException() {
        Mockito.when(client.getStagingLogs(Mockito.any(StartingInfo.class), Mockito.any(Integer.class)))
            .thenThrow(new CloudOperationException(HttpStatus.BAD_REQUEST));

        StagingState stagingState = applicationStager.getStagingState(execution, client);

        assertEquals(PackageState.STAGED, stagingState.getState());
    }

}
