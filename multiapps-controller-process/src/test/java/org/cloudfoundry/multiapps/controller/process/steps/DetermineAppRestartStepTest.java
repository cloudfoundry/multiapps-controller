package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.controller.client.lib.domain.RestartParameters;
import org.cloudfoundry.multiapps.controller.core.cf.apps.ApplicationStartupState;
import org.cloudfoundry.multiapps.controller.core.cf.apps.ApplicationStateAction;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DetermineAppRestartStepTest extends DetermineDesiredStateAchievingActionsStepBaseTest {

    private static final ApplicationStartupState STARTED_APPLICATION_STARTUP_STATE = ApplicationStartupState.STARTED;
    private static final boolean HAS_APP_CHANGED = false;

    private boolean shouldRestartOnVcapAppChange, shouldRestartOnVcapServicesChange, shouldRestartOnUserProvidedChange;

    // Staging is always required, because there are no previous builds
    public static Stream<Arguments> testParameters() {
        return Stream.of(
        // @formatter:off
                    // (0)
                    Arguments.of(STARTED_APPLICATION_STARTUP_STATE, STARTED_APPLICATION_STARTUP_STATE, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.START, ApplicationStateAction.STOP).collect(Collectors.toList()), true, true, true, true, true, true, true),
                    // (1)
                    Arguments.of(STARTED_APPLICATION_STARTUP_STATE, STARTED_APPLICATION_STARTUP_STATE, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.START, ApplicationStateAction.STOP).collect(Collectors.toList()),true, true, false, false, true, false, false),
                    // (2)
                    Arguments.of(STARTED_APPLICATION_STARTUP_STATE, STARTED_APPLICATION_STARTUP_STATE, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.START, ApplicationStateAction.STOP).collect(Collectors.toList()), true, false, true, false, false, true, false),
                    // (3)
                    Arguments.of(STARTED_APPLICATION_STARTUP_STATE, STARTED_APPLICATION_STARTUP_STATE, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.START, ApplicationStateAction.STOP).collect(Collectors.toList()), true, false, false, true, false, false, true),
                    // (4)
                    Arguments.of(STARTED_APPLICATION_STARTUP_STATE, STARTED_APPLICATION_STARTUP_STATE, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.START, ApplicationStateAction.STOP).collect(Collectors.toList()), true, false, false, false, false, false, false),
                    // (5)
                    Arguments.of(STARTED_APPLICATION_STARTUP_STATE, STARTED_APPLICATION_STARTUP_STATE, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.START, ApplicationStateAction.STOP).collect(Collectors.toList()), true, true, false, false, false, false, false),
                    // (6)
                    Arguments.of(STARTED_APPLICATION_STARTUP_STATE, STARTED_APPLICATION_STARTUP_STATE, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.START, ApplicationStateAction.STOP).collect(Collectors.toList()), true, false, true, false, false, false, false),
                    // (7)
                    Arguments.of(STARTED_APPLICATION_STARTUP_STATE, STARTED_APPLICATION_STARTUP_STATE, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.START, ApplicationStateAction.STOP).collect(Collectors.toList()), true, false, false, true, false, false, false),
                    // (8)
                    Arguments.of(STARTED_APPLICATION_STARTUP_STATE, STARTED_APPLICATION_STARTUP_STATE, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.START, ApplicationStateAction.STOP).collect(Collectors.toList()), true, false, false, false, true, false, false),
                    // (9)
                    Arguments.of(STARTED_APPLICATION_STARTUP_STATE, STARTED_APPLICATION_STARTUP_STATE, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.START, ApplicationStateAction.STOP).collect(Collectors.toList()),true, false, false, false, false, true, false),
                    // (10)
                    Arguments.of(STARTED_APPLICATION_STARTUP_STATE, STARTED_APPLICATION_STARTUP_STATE, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.START, ApplicationStateAction.STOP).collect(Collectors.toList()), true, false, false, false, false, false, true)
        // @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    void testParameters(ApplicationStartupState currentAppState, ApplicationStartupState desiredAppState,
                        List<ApplicationStateAction> expectedAppStateActions, boolean hasCloudPackage, boolean vcapPropertiesChanged,
                        boolean vcapServicesChanged, boolean userPropertiesChanged, boolean shouldRestartOnVcapAppChange,
                        boolean shouldRestartOnVcapServicesChange, boolean shouldRestartOnUserProvidedChange) {
        this.shouldRestartOnVcapAppChange = shouldRestartOnVcapAppChange;
        this.shouldRestartOnVcapServicesChange = shouldRestartOnVcapServicesChange;
        this.shouldRestartOnUserProvidedChange = shouldRestartOnUserProvidedChange;
        initializeParameters(currentAppState, desiredAppState, HAS_APP_CHANGED, hasCloudPackage);
        initializeProperties(vcapPropertiesChanged, vcapServicesChanged, userPropertiesChanged);
        step.execute(execution);
        assertTrue(expectedAppStateActions.containsAll(context.getVariable(Variables.APP_STATE_ACTIONS_TO_EXECUTE)),
                   "Not all expectedAppStateActions were returned");
    }

    @Override
    protected DetermineDesiredStateAchievingActionsStep createStep() {
        return new DetermineDesiredStateAchievingActionsStep();
    }

    private void initializeProperties(boolean vcapPropertiesChanged, boolean vcapServicesChanged, boolean userPropertiesChanged) {
        context.setVariable(Variables.VCAP_APP_PROPERTIES_CHANGED, vcapPropertiesChanged);
        context.setVariable(Variables.VCAP_SERVICES_PROPERTIES_CHANGED, vcapServicesChanged);
        context.setVariable(Variables.USER_PROPERTIES_CHANGED, userPropertiesChanged);
    }

    @Override
    protected RestartParameters getRestartParameters() {
        return new RestartParameters(shouldRestartOnVcapAppChange, shouldRestartOnVcapServicesChange, shouldRestartOnUserProvidedChange);
    }
}
