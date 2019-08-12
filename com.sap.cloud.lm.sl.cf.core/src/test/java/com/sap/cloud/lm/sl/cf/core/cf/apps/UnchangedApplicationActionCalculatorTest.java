package com.sap.cloud.lm.sl.cf.core.cf.apps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class UnchangedApplicationActionCalculatorTest {

    private ActionCalculator applicationCalculator;

    @BeforeEach
    public void setUp() {
        applicationCalculator = new UnchangedApplicationActionCalculator();
    }

    public static Stream<Arguments> testUnchangedApplicationCalculator() {
        return Stream.of(
        //@formatter:off
                        Arguments.of(ApplicationStartupState.STOPPED, ApplicationStartupState.STARTED, true, Arrays.asList(ApplicationStateAction.STAGE, ApplicationStateAction.START)),
                        
                        Arguments.of(ApplicationStartupState.STOPPED, ApplicationStartupState.STARTED, false, Arrays.asList(ApplicationStateAction.STAGE, ApplicationStateAction.START)),
                        
                        Arguments.of(ApplicationStartupState.STOPPED, ApplicationStartupState.STOPPED, true, Collections.emptyList()),
                        
                        Arguments.of(ApplicationStartupState.STOPPED, ApplicationStartupState.STOPPED, false, Arrays.asList(ApplicationStateAction.STOP, ApplicationStateAction.STAGE)),
                        
                        Arguments.of(ApplicationStartupState.INCONSISTENT, ApplicationStartupState.STARTED, true,
                                     Arrays.asList(ApplicationStateAction.STOP, ApplicationStateAction.STAGE, ApplicationStateAction.START)),
                        
                        Arguments.of(ApplicationStartupState.INCONSISTENT, ApplicationStartupState.STARTED, false,
                                     Arrays.asList(ApplicationStateAction.STOP, ApplicationStateAction.STAGE, ApplicationStateAction.START)),
                        
                        Arguments.of(ApplicationStartupState.INCONSISTENT, ApplicationStartupState.STOPPED, true, Arrays.asList(ApplicationStateAction.STOP)),
                        
                        Arguments.of(ApplicationStartupState.INCONSISTENT, ApplicationStartupState.STOPPED, false, Arrays.asList(ApplicationStateAction.STAGE, ApplicationStateAction.STOP)),
                        
                        Arguments.of(ApplicationStartupState.STARTED, ApplicationStartupState.STARTED, true, Collections.emptyList()),
                        
                        Arguments.of(ApplicationStartupState.STARTED, ApplicationStartupState.STARTED, false, 
                                     Arrays.asList(ApplicationStateAction.STOP, ApplicationStateAction.STAGE, ApplicationStateAction.START)),
                        
                        Arguments.of(ApplicationStartupState.STARTED, ApplicationStartupState.STOPPED, true, Arrays.asList(ApplicationStateAction.STOP)),
                        
                        Arguments.of(ApplicationStartupState.STARTED, ApplicationStartupState.STOPPED, false, Arrays.asList(ApplicationStateAction.STOP, ApplicationStateAction.STAGE)),
                        
                        Arguments.of(ApplicationStartupState.INCONSISTENT, ApplicationStartupState.EXECUTED, true,
                                     Arrays.asList(ApplicationStateAction.STOP, ApplicationStateAction.STAGE, ApplicationStateAction.EXECUTE, ApplicationStateAction.START)),
                        
                        Arguments.of(ApplicationStartupState.INCONSISTENT, ApplicationStartupState.EXECUTED, false,
                                     Arrays.asList(ApplicationStateAction.STOP, ApplicationStateAction.STAGE, ApplicationStateAction.EXECUTE, ApplicationStateAction.START)),
                        
                        Arguments.of(ApplicationStartupState.STOPPED, ApplicationStartupState.EXECUTED, true,
                                     Arrays.asList(ApplicationStateAction.STAGE, ApplicationStateAction.START, ApplicationStateAction.EXECUTE)),
                        
                        Arguments.of(ApplicationStartupState.STOPPED, ApplicationStartupState.EXECUTED, false,
                                     Arrays.asList(ApplicationStateAction.STAGE, ApplicationStateAction.START, ApplicationStateAction.EXECUTE)),
                        
                        Arguments.of(ApplicationStartupState.STARTED, ApplicationStartupState.EXECUTED, true,
                                     Arrays.asList(ApplicationStateAction.STOP, ApplicationStateAction.EXECUTE)),
                        
                        Arguments.of(ApplicationStartupState.STARTED, ApplicationStartupState.EXECUTED, false,
                                     Arrays.asList(ApplicationStateAction.STOP, ApplicationStateAction.STAGE, ApplicationStateAction.EXECUTE))
        //@formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testUnchangedApplicationCalculator(ApplicationStartupState currentState, ApplicationStartupState desiredState,
                                                   boolean isApplicationStagedCorrectly,
                                                   List<ApplicationStateAction> expectedApplicationActions) {
        Set<ApplicationStateAction> actionsToExecute = applicationCalculator.determineActionsToExecute(currentState, desiredState,
                                                                                                       isApplicationStagedCorrectly);

        assertEquals(expectedApplicationActions.size(), actionsToExecute.size(),
                     MessageFormat.format("Expected actions {0} but they are {1}", expectedApplicationActions, actionsToExecute));
        assertTrue(expectedApplicationActions.containsAll(actionsToExecute),
                   MessageFormat.format("Expected actions {0} but they are {1}", expectedApplicationActions, actionsToExecute));

    }
}
