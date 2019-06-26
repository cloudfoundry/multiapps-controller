package com.sap.cloud.lm.sl.cf.process.steps;

import com.sap.cloud.lm.sl.cf.client.lib.domain.RestartParameters;
import com.sap.cloud.lm.sl.cf.core.cf.apps.ApplicationStartupState;
import com.sap.cloud.lm.sl.cf.core.cf.apps.ApplicationStateAction;
import com.sap.cloud.lm.sl.cf.process.Constants;
import org.cloudfoundry.client.lib.domain.CloudBuild;
import org.cloudfoundry.client.lib.domain.CloudBuild.State;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class DetermineDesiredStateAchievingActionsStepTest extends DetermineDesiredStateAchievingActionsStepBaseTest {

    private final Class<? extends Exception> exception;

    public DetermineDesiredStateAchievingActionsStepTest(ApplicationStartupState currentAppState, ApplicationStartupState desiredAppState,
        boolean hasAppChanged, Set<ApplicationStateAction> expectedAppStateActions, List<CloudBuild> cloudBuilds,
        Class<? extends Exception> exception) {
        super(currentAppState, desiredAppState, hasAppChanged, expectedAppStateActions, cloudBuilds);
        this.exception = exception;
    }

    @Parameters
    public static List<Object[]> getParameters() throws ParseException {
        return Arrays.asList(new Object[][] {
            // @formatter:off
            // (0)
            {
                ApplicationStartupState.STOPPED, ApplicationStartupState.STOPPED, false, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.STOP).collect(Collectors.toSet()), Arrays.asList(createCloudBuild(State.STAGED, parseDate("20-03-2018"), null), createCloudBuild(State.FAILED, parseDate("21-03-2018"), FAKE_ERROR)), null
            },
            // (1)
            {
                ApplicationStartupState.STOPPED, ApplicationStartupState.STOPPED, false, Collections.emptySet(), Arrays.asList(createCloudBuild(State.STAGED, parseDate("20-03-2018"), null), createCloudBuild(State.FAILED, parseDate("18-03-2018"), null)), null
            },
            // (2)
            {
                ApplicationStartupState.STOPPED, ApplicationStartupState.STOPPED, true, Stream.of(ApplicationStateAction.STAGE).collect(Collectors.toSet()), Arrays.asList(createCloudBuild(State.STAGED, parseDate("20-03-2018"), null), createCloudBuild(State.FAILED, parseDate("21-03-2018"), FAKE_ERROR)), null
            },
            // (3)
            {
                ApplicationStartupState.STOPPED, ApplicationStartupState.STOPPED, true, Stream.of(ApplicationStateAction.STAGE).collect(Collectors.toSet()), Arrays.asList(createCloudBuild(State.STAGED, parseDate("20-03-2018"), null), createCloudBuild(State.FAILED, parseDate("18-03-2018"), FAKE_ERROR)), null
            },
            // (4)
            {
                ApplicationStartupState.STOPPED, ApplicationStartupState.STARTED, false, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.START).collect(Collectors.toSet()), Arrays.asList(createCloudBuild(State.STAGED, parseDate("20-03-2018"), null), createCloudBuild(State.FAILED, parseDate("21-03-2018"), FAKE_ERROR)), null
            },
            // (5)
            {
                ApplicationStartupState.STOPPED, ApplicationStartupState.STARTED, false, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.START).collect(Collectors.toSet()), Arrays.asList(createCloudBuild(State.STAGED, parseDate("20-03-2018"), null), createCloudBuild(State.STAGED, parseDate("21-03-2018"), null)), null
            },
            // (6)
            {
                ApplicationStartupState.STOPPED, ApplicationStartupState.STARTED, true, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.START).collect(Collectors.toSet()), Arrays.asList(createCloudBuild(State.STAGED, parseDate("20-03-2018"), null), createCloudBuild(State.FAILED, parseDate("21-03-2018"), FAKE_ERROR)), null
            },
            // (7)
            {
                ApplicationStartupState.STOPPED, ApplicationStartupState.STARTED, true, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.START).collect(Collectors.toSet()), Arrays.asList(createCloudBuild(State.STAGED, parseDate("20-03-2018"), null), createCloudBuild(State.STAGED, parseDate("21-03-2018"), null)), null
            },
            // (8)
            {
                ApplicationStartupState.STOPPED, ApplicationStartupState.EXECUTED, false, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.START, ApplicationStateAction.EXECUTE).collect(Collectors.toSet()), Arrays.asList(createCloudBuild(State.STAGED, parseDate("20-03-2018"), null), createCloudBuild(State.FAILED, parseDate("21-03-2018"), FAKE_ERROR)), null
            },
            // (9)
            {
                ApplicationStartupState.STOPPED, ApplicationStartupState.EXECUTED, false, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.START, ApplicationStateAction.EXECUTE).collect(Collectors.toSet()), Arrays.asList(createCloudBuild(State.STAGED, parseDate("20-03-2018"), null), createCloudBuild(State.STAGED, parseDate("21-03-2018"), null)), null
            },
            // (10)
            {
                ApplicationStartupState.STOPPED, ApplicationStartupState.EXECUTED, true, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.START, ApplicationStateAction.EXECUTE).collect(Collectors.toSet()), Arrays.asList(createCloudBuild(State.STAGED, parseDate("20-03-2018"), null), createCloudBuild(State.FAILED, parseDate("21-03-2018"), FAKE_ERROR)), null
            },
            // (11)
            {
                ApplicationStartupState.STOPPED, ApplicationStartupState.EXECUTED, true, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.START, ApplicationStateAction.EXECUTE).collect(Collectors.toSet()), Arrays.asList(createCloudBuild(State.STAGED, parseDate("20-03-2018"), null), createCloudBuild(State.STAGED, parseDate("21-03-2018"), null)), null
            },
            // (12)
            {
                ApplicationStartupState.STARTED, ApplicationStartupState.STOPPED, false, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.STOP).collect(Collectors.toSet()), Arrays.asList(createCloudBuild(State.STAGED, parseDate("20-03-2018"), null), createCloudBuild(State.FAILED, parseDate("21-03-2018"), FAKE_ERROR)), null
            },
            // (13)
            {
                ApplicationStartupState.STARTED, ApplicationStartupState.STOPPED, false, Stream.of(ApplicationStateAction.STOP).collect(Collectors.toSet()), Arrays.asList(createCloudBuild(State.STAGED, parseDate("20-03-2018"), null), createCloudBuild(State.STAGED, parseDate("21-03-2018"), null)), null
            },
            // (14)
            {
                ApplicationStartupState.STARTED, ApplicationStartupState.STOPPED, true, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.STOP).collect(Collectors.toSet()), Arrays.asList(createCloudBuild(State.STAGED, parseDate("20-03-2018"), null), createCloudBuild(State.FAILED, parseDate("21-03-2018"), FAKE_ERROR)), null
            },
            // (15)
            {
                ApplicationStartupState.STARTED, ApplicationStartupState.STOPPED, true, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.STOP).collect(Collectors.toSet()), Arrays.asList(createCloudBuild(State.STAGED, parseDate("20-03-2018"), null), createCloudBuild(State.STAGED, parseDate("21-03-2018"), null)), null
            },
            // (16)
            {
                ApplicationStartupState.STARTED, ApplicationStartupState.STARTED, false, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.STOP, ApplicationStateAction.START).collect(Collectors.toSet()), Arrays.asList(createCloudBuild(State.STAGED, parseDate("20-03-2018"), null), createCloudBuild(State.FAILED, parseDate("21-03-2018"), FAKE_ERROR)), null
            },
            // (17)
            {
                ApplicationStartupState.STARTED, ApplicationStartupState.STARTED, false, Collections.emptySet(), Arrays.asList(createCloudBuild(State.STAGED, parseDate("20-03-2018"), null), createCloudBuild(State.STAGED, parseDate("21-03-2018"), null)), null
            },
            // (18)
            {
                ApplicationStartupState.STARTED, ApplicationStartupState.STARTED, true, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.START, ApplicationStateAction.STOP).collect(Collectors.toSet()), Arrays.asList(createCloudBuild(State.STAGED, parseDate("20-03-2018"), null), createCloudBuild(State.FAILED, parseDate("21-03-2018"), FAKE_ERROR)), null
            },
            // (19)
            {
                ApplicationStartupState.STARTED, ApplicationStartupState.STARTED, true, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.START, ApplicationStateAction.STOP).collect(Collectors.toSet()), Arrays.asList(createCloudBuild(State.STAGED, parseDate("20-03-2018"), null), createCloudBuild(State.STAGED, parseDate("21-03-2018"), null)), null
            },
            // (20)
            {
                ApplicationStartupState.STARTED, ApplicationStartupState.EXECUTED, false, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.STOP, ApplicationStateAction.EXECUTE).collect(Collectors.toSet()), Arrays.asList(createCloudBuild(State.STAGED, parseDate("20-03-2018"), null), createCloudBuild(State.FAILED, parseDate("21-03-2018"), FAKE_ERROR)), null
            },
            // (21)
            {
                ApplicationStartupState.STARTED, ApplicationStartupState.EXECUTED, false, Stream.of(ApplicationStateAction.STOP, ApplicationStateAction.EXECUTE).collect(Collectors.toSet()), Arrays.asList(createCloudBuild(State.STAGED, parseDate("20-03-2018"), null), createCloudBuild(State.STAGED, parseDate("21-03-2018"), null)), null
            },
            // (22)
            {
                ApplicationStartupState.STARTED, ApplicationStartupState.EXECUTED, true, Stream.of(ApplicationStateAction.STOP, ApplicationStateAction.STAGE, ApplicationStateAction.EXECUTE, ApplicationStateAction.START).collect(Collectors.toSet()), Arrays.asList(createCloudBuild(State.STAGED, parseDate("20-03-2018"), null), createCloudBuild(State.FAILED, parseDate("21-03-2018"), FAKE_ERROR)), null
            },
            // (23)
            {
                ApplicationStartupState.STARTED, ApplicationStartupState.EXECUTED, true, Stream.of(ApplicationStateAction.STOP, ApplicationStateAction.STAGE, ApplicationStateAction.EXECUTE, ApplicationStateAction.START).collect(Collectors.toSet()), Arrays.asList(createCloudBuild(State.FAILED, parseDate("20-03-2018"), null), createCloudBuild(State.STAGED, parseDate("21-03-2018"), null)), null
            },
            // (24)
            {
                ApplicationStartupState.INCONSISTENT, ApplicationStartupState.STOPPED, false, Stream.of(ApplicationStateAction.STOP, ApplicationStateAction.STAGE).collect(Collectors.toSet()), Arrays.asList(createCloudBuild(State.FAILED, parseDate("20-03-2018"), null), createCloudBuild(State.FAILED, parseDate("21-03-2018"), FAKE_ERROR)), null
            },
            // (25)
            {
                ApplicationStartupState.INCONSISTENT, ApplicationStartupState.STOPPED, false, Stream.of(ApplicationStateAction.STOP).collect(Collectors.toSet()), Arrays.asList(createCloudBuild(State.FAILED, parseDate("20-03-2018"), null), createCloudBuild(State.STAGED, parseDate("21-03-2018"), null)), null
            },
            // (26)
            {
                ApplicationStartupState.INCONSISTENT, ApplicationStartupState.STOPPED, true, Stream.of(ApplicationStateAction.STOP, ApplicationStateAction.STAGE).collect(Collectors.toSet()), Arrays.asList(createCloudBuild(State.FAILED, parseDate("20-03-2018"), null), createCloudBuild(State.FAILED, parseDate("21-03-2018"), FAKE_ERROR)), null
            },
            // (27)
            {
                ApplicationStartupState.INCONSISTENT, ApplicationStartupState.STOPPED, true, Stream.of(ApplicationStateAction.STOP, ApplicationStateAction.STAGE).collect(Collectors.toSet()), Arrays.asList(createCloudBuild(State.FAILED, parseDate("20-03-2018"), null), createCloudBuild(State.STAGED, parseDate("21-03-2018"), null)), null
            },
            // (28)
            {
                ApplicationStartupState.INCONSISTENT, ApplicationStartupState.STARTED, false, Stream.of(ApplicationStateAction.STOP, ApplicationStateAction.STAGE, ApplicationStateAction.START).collect(Collectors.toSet()), Arrays.asList(createCloudBuild(State.FAILED, parseDate("20-03-2018"), null), createCloudBuild(State.FAILED, parseDate("21-03-2018"), null)), null
            },
            // (29)
            {
                ApplicationStartupState.INCONSISTENT, ApplicationStartupState.STARTED, false, Stream.of(ApplicationStateAction.STOP, ApplicationStateAction.STAGE, ApplicationStateAction.START).collect(Collectors.toSet()), Arrays.asList(createCloudBuild(State.STAGED, parseDate("20-03-2018"), null), createCloudBuild(State.STAGED, parseDate("21-03-2018"), null)), null
            },
            // (30)
            {
                ApplicationStartupState.INCONSISTENT, ApplicationStartupState.STARTED, true, Stream.of(ApplicationStateAction.STOP, ApplicationStateAction.STAGE, ApplicationStateAction.START).collect(Collectors.toSet()), Arrays.asList(createCloudBuild(State.FAILED, parseDate("20-03-2018"), null), createCloudBuild(State.FAILED, parseDate("21-03-2018"), null)), null
            },
            // (31)
            {
                ApplicationStartupState.INCONSISTENT, ApplicationStartupState.STARTED, true, Stream.of(ApplicationStateAction.STOP, ApplicationStateAction.STAGE, ApplicationStateAction.START).collect(Collectors.toSet()), Arrays.asList(createCloudBuild(State.STAGED, parseDate("20-03-2018"), null), createCloudBuild(State.STAGED, parseDate("21-03-2018"), null)), null
            },
            // (32)
            {
                ApplicationStartupState.INCONSISTENT, ApplicationStartupState.EXECUTED, false, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.EXECUTE, ApplicationStateAction.START, ApplicationStateAction.STOP).collect(Collectors.toSet()), Arrays.asList(createCloudBuild(State.FAILED, parseDate("20-03-2018"), null), createCloudBuild(State.FAILED, parseDate("21-03-2018"), null)), null
            },
            // (33)
            {
                ApplicationStartupState.INCONSISTENT, ApplicationStartupState.EXECUTED, false, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.EXECUTE, ApplicationStateAction.START, ApplicationStateAction.STOP).collect(Collectors.toSet()), Arrays.asList(createCloudBuild(State.FAILED, parseDate("20-03-2018"), null), createCloudBuild(State.STAGED, parseDate("21-03-2018"), null)), null
            },
            // (34)
            {
                ApplicationStartupState.INCONSISTENT, ApplicationStartupState.EXECUTED, true, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.EXECUTE, ApplicationStateAction.START, ApplicationStateAction.STOP).collect(Collectors.toSet()), Arrays.asList(createCloudBuild(State.FAILED, parseDate("20-03-2018"), null), createCloudBuild(State.FAILED, parseDate("21-03-2018"), null)), null
            },
            // (35)
            {
                ApplicationStartupState.INCONSISTENT, ApplicationStartupState.EXECUTED, true, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.EXECUTE, ApplicationStateAction.START, ApplicationStateAction.STOP).collect(Collectors.toSet()), Arrays.asList(createCloudBuild(State.FAILED, parseDate("20-03-2018"), null), createCloudBuild(State.STAGED, parseDate("21-03-2018"), null)), null
            },
            // (36)
            {
                ApplicationStartupState.STARTED, ApplicationStartupState.STARTED, true, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.START, ApplicationStateAction.STOP).collect(Collectors.toSet()), Arrays.asList(createCloudBuild(State.STAGED, parseDate("20-03-2018"), null), createCloudBuild(State.STAGED, null, null)), null
            },
        });
    }

    @Test
    public void testExecute() throws Exception {
        if (exception != null) {
            expectedException.expect(exception);
        }
        step.execute(context);
        assertStepFinishedSuccessfully();

        assertEquals(expectedAppStateActions, StepsUtil.getAppStateActionsToExecute(context));
    }

    @Before
    public void setUpProperties() {
        context.setVariable(Constants.VAR_VCAP_APP_PROPERTIES_CHANGED, false);
        context.setVariable(Constants.VAR_VCAP_SERVICES_PROPERTIES_CHANGED, false);
        context.setVariable(Constants.VAR_USER_PROPERTIES_CHANGED, false);        
    }

    @Override
    protected RestartParameters getRestartParameters() {
        return new RestartParameters(false, false, false);
    }

    @RunWith(Parameterized.class)
    public static class DetermineAppRestartTest extends DetermineDesiredStateAchievingActionsStepBaseTest {

        private static final ApplicationStartupState STARTED_APPLICATION_STARTUP_STATE = ApplicationStartupState.STARTED;
        private static final boolean HAS_APP_CHANGED = false;
        private boolean vcapPropertiesChanged;
        private boolean vcapServicesChanged;
        private boolean userPropertiesChanged;
        private boolean shouldRestartOnVcapAppChange;
        private boolean shouldRestartOnVcapServicesChange;
        private boolean shouldRestartOnUserProvidedChange;
        public DetermineAppRestartTest(ApplicationStartupState currentAppState, ApplicationStartupState desiredAppState, Set<ApplicationStateAction> expectedAppStateActions, List<CloudBuild> cloudBuilds, boolean vcapPropertiesChanged, boolean vcapServicesChanged, boolean userPropertiesChanged , boolean shouldRestartOnVcapAppChange, boolean shouldRestartOnVcapServicesChange,  boolean shouldRestartOnUserProvidedChange) {
            super(currentAppState, desiredAppState, HAS_APP_CHANGED, expectedAppStateActions, cloudBuilds);
            this.vcapPropertiesChanged = vcapPropertiesChanged;
            this.vcapServicesChanged = vcapServicesChanged;
            this.userPropertiesChanged = userPropertiesChanged;
            this.shouldRestartOnVcapAppChange = shouldRestartOnVcapAppChange;
            this.shouldRestartOnVcapServicesChange = shouldRestartOnVcapServicesChange;
            this.shouldRestartOnUserProvidedChange = shouldRestartOnUserProvidedChange;
        }

        // Staging is always required, because there are no previous builds
        @Parameters
        public static List<Object[]> getParameters() throws ParseException {
            return Arrays.asList(new Object[][] {
                // @formatter:off
                // (0)
                {
                    STARTED_APPLICATION_STARTUP_STATE, STARTED_APPLICATION_STARTUP_STATE, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.START, ApplicationStateAction.STOP).collect(Collectors.toSet()), Collections.emptyList(), true, true, true, true, true, true
                },
                // (1)
                {
                    STARTED_APPLICATION_STARTUP_STATE, STARTED_APPLICATION_STARTUP_STATE, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.START, ApplicationStateAction.STOP).collect(Collectors.toSet()), Collections.emptyList(), true, false, false, true, false, false
                },
                // (2)
                {
                    STARTED_APPLICATION_STARTUP_STATE, STARTED_APPLICATION_STARTUP_STATE, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.START, ApplicationStateAction.STOP).collect(Collectors.toSet()), Collections.emptyList(), false, true, false, false, true, false
                },
                // (3)
                {
                    STARTED_APPLICATION_STARTUP_STATE, STARTED_APPLICATION_STARTUP_STATE, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.START, ApplicationStateAction.STOP).collect(Collectors.toSet()), Collections.emptyList(), false, false, true, false, false, true
                },
                // (4)
                {
                    STARTED_APPLICATION_STARTUP_STATE, STARTED_APPLICATION_STARTUP_STATE, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.START, ApplicationStateAction.STOP).collect(Collectors.toSet()), Collections.emptyList(), false, false, false, false, false, false
                },
                // (5)
                {
                    STARTED_APPLICATION_STARTUP_STATE, STARTED_APPLICATION_STARTUP_STATE, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.START, ApplicationStateAction.STOP).collect(Collectors.toSet()), Collections.emptyList(), true, false, false, false, false, false
                },
                // (6)
                {
                    STARTED_APPLICATION_STARTUP_STATE, STARTED_APPLICATION_STARTUP_STATE, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.START, ApplicationStateAction.STOP).collect(Collectors.toSet()), Collections.emptyList(), false, true, false, false, false, false
                },
                // (7)
                {
                    STARTED_APPLICATION_STARTUP_STATE, STARTED_APPLICATION_STARTUP_STATE, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.START, ApplicationStateAction.STOP).collect(Collectors.toSet()), Collections.emptyList(), false, false, true, false, false, false
                },
                // (8)
                {
                    STARTED_APPLICATION_STARTUP_STATE, STARTED_APPLICATION_STARTUP_STATE, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.START, ApplicationStateAction.STOP).collect(Collectors.toSet()), Collections.emptyList(), false, false, false, true, false, false
                },
                // (9)
                {
                    STARTED_APPLICATION_STARTUP_STATE, STARTED_APPLICATION_STARTUP_STATE, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.START, ApplicationStateAction.STOP).collect(Collectors.toSet()), Collections.emptyList(), false, false, false, false, true, false
                },
                // (10)
                {
                    STARTED_APPLICATION_STARTUP_STATE, STARTED_APPLICATION_STARTUP_STATE, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.START, ApplicationStateAction.STOP).collect(Collectors.toSet()), Collections.emptyList(), false, false, false, false, false, true
                },
            });
        }

        @Test
        public void testParameters() {
            step.execute(context);

            assertEquals(expectedAppStateActions, StepsUtil.getAppStateActionsToExecute(context));
        }

        @Override
        protected DetermineDesiredStateAchievingActionsStep createStep() {
            return new DetermineDesiredStateAchievingActionsStep();
        }

        @Before
        public void setUpProperties() {
            context.setVariable(Constants.VAR_VCAP_APP_PROPERTIES_CHANGED, vcapPropertiesChanged);
            context.setVariable(Constants.VAR_VCAP_SERVICES_PROPERTIES_CHANGED, vcapServicesChanged);
            context.setVariable(Constants.VAR_USER_PROPERTIES_CHANGED, userPropertiesChanged);
        }

        @Override
        protected RestartParameters getRestartParameters() {
            return new RestartParameters(shouldRestartOnVcapAppChange, shouldRestartOnVcapServicesChange, shouldRestartOnUserProvidedChange);
        }
    }
}
