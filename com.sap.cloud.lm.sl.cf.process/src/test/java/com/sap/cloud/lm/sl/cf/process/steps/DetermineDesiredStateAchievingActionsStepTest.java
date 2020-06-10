package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.cf.client.lib.domain.RestartParameters;
import com.sap.cloud.lm.sl.cf.core.cf.apps.ApplicationStartupState;
import com.sap.cloud.lm.sl.cf.core.cf.apps.ApplicationStateAction;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;

@RunWith(Parameterized.class)
public class DetermineDesiredStateAchievingActionsStepTest extends DetermineDesiredStateAchievingActionsStepBaseTest {

    @Parameters
    public static List<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
            // @formatter:off
            // (0)
            {
                ApplicationStartupState.STOPPED, ApplicationStartupState.STOPPED, false, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.STOP).collect(Collectors.toList()), true, null
            },
            // (1)
            {
                ApplicationStartupState.STOPPED, ApplicationStartupState.STOPPED, false, Collections.emptyList(), false, null
            },
            // (2)
            {
                ApplicationStartupState.STOPPED, ApplicationStartupState.STOPPED, true, Stream.of(ApplicationStateAction.STAGE).collect(Collectors.toList()), false, null
            },
            // (3)
            {
                ApplicationStartupState.STOPPED, ApplicationStartupState.STARTED, false, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.START).collect(Collectors.toList()), false, null
            },
            // (4)
            {
                ApplicationStartupState.STOPPED, ApplicationStartupState.EXECUTED, false, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.START, ApplicationStateAction.EXECUTE).collect(Collectors.toList()),false , null
            },
            // (5)
            {
                ApplicationStartupState.STARTED, ApplicationStartupState.STOPPED, false, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.STOP).collect(Collectors.toList()),true , null
            },
            // (6)
            {
                ApplicationStartupState.STARTED, ApplicationStartupState.STOPPED, false, Stream.of(ApplicationStateAction.STOP).collect(Collectors.toList()), false, null
            },
            // (7)
            {
                ApplicationStartupState.STARTED, ApplicationStartupState.STOPPED, true, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.STOP).collect(Collectors.toList()), false, null
            },
            // (8)
            {
                ApplicationStartupState.STARTED, ApplicationStartupState.STARTED, false, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.STOP, ApplicationStateAction.START).collect(Collectors.toList()), true, null
            },
            // (9)
            {
                ApplicationStartupState.STARTED, ApplicationStartupState.STARTED, false, Collections.emptyList(), false, null
            },
            // (10)
            {
                ApplicationStartupState.STARTED, ApplicationStartupState.STARTED, true, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.START, ApplicationStateAction.STOP).collect(Collectors.toList()),false, null
            },
            // (11)
            {
                ApplicationStartupState.STARTED, ApplicationStartupState.EXECUTED, false, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.STOP, ApplicationStateAction.EXECUTE).collect(Collectors.toList()), true, null
            },
            // (12)
            {
                ApplicationStartupState.STARTED, ApplicationStartupState.EXECUTED, false, Stream.of(ApplicationStateAction.STOP, ApplicationStateAction.EXECUTE).collect(Collectors.toList()), false, null
            },
            // (13)
            {
                ApplicationStartupState.STARTED, ApplicationStartupState.EXECUTED, true, Stream.of(ApplicationStateAction.STOP, ApplicationStateAction.STAGE, ApplicationStateAction.EXECUTE, ApplicationStateAction.START).collect(Collectors.toList()), false, null
            },
            // (14)
            {
                ApplicationStartupState.INCONSISTENT, ApplicationStartupState.STOPPED, false, Stream.of(ApplicationStateAction.STOP, ApplicationStateAction.STAGE).collect(Collectors.toList()), true, null
            },
            // (15)
            {
                ApplicationStartupState.INCONSISTENT, ApplicationStartupState.STOPPED, false, Stream.of(ApplicationStateAction.STOP).collect(Collectors.toList()),false, null
            },
            // (16)
            {
                ApplicationStartupState.INCONSISTENT, ApplicationStartupState.STOPPED, true, Stream.of(ApplicationStateAction.STOP, ApplicationStateAction.STAGE).collect(Collectors.toList()), false, null
            },
            // (17)
            {
                ApplicationStartupState.INCONSISTENT, ApplicationStartupState.STARTED, false, Stream.of(ApplicationStateAction.STOP, ApplicationStateAction.STAGE, ApplicationStateAction.START).collect(Collectors.toList()), false, null
            },
            // (18)
            {
                ApplicationStartupState.INCONSISTENT, ApplicationStartupState.STARTED, true, Stream.of(ApplicationStateAction.STOP, ApplicationStateAction.STAGE, ApplicationStateAction.START).collect(Collectors.toList()), false, null
            },
            // (19)
            {
                ApplicationStartupState.INCONSISTENT, ApplicationStartupState.EXECUTED, false, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.EXECUTE, ApplicationStateAction.START, ApplicationStateAction.STOP).collect(Collectors.toList()), false, null
            },
        });
    } 

    private final Class<? extends Exception> exception;

    public DetermineDesiredStateAchievingActionsStepTest(ApplicationStartupState currentAppState, ApplicationStartupState desiredAppState,
        boolean hasAppChanged, List<ApplicationStateAction> expectedAppStateActions,  boolean hasUploadToken,
        Class<? extends Exception> exception) {
        super(currentAppState, desiredAppState, hasAppChanged, expectedAppStateActions,  hasUploadToken);
        this.exception = exception;
    }

    @Test
    public void testExecute() {
        if (exception != null) {
            expectedException.expect(exception);
        }
        step.execute(execution);
        assertStepFinishedSuccessfully();
        assertEquals(new HashSet<>(expectedAppStateActions), new HashSet<>(context.getVariable(Variables.APP_STATE_ACTIONS_TO_EXECUTE)));
    }

    @Before
    public void setUpProperties() {
        context.setVariable(Variables.VCAP_APP_PROPERTIES_CHANGED, false);
        context.setVariable(Variables.VCAP_SERVICES_PROPERTIES_CHANGED, false);
        context.setVariable(Variables.USER_PROPERTIES_CHANGED, false);
    }

    @Override
    protected RestartParameters getRestartParameters() {
        return new RestartParameters(false, false, false);
    }

    @RunWith(Parameterized.class)
    public static class DetermineAppRestartTest extends DetermineDesiredStateAchievingActionsStepBaseTest {

        private static final ApplicationStartupState STARTED_APPLICATION_STARTUP_STATE = ApplicationStartupState.STARTED;
        private static final boolean HAS_APP_CHANGED = false;

        // Staging is always required, because there are no previous builds
        @Parameters
        public static List<Object[]> getParameters() {
            return Arrays.asList(new Object[][] {
                // @formatter:off
                // (0)
                {
                    STARTED_APPLICATION_STARTUP_STATE, STARTED_APPLICATION_STARTUP_STATE, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.START, ApplicationStateAction.STOP).collect(Collectors.toList()), true, true, true, true, true, true, true
                },
                // (1)
                {
                    STARTED_APPLICATION_STARTUP_STATE, STARTED_APPLICATION_STARTUP_STATE, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.START, ApplicationStateAction.STOP).collect(Collectors.toList()),true, true, false, false, true, false, false
                },
                // (2)
                {
                    STARTED_APPLICATION_STARTUP_STATE, STARTED_APPLICATION_STARTUP_STATE, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.START, ApplicationStateAction.STOP).collect(Collectors.toList()), true, false, true, false, false, true, false
                },
                // (3)
                {
                    STARTED_APPLICATION_STARTUP_STATE, STARTED_APPLICATION_STARTUP_STATE, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.START, ApplicationStateAction.STOP).collect(Collectors.toList()), true, false, false, true, false, false, true
                },
                // (4)
                {
                    STARTED_APPLICATION_STARTUP_STATE, STARTED_APPLICATION_STARTUP_STATE, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.START, ApplicationStateAction.STOP).collect(Collectors.toList()), true, false, false, false, false, false, false
                },
                // (5)
                {
                    STARTED_APPLICATION_STARTUP_STATE, STARTED_APPLICATION_STARTUP_STATE, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.START, ApplicationStateAction.STOP).collect(Collectors.toList()), true, true, false, false, false, false, false
                },
                // (6)
                {
                    STARTED_APPLICATION_STARTUP_STATE, STARTED_APPLICATION_STARTUP_STATE, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.START, ApplicationStateAction.STOP).collect(Collectors.toList()), true, false, true, false, false, false, false
                },
                // (7)
                {
                    STARTED_APPLICATION_STARTUP_STATE, STARTED_APPLICATION_STARTUP_STATE, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.START, ApplicationStateAction.STOP).collect(Collectors.toList()), true, false, false, true, false, false, false
                },
                // (8)
                {
                    STARTED_APPLICATION_STARTUP_STATE, STARTED_APPLICATION_STARTUP_STATE, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.START, ApplicationStateAction.STOP).collect(Collectors.toList()), true, false, false, false, true, false, false
                },
                // (9)
                {
                    STARTED_APPLICATION_STARTUP_STATE, STARTED_APPLICATION_STARTUP_STATE, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.START, ApplicationStateAction.STOP).collect(Collectors.toList()),true, false, false, false, false, true, false
                },
                // (10)
                {
                    STARTED_APPLICATION_STARTUP_STATE, STARTED_APPLICATION_STARTUP_STATE, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.START, ApplicationStateAction.STOP).collect(Collectors.toList()), true, false, false, false, false, false, true
                },
            });
        }

        private final boolean vcapPropertiesChanged;
        private final boolean vcapServicesChanged;
        private final boolean userPropertiesChanged;
        private final boolean shouldRestartOnVcapAppChange;
        private final boolean shouldRestartOnVcapServicesChange;
        private final boolean shouldRestartOnUserProvidedChange;

        public DetermineAppRestartTest(ApplicationStartupState currentAppState, ApplicationStartupState desiredAppState, List<ApplicationStateAction> expectedAppStateActions, boolean hasUploadToken, boolean vcapPropertiesChanged, boolean vcapServicesChanged, boolean userPropertiesChanged , boolean shouldRestartOnVcapAppChange, boolean shouldRestartOnVcapServicesChange,  boolean shouldRestartOnUserProvidedChange) {
            super(currentAppState, desiredAppState, HAS_APP_CHANGED, expectedAppStateActions, hasUploadToken);
            this.vcapPropertiesChanged = vcapPropertiesChanged;
            this.vcapServicesChanged = vcapServicesChanged;
            this.userPropertiesChanged = userPropertiesChanged;
            this.shouldRestartOnVcapAppChange = shouldRestartOnVcapAppChange;
            this.shouldRestartOnVcapServicesChange = shouldRestartOnVcapServicesChange;
            this.shouldRestartOnUserProvidedChange = shouldRestartOnUserProvidedChange;
        }

        @Test
        public void testParameters() {
            step.execute(execution);

            assertTrue(expectedAppStateActions.containsAll(context.getVariable(Variables.APP_STATE_ACTIONS_TO_EXECUTE)), "Not all expectedAppStateActions were returned");
        }

        @Override
        protected DetermineDesiredStateAchievingActionsStep createStep() {
            return new DetermineDesiredStateAchievingActionsStep();
        }

        @Before
        public void setUpProperties() {
            context.setVariable(Variables.VCAP_APP_PROPERTIES_CHANGED, vcapPropertiesChanged);
            context.setVariable(Variables.VCAP_SERVICES_PROPERTIES_CHANGED, vcapServicesChanged);
            context.setVariable(Variables.USER_PROPERTIES_CHANGED, userPropertiesChanged);
        }

        @Override
        protected RestartParameters getRestartParameters() {
            return new RestartParameters(shouldRestartOnVcapAppChange, shouldRestartOnVcapServicesChange, shouldRestartOnUserProvidedChange);
        }
    }
}
