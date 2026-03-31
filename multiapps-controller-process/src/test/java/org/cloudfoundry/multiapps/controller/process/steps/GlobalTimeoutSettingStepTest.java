package org.cloudfoundry.multiapps.controller.process.steps;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.process.util.StepLogger;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GlobalTimeoutSettingStepTest {

    private GlobalTimeoutSettingStep step;
    private ProcessContext context;
    private StepLogger stepLogger;

    @BeforeEach
    void setUp() {
        step = new GlobalTimeoutSettingStep();
        context = mock(ProcessContext.class);
        stepLogger = mock(StepLogger.class);
        when(step.getStepLogger()).thenReturn(stepLogger);
    }

    @Test
    void testExtractsTimeoutsFromDescriptorParameters() {
        // Arrange
        DeploymentDescriptor descriptor = createDescriptorWithTimeouts();
        when(context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR)).thenReturn(descriptor);

        // Act
        StepPhase result = step.executeStep(context);

        // Assert
        assertEquals(StepPhase.DONE, result);
        
        // Verify at least some timeouts were set
        verify(context, atLeastOnce()).setVariable(
            any(),
            any(Duration.class)
        );
    }

    @Test
    void testHandlesMissingDescriptor() {
        // Arrange
        when(context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR)).thenReturn(null);
        when(context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR_WITH_SYSTEM_PARAMETERS)).thenReturn(null);
        when(context.getVariable(Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR)).thenReturn(null);

        // Act
        StepPhase result = step.executeStep(context);

        // Assert
        assertEquals(StepPhase.DONE, result);
        verify(stepLogger).debug(contains("No descriptor found"));
    }

    @Test
    void testHandlesEmptyDescriptorParameters() {
        // Arrange
        DeploymentDescriptor descriptor = mock(DeploymentDescriptor.class);
        when(descriptor.getParameters()).thenReturn(null);
        when(context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR)).thenReturn(descriptor);

        // Act
        StepPhase result = step.executeStep(context);

        // Assert
        assertEquals(StepPhase.DONE, result);
    }

    @Test
    void testSetsUploadTimeoutFromDescriptor() {
        // Arrange
        DeploymentDescriptor descriptor = createDescriptorWithTimeout(
            SupportedParameters.APPS_UPLOAD_TIMEOUT,
            3600
        );
        when(context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR)).thenReturn(descriptor);

        // Act
        step.executeStep(context);

        // Assert
        verify(context).setVariable(
            Variables.APPS_UPLOAD_TIMEOUT_PROCESS_VARIABLE,
            Duration.ofSeconds(3600)
        );
    }

    @Test
    void testSetsStageTimeoutFromDescriptor() {
        // Arrange
        DeploymentDescriptor descriptor = createDescriptorWithTimeout(
            SupportedParameters.APPS_STAGE_TIMEOUT,
            1800
        );
        when(context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR)).thenReturn(descriptor);

        // Act
        step.executeStep(context);

        // Assert
        verify(context).setVariable(
            Variables.APPS_STAGE_TIMEOUT_PROCESS_VARIABLE,
            Duration.ofSeconds(1800)
        );
    }

    @Test
    void testSetsServiceTimeoutsFromDescriptor() {
        // Arrange
        DeploymentDescriptor descriptor = createDescriptorWithTimeouts(
            Map.of(
                SupportedParameters.SERVICES_CREATE_SERVICE_TIMEOUT, 600,
                SupportedParameters.SERVICES_BIND_SERVICE_TIMEOUT, 900
            )
        );
        when(context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR)).thenReturn(descriptor);

        // Act
        step.executeStep(context);

        // Assert
        verify(context).setVariable(
            Variables.CREATE_SERVICE_TIMEOUT_PROCESS_VARIABLE,
            Duration.ofSeconds(600)
        );
        verify(context).setVariable(
            Variables.BIND_SERVICE_TIMEOUT_PROCESS_VARIABLE,
            Duration.ofSeconds(900)
        );
    }

    @Test
    void testIgnoresInvalidTimeoutValues() {
        // Arrange
        DeploymentDescriptor descriptor = mock(DeploymentDescriptor.class);
        Map<String, Object> params = new HashMap<>();
        params.put(SupportedParameters.APPS_UPLOAD_TIMEOUT, "not-a-number"); // Invalid
        when(descriptor.getParameters()).thenReturn(params);
        when(context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR)).thenReturn(descriptor);

        // Act & Assert
        assertThrows(Exception.class, () -> step.executeStep(context));
    }

    @Test
    void testLogsSuccessfulTimeoutExtractions() {
        // Arrange
        DeploymentDescriptor descriptor = createDescriptorWithTimeouts();
        when(context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR)).thenReturn(descriptor);

        // Act
        step.executeStep(context);

        // Assert
        verify(stepLogger).info(
            contains("Successfully extracted"),
            anyInt()
        );
    }

    @Test
    void testContinuesOnTimeoutResolutionFailure() {
        // Arrange - Create descriptor that will fail resolution for one type
        DeploymentDescriptor descriptor = createDescriptorWithTimeout(
            SupportedParameters.APPS_UPLOAD_TIMEOUT,
            5000000  // Exceeds max allowed value
        );
        when(context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR)).thenReturn(descriptor);

        // Act - Should not throw, should continue to next timeout type
        StepPhase result = step.executeStep(context);

        // Assert
        assertEquals(StepPhase.DONE, result);
        verify(stepLogger).warn(contains("Failed to resolve"));
    }

    // ============ Helper Methods ============

    private DeploymentDescriptor createDescriptorWithTimeouts() {
        return createDescriptorWithTimeouts(Map.of(
            SupportedParameters.APPS_UPLOAD_TIMEOUT, 3600,
            SupportedParameters.APPS_STAGE_TIMEOUT, 1800,
            SupportedParameters.SERVICES_CREATE_SERVICE_TIMEOUT, 600,
            SupportedParameters.SERVICES_BIND_SERVICE_TIMEOUT, 900,
            SupportedParameters.SERVICES_UNBIND_SERVICE_TIMEOUT, 900,
            SupportedParameters.SERVICES_UPDATE_SERVICE_TIMEOUT, 600,
            SupportedParameters.SERVICES_DELETE_SERVICE_TIMEOUT, 600,
            SupportedParameters.SERVICES_CREATE_SERVICE_KEY_TIMEOUT, 300,
            SupportedParameters.SERVICES_DELETE_SERVICE_KEY_TIMEOUT, 300
        ));
    }

    private DeploymentDescriptor createDescriptorWithTimeout(String paramName, int timeoutInSeconds) {
        return createDescriptorWithTimeouts(Map.of(paramName, timeoutInSeconds));
    }

    private DeploymentDescriptor createDescriptorWithTimeouts(Map<String, Integer> timeouts) {
        DeploymentDescriptor descriptor = mock(DeploymentDescriptor.class);
        Map<String, Object> params = new HashMap<>();
        timeouts.forEach((key, value) -> params.put(key, value));
        when(descriptor.getParameters()).thenReturn(params);
        return descriptor;
    }
}

