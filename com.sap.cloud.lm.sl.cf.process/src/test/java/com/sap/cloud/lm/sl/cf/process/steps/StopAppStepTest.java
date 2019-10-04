package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Arrays;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudApplication.State;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ImmutableCloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.steps.ScaleAppStepTest.SimpleApplication;
import com.sap.cloud.lm.sl.cf.process.util.ProcessTypeParser;
import com.sap.cloud.lm.sl.cf.web.api.model.ProcessType;

@RunWith(Parameterized.class)
public class StopAppStepTest extends SyncFlowableStepTest<StopAppStep> {

    private final SimpleApplicationWithState application;
    private final SimpleApplicationWithState existingApplication;

    @Mock
    private ProcessTypeParser processTypeParser;

    private boolean shouldBeStopped;

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
            // @formatter:off
            {
                new SimpleApplicationWithState("test-app-1", 0, State.STARTED), new SimpleApplicationWithState("test-app-1", 0, State.STARTED)
            },
            {
                new SimpleApplicationWithState("test-app-1", 0, State.STARTED), new SimpleApplicationWithState("test-app-1", 0, State.STOPPED)
            },
            {
                new SimpleApplicationWithState("test-app-1", 0, State.STOPPED), new SimpleApplicationWithState("test-app-1", 0, State.STOPPED)
            },
            {
                new SimpleApplicationWithState("test-app-1", 0, State.STOPPED), new SimpleApplicationWithState("test-app-1", 0, State.STARTED)
            },
            // @formatter:on
        });
    }

    public StopAppStepTest(SimpleApplicationWithState application, SimpleApplicationWithState existingApplication) {
        this.application = application;
        this.existingApplication = existingApplication;
    }

    @Before
    public void setUp() {

        prepareContext();
        determineActionForApplication();

        Mockito.when(processTypeParser.getProcessType(Mockito.any()))
               .thenReturn(ProcessType.DEPLOY);
    }

    @Test
    public void testExecute() {
        step.execute(context);

        assertStepFinishedSuccessfully();

        validateStoppedApplications();
    }

    private void determineActionForApplication() {
        shouldBeStopped = existingApplication.state != State.STOPPED;
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
        final State state;

        public SimpleApplicationWithState(String name, int instances, State state) {
            super(name, instances);
            this.state = state;
        }

        @Override
        CloudApplicationExtended toCloudApplication() {
            return ImmutableCloudApplicationExtended.builder()
                                                    .name(name)
                                                    .state(state)
                                                    .build();
        }
    }

    @Override
    protected StopAppStep createStep() {
        return new StopAppStep();
    }

}
