package org.cloudfoundry.multiapps.controller.core.cf.apps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.MessageFormat;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ChangedApplicationActionCalculatorTest {

    private ActionCalculator applicationCalculator;

    @BeforeEach
    void setUp() {
        applicationCalculator = new ChangedApplicationActionCalculator();
    }

    static Stream<Arguments> testChangedApplicationCalculator() {
        return Stream.of(
            //@formatter:off
                        Arguments.of(ApplicationStartupState.STOPPED, ApplicationStartupState.STARTED, List.of(ApplicationStateAction.STAGE, ApplicationStateAction.START)),
                        Arguments.of(ApplicationStartupState.STOPPED, ApplicationStartupState.STOPPED, List.of(ApplicationStateAction.STAGE)),
                        Arguments.of(ApplicationStartupState.INCONSISTENT, ApplicationStartupState.STARTED,
                                     List.of(ApplicationStateAction.STOP, ApplicationStateAction.STAGE, ApplicationStateAction.START)),
                        Arguments.of(ApplicationStartupState.INCONSISTENT, ApplicationStartupState.STOPPED, List.of(ApplicationStateAction.STAGE, ApplicationStateAction.STOP)),
                        Arguments.of(ApplicationStartupState.STARTED, ApplicationStartupState.STARTED, List.of(ApplicationStateAction.STAGE, ApplicationStateAction.STOP, ApplicationStateAction.START)),
                        Arguments.of(ApplicationStartupState.STARTED, ApplicationStartupState.STOPPED, List.of(ApplicationStateAction.STOP, ApplicationStateAction.STAGE)),
                        Arguments.of(ApplicationStartupState.INCONSISTENT, ApplicationStartupState.EXECUTED, 
                                     List.of(ApplicationStateAction.STOP, ApplicationStateAction.STAGE, ApplicationStateAction.EXECUTE, ApplicationStateAction.START)),
                        Arguments.of(ApplicationStartupState.STOPPED, ApplicationStartupState.EXECUTED, 
                                     List.of(ApplicationStateAction.STAGE, ApplicationStateAction.START, ApplicationStateAction.EXECUTE)),
                        Arguments.of(ApplicationStartupState.STARTED, ApplicationStartupState.EXECUTED,
                                     List.of(ApplicationStateAction.STOP, ApplicationStateAction.STAGE, ApplicationStateAction.START, ApplicationStateAction.EXECUTE))
        //@formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    void testChangedApplicationCalculator(ApplicationStartupState currentState, ApplicationStartupState desiredState,
                                          List<ApplicationStateAction> expectedApplicationActions) {
        Set<ApplicationStateAction> actionsToExecute = applicationCalculator.determineActionsToExecute(currentState, desiredState, true);

        assertEquals(expectedApplicationActions.size(), actionsToExecute.size(),
                     MessageFormat.format("Expected actions {0} but they are {1}", expectedApplicationActions, actionsToExecute));
        assertTrue(expectedApplicationActions.containsAll(actionsToExecute),
                   MessageFormat.format("Expected actions {0} but they are {1}", expectedApplicationActions, actionsToExecute));

    }
}
