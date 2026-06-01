package org.cloudfoundry.multiapps.controller.process.util;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientProvider;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdditionalModuleParametersReporterTest {

    private static final String MTA_ID = "test-mta";
    private static final String CORRELATION_ID = "test-correlation-id";
    private static final String MODULE_TYPE = "java-tomcat";
    private static final String MODULE_NAME = "test-module";
    private static final Integer HEALTH_CHECK_INTERVAL_VALUE = 15;

    private ProcessContext context;
    private AdditionalModuleParametersReporter reporter;

    @BeforeEach
    void setUp() {
        context = createContext();
        DeploymentDescriptor descriptor = DeploymentDescriptor.createV3()
                                                              .setId(MTA_ID);
        context.setVariable(Variables.DEPLOYMENT_DESCRIPTOR, descriptor);
        context.setVariable(Variables.CORRELATION_ID, CORRELATION_ID);
        reporter = new AdditionalModuleParametersReporter(context);
    }

    @Test
    void testReportUsageWithHealthCheckIntervalDoesNotThrow() {
        Module module = createModule(Map.of(SupportedParameters.HEALTH_CHECK_INTERVAL, HEALTH_CHECK_INTERVAL_VALUE));

        assertDoesNotThrow(() -> reporter.reportUsageOfAdditionalParameters(module));
    }

    @Test
    void testReportUsageWithoutHealthCheckIntervalDoesNotThrow() {
        Module module = createModule(Map.of());

        assertDoesNotThrow(() -> reporter.reportUsageOfAdditionalParameters(module));
    }

    @Test
    void testReportUsageWithBothHealthCheckIntervalAndReadinessParametersDoesNotThrow() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(SupportedParameters.HEALTH_CHECK_INTERVAL, HEALTH_CHECK_INTERVAL_VALUE);
        parameters.put(SupportedParameters.READINESS_HEALTH_CHECK_TYPE, "http");
        parameters.put(SupportedParameters.READINESS_HEALTH_CHECK_HTTP_ENDPOINT, "/ready");
        parameters.put(SupportedParameters.READINESS_HEALTH_CHECK_INVOCATION_TIMEOUT, 5);
        parameters.put(SupportedParameters.READINESS_HEALTH_CHECK_INTERVAL, 30);
        Module module = createModule(parameters);

        assertDoesNotThrow(() -> reporter.reportUsageOfAdditionalParameters(module));
    }

    @Test
    void testHealthCheckIntervalMessageTemplateIsWellFormed() {
        assertNotNull(Messages.MTA_USES_HEALTH_CHECK_INTERVAL_PARAMETER);
        String formatted = MessageFormat.format(Messages.MTA_USES_HEALTH_CHECK_INTERVAL_PARAMETER, MTA_ID, CORRELATION_ID,
                                                HEALTH_CHECK_INTERVAL_VALUE, "[]", MODULE_TYPE);

        assertTrue(formatted.contains(MTA_ID), () -> "expected MTA id in message, got: " + formatted);
        assertTrue(formatted.contains(CORRELATION_ID), () -> "expected correlation id in message, got: " + formatted);
        assertTrue(formatted.contains(HEALTH_CHECK_INTERVAL_VALUE.toString()),
                   () -> "expected health-check-interval value in message, got: " + formatted);
        assertTrue(formatted.contains(MODULE_TYPE), () -> "expected module type in message, got: " + formatted);
    }

    @Test
    void testHealthCheckIntervalMessageMentionsIntervalKeyword() {
        String formatted = MessageFormat.format(Messages.MTA_USES_HEALTH_CHECK_INTERVAL_PARAMETER, MTA_ID, CORRELATION_ID,
                                                HEALTH_CHECK_INTERVAL_VALUE, "[]", MODULE_TYPE);

        assertTrue(formatted.toLowerCase()
                            .contains("interval"),
                   () -> "expected the message to identify it as a health-check-interval log line, got: " + formatted);
    }

    @Test
    void testReportUsageReadsHealthCheckIntervalFromModuleParametersAsInteger() {
        Module module = createModule(Map.of(SupportedParameters.HEALTH_CHECK_INTERVAL, HEALTH_CHECK_INTERVAL_VALUE));

        Object actual = module.getParameters()
                              .get(SupportedParameters.HEALTH_CHECK_INTERVAL);

        assertEquals(HEALTH_CHECK_INTERVAL_VALUE, actual);
        assertDoesNotThrow(() -> reporter.reportUsageOfAdditionalParameters(module));
    }

    @Test
    void testReportUsageWithHealthCheckIntervalAndBuildpacksDoesNotThrow() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(SupportedParameters.HEALTH_CHECK_INTERVAL, HEALTH_CHECK_INTERVAL_VALUE);
        parameters.put(SupportedParameters.BUILDPACKS, List.of("java_buildpack"));
        Module module = createModule(parameters);

        assertDoesNotThrow(() -> reporter.reportUsageOfAdditionalParameters(module));
    }

    private Module createModule(Map<String, Object> parameters) {
        Map<String, Object> mutableParameters = new HashMap<>(parameters);
        return Module.createV3()
                     .setName(MODULE_NAME)
                     .setType(MODULE_TYPE)
                     .setParameters(mutableParameters);
    }

    private ProcessContext createContext() {
        DelegateExecution execution = MockDelegateExecution.createSpyInstance();
        StepLogger stepLogger = Mockito.mock(StepLogger.class);
        CloudControllerClientProvider clientProvider = Mockito.mock(CloudControllerClientProvider.class);
        return new ProcessContext(execution, stepLogger, clientProvider);
    }

}
