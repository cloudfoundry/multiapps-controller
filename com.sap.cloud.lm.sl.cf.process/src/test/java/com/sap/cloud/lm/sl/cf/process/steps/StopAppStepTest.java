package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Arrays;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.steps.ScaleAppStepTest.SimpleApplication;

@RunWith(Parameterized.class)
public class StopAppStepTest extends SyncFlowableStepTest<StopAppStep> {

    private SimpleApplicationWithState application;
    private SimpleApplicationWithState existingApplication;

    private boolean shouldBeStopped;

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
            // @formatter:off
            {
                new SimpleApplicationWithState("test-app-1", 0, AppState.STARTED), new SimpleApplicationWithState("test-app-1", 0, AppState.STARTED)
            },
            {
                new SimpleApplicationWithState("test-app-1", 0, AppState.STARTED), new SimpleApplicationWithState("test-app-1", 0, AppState.STOPPED)
            },
            {
                new SimpleApplicationWithState("test-app-1", 0, AppState.STOPPED), new SimpleApplicationWithState("test-app-1", 0, AppState.STOPPED)
            },
            {
                new SimpleApplicationWithState("test-app-1", 0, AppState.STOPPED), new SimpleApplicationWithState("test-app-1", 0, AppState.STARTED)
            },
            // @formatter:on
        });
    }

    public StopAppStepTest(SimpleApplicationWithState application, SimpleApplicationWithState existingApplication) {
        this.application = application;
        this.existingApplication = existingApplication;
    }

    @Before
    public void setUp() throws Exception {
        prepareContext();
        determineActionForApplication();
    }

    @Test
    public void testExecute() throws Exception {
        step.execute(context);

        assertStepFinishedSuccessfully();

        validateStoppedApplications();
    }

    private void determineActionForApplication() {
        if (existingApplication.state != AppState.STOPPED) {
            shouldBeStopped = true;
        } else {
            shouldBeStopped = false;
        }
    }

    private void prepareContext() {
        context.setVariable(Constants.VAR_MODULES_INDEX, 0);
        StepsTestUtil.mockApplicationsToDeploy(toCloudApplication(), context);
        StepsUtil.setExistingApp(context, (existingApplication != null) ? existingApplication.toCloudApplication() : null);
    }

    List<CloudApplicationExtended> toCloudApplication() {
        return Arrays.asList(application.toCloudApplication());
    }

    private void validateStoppedApplications() {
        String appName = application.name;
        if (shouldBeStopped) {
            Mockito.verify(client)
                .stopApplication(appName);
        } else {
            Mockito.verify(client, Mockito.times(0))
                .stopApplication(appName);
        }
    }

    private static class SimpleApplicationWithState extends SimpleApplication {
        AppState state;

        public SimpleApplicationWithState(String name, int instances, AppState state) {
            super(name, instances);
            this.state = state;
        }

        @Override
        CloudApplicationExtended toCloudApplication() {
            CloudApplicationExtended app = new CloudApplicationExtended(null, name);
            app.setState(state);
            return app;
        }
    }

    @Override
    protected StopAppStep createStep() {
        return new StopAppStep();
    }

}
