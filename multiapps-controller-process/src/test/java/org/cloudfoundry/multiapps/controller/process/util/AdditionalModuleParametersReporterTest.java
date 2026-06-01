package org.cloudfoundry.multiapps.controller.process.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientProvider;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class AdditionalModuleParametersReporterTest {

    private static final String MODULE_TYPE = "java";

    @Mock
    private StepLogger stepLogger;
    @Mock
    private CloudControllerClientProvider clientProvider;

    private AdditionalModuleParametersReporter reporter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        DelegateExecution execution = MockDelegateExecution.createSpyInstance();
        ProcessContext context = new ProcessContext(execution, stepLogger, clientProvider);
        context.setVariable(Variables.DEPLOYMENT_DESCRIPTOR, DeploymentDescriptor.createV3()
                                                                                 .setId("test-mta")
                                                                                 .setVersion("1.0.0")
                                                                                 .setModules(List.of())
                                                                                 .setResources(List.of()));
        context.setVariable(Variables.CORRELATION_ID, "correlation-id-1");
        reporter = new AdditionalModuleParametersReporter(context);
    }

    @Test
    void testReportingHealthCheckIntervalOnlyDoesNotThrow() {
        Module module = buildModuleWithParameters(Map.of(SupportedParameters.HEALTH_CHECK_INTERVAL, 30));
        assertDoesNotThrow(() -> reporter.reportUsageOfAdditionalParameters(module));
    }

    @Test
    void testReportingFullHealthCheckFamilyDoesNotThrow() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(SupportedParameters.HEALTH_CHECK_TYPE, "http");
        parameters.put(SupportedParameters.HEALTH_CHECK_HTTP_ENDPOINT, "/health");
        parameters.put(SupportedParameters.HEALTH_CHECK_TIMEOUT, 10);
        parameters.put(SupportedParameters.HEALTH_CHECK_INVOCATION_TIMEOUT, 5);
        parameters.put(SupportedParameters.HEALTH_CHECK_INTERVAL, 45);
        Module module = buildModuleWithParameters(parameters);
        assertDoesNotThrow(() -> reporter.reportUsageOfAdditionalParameters(module));
    }

    @Test
    void testReportingWithNoHealthCheckParametersDoesNotThrow() {
        Module module = buildModuleWithParameters(Map.of());
        assertDoesNotThrow(() -> reporter.reportUsageOfAdditionalParameters(module));
    }

    private Module buildModuleWithParameters(Map<String, Object> parameters) {
        Module module = Module.createV3()
                              .setName("test-module")
                              .setType(MODULE_TYPE);
        module.setParameters(parameters);
        return module;
    }
}
