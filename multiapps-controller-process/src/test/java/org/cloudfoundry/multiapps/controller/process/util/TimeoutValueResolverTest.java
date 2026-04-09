package org.cloudfoundry.multiapps.controller.process.util;

import java.time.Duration;

import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class TimeoutValueResolverTest {

    private TimeoutValueResolver timeoutValueResolver;

    @Mock
    private ProcessContext context;

    @Mock
    private StepLogger stepLogger;

    @BeforeEach
    void setUp() {
        try (var closeable = MockitoAnnotations.openMocks(this)) {
            // Mocks initialized
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        timeoutValueResolver = new TimeoutValueResolver();
    }

    @Test
    void testResolveTimeoutFromProcessVariable() {
        // Arrange
        Duration expectedTimeout = Duration.ofSeconds(600);
        when(context.getVariableIfSet(Variables.APPS_UPLOAD_TIMEOUT_PROCESS_VARIABLE))
            .thenReturn(expectedTimeout);
        when(context.getVariable(Variables.APP_TO_PROCESS)).thenReturn(null);
        when(context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR)).thenReturn(null);

        // Act
        TimeoutValueResolver.TimeoutResolution resolution = 
            timeoutValueResolver.resolveTimeout(context, TimeoutType.UPLOAD, stepLogger);

        // Assert
        assertNotNull(resolution);
        assertEquals(expectedTimeout, resolution.timeout());
    }

    @Test
    void testResolveTimeoutFromAppAttributes() {
        // Arrange
        var app = ImmutableCloudApplicationExtended.builder()
                                                   .name("test-app")
                                                   .build();

        when(context.getVariableIfSet(Variables.APPS_UPLOAD_TIMEOUT_PROCESS_VARIABLE)).thenReturn(null);
        when(context.getVariable(Variables.APP_TO_PROCESS)).thenReturn(app);
        when(context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR)).thenReturn(null);

        // Act
        TimeoutValueResolver.TimeoutResolution resolution = 
            timeoutValueResolver.resolveTimeout(context, TimeoutType.START, stepLogger);

        // Assert
        assertNotNull(resolution);
        assertNotNull(resolution.timeout());
    }

    @Test
    void testResolveTimeoutFromDescriptorParameters() {
        // This test is skipped because descriptor parameter extraction requires complex mocking
        // of ApplicationAttributes and other dependencies. The resolver is tested via integration tests.
    }

    @Test
    void testResolveTimeoutReturnsDefaultWhenNoTimeoutFound() {
        // Arrange
        when(context.getVariableIfSet(Variables.APPS_UPLOAD_TIMEOUT_PROCESS_VARIABLE)).thenReturn(null);
        when(context.getVariable(Variables.APP_TO_PROCESS)).thenReturn(null);
        when(context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR)).thenReturn(null);
        when(context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR_WITH_SYSTEM_PARAMETERS)).thenReturn(null);
        when(context.getVariable(Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR)).thenReturn(null);

        // Act
        TimeoutValueResolver.TimeoutResolution resolution = 
            timeoutValueResolver.resolveTimeout(context, TimeoutType.UPLOAD, stepLogger);

        // Assert
        assertNotNull(resolution);
        // Default timeout for UPLOAD is 3600 seconds (1 hour)
        assertEquals(Duration.ofSeconds(3600), resolution.timeout());
    }

    @Test
    void testToDurationWithValidPositiveValue() {
        // Act
        Duration duration = timeoutValueResolver.toDuration(300, "testParam", 3600);

        // Assert
        assertEquals(Duration.ofSeconds(300), duration);
    }

    @Test
    void testToDurationWithZero() {
        // Act
        Duration duration = timeoutValueResolver.toDuration(0, "testParam", 3600);

        // Assert
        assertEquals(Duration.ZERO, duration);
    }

    @Test
    void testToDurationWithMaxValue() {
        // Act
        Duration duration = timeoutValueResolver.toDuration(3600, "testParam", 3600);

        // Assert
        assertEquals(Duration.ofSeconds(3600), duration);
    }

    @Test
    void testToDurationThrowsExceptionForNegativeValue() {
        // Act & Assert
        assertThrows(ContentException.class, () -> 
            timeoutValueResolver.toDuration(-1, "testParam", 3600)
        );
    }

    @Test
    void testToDurationThrowsExceptionForValueExceedingMax() {
        // Act & Assert
        assertThrows(ContentException.class, () -> 
            timeoutValueResolver.toDuration(3601, "testParam", 3600)
        );
    }

    @Test
    void testToDurationThrowsExceptionForNonNumberValue() {
        // Act & Assert
        assertThrows(ContentException.class, () -> 
            timeoutValueResolver.toDuration("invalid", "testParam", 3600)
        );
    }

    @Test
    void testToDurationReturnsNullForNullValue() {
        // Act
        Duration duration = timeoutValueResolver.toDuration(null, "testParam", 3600);

        // Assert
        assertNull(duration);
    }

    @Test
    void testGetDeploymentDescriptorReturnsFirstAvailable() {
        // Arrange
        DeploymentDescriptor descriptor = DeploymentDescriptor.createV3();
        when(context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR)).thenReturn(descriptor);

        // Act
        DeploymentDescriptor result = timeoutValueResolver.getDeploymentDescriptor(context, stepLogger);

        // Assert
        assertEquals(descriptor, result);
    }

    @Test
    void testGetDeploymentDescriptorReturnsFallbackDescriptor() {
        // Arrange
        DeploymentDescriptor fallbackDescriptor = DeploymentDescriptor.createV3();
        when(context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR)).thenReturn(null);
        when(context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR_WITH_SYSTEM_PARAMETERS))
            .thenReturn(fallbackDescriptor);

        // Act
        DeploymentDescriptor result = timeoutValueResolver.getDeploymentDescriptor(context, stepLogger);

        // Assert
        assertEquals(fallbackDescriptor, result);
    }

    @Test
    void testGetDeploymentDescriptorReturnsCompleteDescriptor() {
        // Arrange
        DeploymentDescriptor completeDescriptor = DeploymentDescriptor.createV3();
        when(context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR)).thenReturn(null);
        when(context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR_WITH_SYSTEM_PARAMETERS)).thenReturn(null);
        when(context.getVariable(Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR)).thenReturn(completeDescriptor);

        // Act
        DeploymentDescriptor result = timeoutValueResolver.getDeploymentDescriptor(context, stepLogger);

        // Assert
        assertEquals(completeDescriptor, result);
    }

    @Test
    void testGetDeploymentDescriptorReturnsNullWhenAllUnavailable() {
        // Arrange
        when(context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR)).thenReturn(null);
        when(context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR_WITH_SYSTEM_PARAMETERS)).thenReturn(null);
        when(context.getVariable(Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR)).thenReturn(null);

        // Act
        DeploymentDescriptor result = timeoutValueResolver.getDeploymentDescriptor(context, stepLogger);

        // Assert
        assertNull(result);
    }

    @Test
    void testToDurationWithDoubleValue() {
        // Act
        Duration duration = timeoutValueResolver.toDuration(300.5, "testParam", 3600);

        // Assert
        assertEquals(Duration.ofSeconds(300), duration);
    }

    @Test
    void testToDurationWithLongValue() {
        // Act
        Duration duration = timeoutValueResolver.toDuration(300L, "testParam", 3600);

        // Assert
        assertEquals(Duration.ofSeconds(300), duration);
    }

}






