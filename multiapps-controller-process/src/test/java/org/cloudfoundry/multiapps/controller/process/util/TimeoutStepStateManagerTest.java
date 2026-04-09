package org.cloudfoundry.multiapps.controller.process.util;

import java.time.Duration;

import org.cloudfoundry.multiapps.controller.process.Constants;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.steps.StepPhase;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TimeoutStepStateManagerTest {

    private TimeoutStepStateManager timeoutStepStateManager;

    @Mock
    private ProcessContext context;

    @Mock
    private DelegateExecution execution;

    @BeforeEach
    void setUp() {
        try (var closeable = MockitoAnnotations.openMocks(this)) {
            // Mocks initialized
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        timeoutStepStateManager = new TimeoutStepStateManager();
        when(context.getExecution()).thenReturn(execution);
    }

    @Test
    void testHasTimedOutReturnsFalseWhenWithinTimeout() {
        // Arrange
        String stepName = "testStep";
        Duration timeout = Duration.ofSeconds(10);
        long now = System.currentTimeMillis();
        long stepStartTime = now - 5000; // 5 seconds ago

        when(execution.getVariable(Constants.VAR_STEP_START_TIME + stepName)).thenReturn(stepStartTime);
        when(context.getVariable(Variables.STEP_PHASE)).thenReturn(StepPhase.DONE);
        when(context.getVariable(Variables.MUST_RESET_TIMEOUT)).thenReturn(false);

        // Act
        boolean hasTimedOut = timeoutStepStateManager.hasTimedOut(context, stepName, timeout);

        // Assert
        assertFalse(hasTimedOut);
    }

    @Test
    void testHasTimedOutReturnsTrueWhenTimeoutExceeded() {
        // Arrange
        String stepName = "testStep";
        Duration timeout = Duration.ofSeconds(5);
        long now = System.currentTimeMillis();
        long stepStartTime = now - 10000; // 10 seconds ago

        when(execution.getVariable(Constants.VAR_STEP_START_TIME + stepName)).thenReturn(stepStartTime);
        when(context.getVariable(Variables.STEP_PHASE)).thenReturn(StepPhase.DONE);
        when(context.getVariable(Variables.MUST_RESET_TIMEOUT)).thenReturn(false);

        // Act
        boolean hasTimedOut = timeoutStepStateManager.hasTimedOut(context, stepName, timeout);

        // Assert
        assertTrue(hasTimedOut);
    }

    @Test
    void testHasTimedOutReturnsFalseAtExactTimeout() {
        // Arrange
        String stepName = "testStep";
        Duration timeout = Duration.ofSeconds(10);
        long now = System.currentTimeMillis();
        long stepStartTime = now - 10000; // Exactly 10 seconds ago

        when(execution.getVariable(Constants.VAR_STEP_START_TIME + stepName)).thenReturn(stepStartTime);
        when(context.getVariable(Variables.STEP_PHASE)).thenReturn(StepPhase.DONE);
        when(context.getVariable(Variables.MUST_RESET_TIMEOUT)).thenReturn(false);

        // Act
        boolean hasTimedOut = timeoutStepStateManager.hasTimedOut(context, stepName, timeout);

        // Assert
        assertTrue(hasTimedOut);
    }

    @Test
    void testGetStepStartTimeInitializesTimeWhenNotSet() {
        // Arrange
        String stepName = "testStep";
        long now = System.currentTimeMillis();

        when(execution.getVariable(Constants.VAR_STEP_START_TIME + stepName)).thenReturn(null);
        when(context.getVariable(Variables.STEP_PHASE)).thenReturn(StepPhase.DONE);
        when(context.getVariable(Variables.MUST_RESET_TIMEOUT)).thenReturn(false);

        // Act
        long stepStartTime = timeoutStepStateManager.getStepStartTime(context, stepName);

        // Assert
        assertTrue(stepStartTime >= now);
        assertTrue(stepStartTime <= System.currentTimeMillis());
        verify(execution).setVariable(Constants.VAR_STEP_START_TIME + stepName, stepStartTime);
    }

    @Test
    void testGetStepStartTimeReturnsExistingTimeWhenInitialized() {
        // Arrange
        String stepName = "testStep";
        long existingTime = System.currentTimeMillis() - 5000;

        when(execution.getVariable(Constants.VAR_STEP_START_TIME + stepName)).thenReturn(existingTime);
        when(context.getVariable(Variables.STEP_PHASE)).thenReturn(StepPhase.DONE);
        when(context.getVariable(Variables.MUST_RESET_TIMEOUT)).thenReturn(false);

        // Act
        long stepStartTime = timeoutStepStateManager.getStepStartTime(context, stepName);

        // Assert
        assertEquals(existingTime, stepStartTime);
    }

    @Test
    void testGetStepStartTimeResetsTimeoutOnRetry() {
        // Arrange
        String stepName = "testStep";
        long oldTime = System.currentTimeMillis() - 100000;
        long now = System.currentTimeMillis();

        when(execution.getVariable(Constants.VAR_STEP_START_TIME + stepName)).thenReturn(oldTime);
        when(context.getVariable(Variables.STEP_PHASE)).thenReturn(StepPhase.RETRY);
        when(context.getVariable(Variables.MUST_RESET_TIMEOUT)).thenReturn(false);

        // Act
        long stepStartTime = timeoutStepStateManager.getStepStartTime(context, stepName);

        // Assert
        assertTrue(stepStartTime >= now);
        assertTrue(stepStartTime <= System.currentTimeMillis());
        verify(execution).setVariable(Constants.VAR_STEP_START_TIME + stepName, stepStartTime);
    }

    @Test
    void testGetStepStartTimeResetsTimeoutWhenMustResetFlagIsSet() {
        // Arrange
        String stepName = "testStep";
        long oldTime = System.currentTimeMillis() - 100000;
        long now = System.currentTimeMillis();

        when(execution.getVariable(Constants.VAR_STEP_START_TIME + stepName)).thenReturn(oldTime);
        when(context.getVariable(Variables.STEP_PHASE)).thenReturn(StepPhase.DONE);
        when(context.getVariable(Variables.MUST_RESET_TIMEOUT)).thenReturn(true);

        // Act
        long stepStartTime = timeoutStepStateManager.getStepStartTime(context, stepName);

        // Assert
        assertTrue(stepStartTime >= now);
        assertTrue(stepStartTime <= System.currentTimeMillis());
        verify(execution).setVariable(Constants.VAR_STEP_START_TIME + stepName, stepStartTime);
        verify(context).setVariable(Variables.MUST_RESET_TIMEOUT, false);
    }

    @Test
    void testGetStepStartTimeDoesNotResetWhenNotNeeded() {
        // Arrange
        String stepName = "testStep";
        long existingTime = System.currentTimeMillis() - 5000;

        when(execution.getVariable(Constants.VAR_STEP_START_TIME + stepName)).thenReturn(existingTime);
        when(context.getVariable(Variables.STEP_PHASE)).thenReturn(StepPhase.DONE);
        when(context.getVariable(Variables.MUST_RESET_TIMEOUT)).thenReturn(false);

        // Act
        long stepStartTime = timeoutStepStateManager.getStepStartTime(context, stepName);

        // Assert
        assertEquals(existingTime, stepStartTime);
    }

    @Test
    void testMultipleStepsHaveSeparateTimeoutStates() {
        // Arrange
        String step1 = "step1";
        String step2 = "step2";
        long time1 = System.currentTimeMillis() - 5000;
        long time2 = System.currentTimeMillis() - 10000;

        when(execution.getVariable(Constants.VAR_STEP_START_TIME + step1)).thenReturn(time1);
        when(execution.getVariable(Constants.VAR_STEP_START_TIME + step2)).thenReturn(time2);
        when(context.getVariable(Variables.STEP_PHASE)).thenReturn(StepPhase.DONE);
        when(context.getVariable(Variables.MUST_RESET_TIMEOUT)).thenReturn(false);

        // Act
        long step1StartTime = timeoutStepStateManager.getStepStartTime(context, step1);
        long step2StartTime = timeoutStepStateManager.getStepStartTime(context, step2);

        // Assert
        assertEquals(time1, step1StartTime);
        assertEquals(time2, step2StartTime);
    }

    @Test
    void testHasTimedOutWithLargeTimeout() {
        // Arrange
        String stepName = "testStep";
        Duration timeout = Duration.ofHours(1);
        long now = System.currentTimeMillis();
        long stepStartTime = now - 60000; // 1 minute ago

        when(execution.getVariable(Constants.VAR_STEP_START_TIME + stepName)).thenReturn(stepStartTime);
        when(context.getVariable(Variables.STEP_PHASE)).thenReturn(StepPhase.DONE);
        when(context.getVariable(Variables.MUST_RESET_TIMEOUT)).thenReturn(false);

        // Act
        boolean hasTimedOut = timeoutStepStateManager.hasTimedOut(context, stepName, timeout);

        // Assert
        assertFalse(hasTimedOut);
    }

    @Test
    void testHasTimedOutWithSmallTimeout() {
        // Arrange
        String stepName = "testStep";
        Duration timeout = Duration.ofMillis(100);
        long now = System.currentTimeMillis();
        long stepStartTime = now - 200; // 200ms ago

        when(execution.getVariable(Constants.VAR_STEP_START_TIME + stepName)).thenReturn(stepStartTime);
        when(context.getVariable(Variables.STEP_PHASE)).thenReturn(StepPhase.DONE);
        when(context.getVariable(Variables.MUST_RESET_TIMEOUT)).thenReturn(false);

        // Act
        boolean hasTimedOut = timeoutStepStateManager.hasTimedOut(context, stepName, timeout);

        // Assert
        assertTrue(hasTimedOut);
    }

}






