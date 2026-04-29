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
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this).close();
        timeoutValueResolver = new TimeoutValueResolver();
    }

    @Test
    void testResolveTimeoutFromProcessVariable() {
        Duration expectedTimeout = Duration.ofSeconds(600);
        when(context.getVariableIfSet(Variables.APPS_UPLOAD_TIMEOUT_PROCESS_VARIABLE))
            .thenReturn(expectedTimeout);
        when(context.getVariable(Variables.APP_TO_PROCESS)).thenReturn(null);
        when(context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR)).thenReturn(null);

        TimeoutValueResolver.TimeoutResolution resolution = 
            timeoutValueResolver.resolveTimeout(context, TimeoutType.UPLOAD, stepLogger);

        assertNotNull(resolution);
        assertEquals(expectedTimeout, resolution.timeout());
    }

    @Test
    void testResolveTimeoutFromAppAttributes() {
        var app = ImmutableCloudApplicationExtended.builder()
                                                   .name("test-app")
                                                   .build();

        when(context.getVariableIfSet(Variables.APPS_UPLOAD_TIMEOUT_PROCESS_VARIABLE)).thenReturn(null);
        when(context.getVariable(Variables.APP_TO_PROCESS)).thenReturn(app);
        when(context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR)).thenReturn(null);

        TimeoutValueResolver.TimeoutResolution resolution = 
            timeoutValueResolver.resolveTimeout(context, TimeoutType.START, stepLogger);

        assertNotNull(resolution);
        assertNotNull(resolution.timeout());
    }

    @Test
    void testResolveTimeoutFromDescriptorParameters() {
    }

    @Test
    void testResolveTimeoutReturnsDefaultWhenNoTimeoutFound() {
        when(context.getVariableIfSet(Variables.APPS_UPLOAD_TIMEOUT_PROCESS_VARIABLE)).thenReturn(null);
        when(context.getVariable(Variables.APP_TO_PROCESS)).thenReturn(null);
        when(context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR)).thenReturn(null);
        when(context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR_WITH_SYSTEM_PARAMETERS)).thenReturn(null);
        when(context.getVariable(Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR)).thenReturn(null);

        TimeoutValueResolver.TimeoutResolution resolution = 
            timeoutValueResolver.resolveTimeout(context, TimeoutType.UPLOAD, stepLogger);

        assertNotNull(resolution);
        assertEquals(Duration.ofSeconds(3600), resolution.timeout());
    }

    @Test
    void testToDurationWithValidPositiveValue() {
        Duration duration = timeoutValueResolver.toDuration(300, "testParam", 3600);

        assertEquals(Duration.ofSeconds(300), duration);
    }

    @Test
    void testToDurationWithZero() {
        Duration duration = timeoutValueResolver.toDuration(0, "testParam", 3600);

        assertEquals(Duration.ZERO, duration);
    }

    @Test
    void testToDurationWithMaxValue() {
        Duration duration = timeoutValueResolver.toDuration(3600, "testParam", 3600);

        assertEquals(Duration.ofSeconds(3600), duration);
    }

    @Test
    void testToDurationThrowsExceptionForNegativeValue() {
        assertThrows(ContentException.class, () -> 
            timeoutValueResolver.toDuration(-1, "testParam", 3600)
        );
    }

    @Test
    void testToDurationThrowsExceptionForValueExceedingMax() {
        assertThrows(ContentException.class, () -> 
            timeoutValueResolver.toDuration(3601, "testParam", 3600)
        );
    }

    @Test
    void testToDurationThrowsExceptionForNonNumberValue() {
        assertThrows(ContentException.class, () -> 
            timeoutValueResolver.toDuration("invalid", "testParam", 3600)
        );
    }

    @Test
    void testToDurationReturnsNullForNullValue() {
        Duration duration = timeoutValueResolver.toDuration(null, "testParam", 3600);

        assertNull(duration);
    }

    @Test
    void testGetDeploymentDescriptorReturnsFirstAvailable() {
        DeploymentDescriptor descriptor = DeploymentDescriptor.createV3();
        when(context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR)).thenReturn(descriptor);

        DeploymentDescriptor result = timeoutValueResolver.getDeploymentDescriptor(context, stepLogger);

        assertEquals(descriptor, result);
    }

    @Test
    void testGetDeploymentDescriptorReturnsFallbackDescriptor() {
        DeploymentDescriptor fallbackDescriptor = DeploymentDescriptor.createV3();
        when(context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR)).thenReturn(null);
        when(context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR_WITH_SYSTEM_PARAMETERS))
            .thenReturn(fallbackDescriptor);

        DeploymentDescriptor result = timeoutValueResolver.getDeploymentDescriptor(context, stepLogger);

        assertEquals(fallbackDescriptor, result);
    }

    @Test
    void testGetDeploymentDescriptorReturnsCompleteDescriptor() {
        DeploymentDescriptor completeDescriptor = DeploymentDescriptor.createV3();
        when(context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR)).thenReturn(null);
        when(context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR_WITH_SYSTEM_PARAMETERS)).thenReturn(null);
        when(context.getVariable(Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR)).thenReturn(completeDescriptor);

        DeploymentDescriptor result = timeoutValueResolver.getDeploymentDescriptor(context, stepLogger);

        assertEquals(completeDescriptor, result);
    }

    @Test
    void testGetDeploymentDescriptorReturnsNullWhenAllUnavailable() {
        when(context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR)).thenReturn(null);
        when(context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR_WITH_SYSTEM_PARAMETERS)).thenReturn(null);
        when(context.getVariable(Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR)).thenReturn(null);

        DeploymentDescriptor result = timeoutValueResolver.getDeploymentDescriptor(context, stepLogger);

        assertNull(result);
    }

    @Test
    void testToDurationWithDoubleValue() {
        Duration duration = timeoutValueResolver.toDuration(300.5, "testParam", 3600);

        assertEquals(Duration.ofSeconds(300), duration);
    }

    @Test
    void testToDurationWithLongValue() {
        Duration duration = timeoutValueResolver.toDuration(300L, "testParam", 3600);

        assertEquals(Duration.ofSeconds(300), duration);
    }

}






