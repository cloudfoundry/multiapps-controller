package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.controller.client.lib.domain.RestartParameters;
import org.cloudfoundry.multiapps.controller.core.cf.apps.ApplicationStartupState;
import org.cloudfoundry.multiapps.controller.core.cf.apps.ApplicationStateAction;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DetermineDesiredStateAchievingActionsStepTest extends DetermineDesiredStateAchievingActionsStepBaseTest {

    public static Stream<Arguments> testExecute() {
        return Stream.of(
        // @formatter:off
            // (0)
            Arguments.of(ApplicationStartupState.STOPPED, ApplicationStartupState.STOPPED, false, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.STOP).collect(Collectors.toList()), true, null),
            // (1)
            Arguments.of(ApplicationStartupState.STOPPED, ApplicationStartupState.STOPPED, false, Collections.emptyList(), false, null),
            // (2)
            Arguments.of(ApplicationStartupState.STOPPED, ApplicationStartupState.STOPPED, true, Stream.of(ApplicationStateAction.STAGE).collect(Collectors.toList()), false, null),
            // (3)
            Arguments.of(ApplicationStartupState.STOPPED, ApplicationStartupState.STARTED, false, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.START).collect(Collectors.toList()), false, null),
            // (4)
            Arguments.of(ApplicationStartupState.STOPPED, ApplicationStartupState.EXECUTED, false, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.START, ApplicationStateAction.EXECUTE).collect(Collectors.toList()),false , null),
            // (5)
            Arguments.of(ApplicationStartupState.STARTED, ApplicationStartupState.STOPPED, false, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.STOP).collect(Collectors.toList()),true , null),
            // (6)
            Arguments.of(ApplicationStartupState.STARTED, ApplicationStartupState.STOPPED, false, Stream.of(ApplicationStateAction.STOP).collect(Collectors.toList()), false, null),
            // (7)
            Arguments.of(ApplicationStartupState.STARTED, ApplicationStartupState.STOPPED, true, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.STOP).collect(Collectors.toList()), false, null),
            // (8)
            Arguments.of(ApplicationStartupState.STARTED, ApplicationStartupState.STARTED, false, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.STOP, ApplicationStateAction.START).collect(Collectors.toList()), true, null),
            // (9)
            Arguments.of(ApplicationStartupState.STARTED, ApplicationStartupState.STARTED, false, Collections.emptyList(), false, null),
            // (10)
            Arguments.of(ApplicationStartupState.STARTED, ApplicationStartupState.STARTED, true, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.START, ApplicationStateAction.STOP).collect(Collectors.toList()),false, null),
            // (11)
            Arguments.of(ApplicationStartupState.STARTED, ApplicationStartupState.EXECUTED, false, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.STOP, ApplicationStateAction.EXECUTE).collect(Collectors.toList()), true, null),
            // (12)
            Arguments.of(ApplicationStartupState.STARTED, ApplicationStartupState.EXECUTED, false, Stream.of(ApplicationStateAction.STOP, ApplicationStateAction.EXECUTE).collect(Collectors.toList()), false, null),
            // (13)
            Arguments.of(ApplicationStartupState.STARTED, ApplicationStartupState.EXECUTED, true, Stream.of(ApplicationStateAction.STOP, ApplicationStateAction.STAGE, ApplicationStateAction.EXECUTE, ApplicationStateAction.START).collect(Collectors.toList()), false, null),
            // (14)
            Arguments.of(ApplicationStartupState.INCONSISTENT, ApplicationStartupState.STOPPED, false, Stream.of(ApplicationStateAction.STOP, ApplicationStateAction.STAGE).collect(Collectors.toList()), true, null),
            // (15)
            Arguments.of(ApplicationStartupState.INCONSISTENT, ApplicationStartupState.STOPPED, false, Stream.of(ApplicationStateAction.STOP).collect(Collectors.toList()),false, null),
            // (16)
            Arguments.of(ApplicationStartupState.INCONSISTENT, ApplicationStartupState.STOPPED, true, Stream.of(ApplicationStateAction.STOP, ApplicationStateAction.STAGE).collect(Collectors.toList()), false, null),
            // (17)
            Arguments.of(ApplicationStartupState.INCONSISTENT, ApplicationStartupState.STARTED, false, Stream.of(ApplicationStateAction.STOP, ApplicationStateAction.STAGE, ApplicationStateAction.START).collect(Collectors.toList()), false, null),
            // (18)
            Arguments.of(ApplicationStartupState.INCONSISTENT, ApplicationStartupState.STARTED, true, Stream.of(ApplicationStateAction.STOP, ApplicationStateAction.STAGE, ApplicationStateAction.START).collect(Collectors.toList()), false, null),
            // (19)
            Arguments.of(ApplicationStartupState.INCONSISTENT, ApplicationStartupState.EXECUTED, false, Stream.of(ApplicationStateAction.STAGE, ApplicationStateAction.EXECUTE, ApplicationStateAction.START, ApplicationStateAction.STOP).collect(Collectors.toList()), false, null)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testExecute(ApplicationStartupState currentAppState,
                            ApplicationStartupState desiredAppState, boolean hasAppChanged,
                            List<ApplicationStateAction> expectedAppStateActions, boolean hasCloudPacakge, Class<? extends Exception> exception) {
        initializeParameters(currentAppState, desiredAppState, hasAppChanged, hasCloudPacakge);
        if (exception != null) {
            assertThrows(exception, () -> step.execute(execution));
            return;
        }
        step.execute(execution);
        assertStepFinishedSuccessfully();
        assertEquals(new HashSet<>(expectedAppStateActions), new HashSet<>(context.getVariable(Variables.APP_STATE_ACTIONS_TO_EXECUTE)));
    }

    @BeforeEach
    public void setUpProperties() {
        context.setVariable(Variables.VCAP_APP_PROPERTIES_CHANGED, false);
        context.setVariable(Variables.VCAP_SERVICES_PROPERTIES_CHANGED, false);
        context.setVariable(Variables.USER_PROPERTIES_CHANGED, false);
    }

    @Override
    protected RestartParameters getRestartParameters() {
        return new RestartParameters(false, false, false);
    }
}
