package org.cloudfoundry.multiapps.controller.process.util;

import java.util.HashMap;
import java.util.Map;

import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientProvider;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class AdditionalModuleParametersReporterTest {

    private static final String MTA_ID = "test-mta";
    private static final String CORRELATION_ID = "test-correlation-id";

    @Test
    void testHealthCheckIntervalLoggedWhenSet() {
        Logger mockLogger = Mockito.mock(Logger.class);
        Module module = createModule(Map.of("health-check-interval", 30));

        new AdditionalModuleParametersReporter(createContext(), mockLogger).reportUsageOfAdditionalParameters(module);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockLogger, atLeastOnce()).info(messageCaptor.capture());
        long livenessLogs = messageCaptor.getAllValues()
                                         .stream()
                                         .filter(message -> message.contains("liveness health check"))
                                         .count();
        assertEquals(1, livenessLogs);
        String message = messageCaptor.getAllValues()
                                      .stream()
                                      .filter(formatted -> formatted.contains("liveness health check"))
                                      .findFirst()
                                      .orElseThrow();
        assertTrue(message.contains("interval=\"30\""), "Log message should contain interval=\"30\", was: " + message);
        assertTrue(message.contains(MTA_ID), "Log message should contain MTA id, was: " + message);
        assertTrue(message.contains(CORRELATION_ID), "Log message should contain correlation id, was: " + message);
    }

    @Test
    void testHealthCheckIntervalNotLoggedWhenAbsent() {
        Logger mockLogger = Mockito.mock(Logger.class);
        Module module = createModule(new HashMap<>());

        new AdditionalModuleParametersReporter(createContext(), mockLogger).reportUsageOfAdditionalParameters(module);

        verify(mockLogger, never()).info(anyString());
    }

    private static Module createModule(Map<String, Object> parameters) {
        return Module.createV3()
                     .setName("test-module")
                     .setType("application")
                     .setParameters(parameters);
    }

    private static ProcessContext createContext() {
        DelegateExecution delegateExecution = MockDelegateExecution.createSpyInstance();
        StepLogger stepLogger = Mockito.mock(StepLogger.class);
        CloudControllerClientProvider cloudControllerClientProvider = Mockito.mock(CloudControllerClientProvider.class);
        ProcessContext context = new ProcessContext(delegateExecution, stepLogger, cloudControllerClientProvider);
        DeploymentDescriptor deploymentDescriptor = DeploymentDescriptor.createV3()
                                                                       .setId(MTA_ID);
        context.setVariable(Variables.DEPLOYMENT_DESCRIPTOR, deploymentDescriptor);
        context.setVariable(Variables.CORRELATION_ID, CORRELATION_ID);
        return context;
    }
}
