package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;

import org.cloudfoundry.client.lib.StartingInfo;
import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

public class RestartAppStepTest extends SyncFlowableStepTest<RestartAppStep> {

    private static final String APP_NAME = "foo";

    @Before
    public void setUp() {
        context.setVariable(Constants.VAR_MODULES_INDEX, 0);
    }

    @Test
    public void testExecuteWhenAppIsStopped() throws Exception {
        CloudApplicationExtended app = createApplication(APP_NAME, AppState.STOPPED);
        StartingInfo startingInfo = new StartingInfo("dummyStagingFile");
        prepareContextAndClient(app, startingInfo);

        step.execute(context);
        assertStepFinishedSuccessfully();

        Mockito.verify(client, Mockito.never())
            .stopApplication(APP_NAME);
        Mockito.verify(client, Mockito.times(1))
            .startApplication(APP_NAME, false);

        assertEquals(JsonUtil.toJson(startingInfo), JsonUtil.toJson(StepsUtil.getStartingInfo(context)));
    }

    @Override
    protected void assertStepFinishedSuccessfully() {
        assertEquals(StepPhase.POLL.toString(), getExecutionStatus());
    }

    @Test
    public void testExecuteWhenAppIsStarted() throws Exception {
        CloudApplicationExtended app = createApplication(APP_NAME, AppState.STARTED);
        StartingInfo startingInfo = new StartingInfo("dummyStagingFile");
        prepareContextAndClient(app, startingInfo);

        step.execute(context);
        assertStepFinishedSuccessfully();

        Mockito.verify(client)
            .stopApplication(APP_NAME);
        Mockito.verify(client)
            .startApplication(APP_NAME, false);

        assertEquals(JsonUtil.toJson(startingInfo), JsonUtil.toJson(StepsUtil.getStartingInfo(context)));
    }

    private CloudApplicationExtended createApplication(String name, AppState state) {
        CloudApplicationExtended app = new CloudApplicationExtended(null, name);
        app.setState(state);
        return app;
    }

    private void prepareContextAndClient(CloudApplicationExtended app, StartingInfo startingInfo) {
        Mockito.when(client.getApplication(APP_NAME))
            .thenReturn(app);
        Mockito.when(client.startApplication(APP_NAME, false))
            .thenReturn(startingInfo);
        StepsUtil.setApp(context, app);
    }

    @Override
    protected RestartAppStep createStep() {
        return new RestartAppStep();
    }

}
