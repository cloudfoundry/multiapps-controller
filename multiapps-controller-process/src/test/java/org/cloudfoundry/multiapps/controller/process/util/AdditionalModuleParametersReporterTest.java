package org.cloudfoundry.multiapps.controller.process.util;

import java.util.HashMap;
import java.util.Map;

import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdditionalModuleParametersReporterTest {

    private static final String MTA_ID = "test-mta";
    private static final String CORRELATION_ID = "corr-1";
    private static final String MODULE_TYPE = "java.tomee";
    private static final String HTTP = "http";
    private static final String ENDPOINT = "/health";
    private static final Integer INVOCATION_TIMEOUT = 5;
    private static final Integer INTERVAL = 30;

    @Mock
    private ProcessContext context;

    private AdditionalModuleParametersReporter reporter;
    private AutoCloseable mockitoCloseable;

    @BeforeEach
    void setUp() {
        mockitoCloseable = MockitoAnnotations.openMocks(this);
        DeploymentDescriptor descriptor = DeploymentDescriptor.createV3()
                                                              .setId(MTA_ID);
        when(context.getRequiredVariable(Variables.DEPLOYMENT_DESCRIPTOR)).thenReturn(descriptor);
        when(context.getRequiredVariable(Variables.CORRELATION_ID)).thenReturn(CORRELATION_ID);
        reporter = new AdditionalModuleParametersReporter(context);
    }

    @AfterEach
    void tearDown() throws Exception {
        mockitoCloseable.close();
    }

    @Test
    void testReportEntersHealthCheckBranchWhenHealthCheckTypeIsPresent() {
        Module module = buildModuleWithHealthCheck(HTTP, INTERVAL);

        assertDoesNotThrow(() -> reporter.reportUsageOfAdditionalParameters(module));

        verify(context).getRequiredVariable(Variables.DEPLOYMENT_DESCRIPTOR);
        verify(context).getRequiredVariable(Variables.CORRELATION_ID);
    }

    private static Module buildModuleWithHealthCheck(String type, Integer interval) {
        Map<String, Object> params = new HashMap<>();
        params.put(SupportedParameters.HEALTH_CHECK_TYPE, type);
        params.put(SupportedParameters.HEALTH_CHECK_HTTP_ENDPOINT, ENDPOINT);
        params.put(SupportedParameters.HEALTH_CHECK_INVOCATION_TIMEOUT, INVOCATION_TIMEOUT);
        if (interval != null) {
            params.put(SupportedParameters.HEALTH_CHECK_INTERVAL, interval);
        }
        return Module.createV3()
                     .setName("m")
                     .setType(MODULE_TYPE)
                     .setParameters(params);
    }

    @Test
    void testReportToleratesNullHealthCheckInterval() {
        Module module = buildModuleWithHealthCheck(HTTP, null);

        assertDoesNotThrow(() -> reporter.reportUsageOfAdditionalParameters(module));
    }

    @Test
    void testReportSkipsHealthCheckBranchWhenHealthCheckTypeIsMissing() {
        Map<String, Object> params = new HashMap<>();
        params.put(SupportedParameters.HEALTH_CHECK_INTERVAL, INTERVAL);
        Module module = Module.createV3()
                              .setName("m")
                              .setType(MODULE_TYPE)
                              .setParameters(params);

        assertDoesNotThrow(() -> reporter.reportUsageOfAdditionalParameters(module));
    }

    @Test
    void testReportHandlesBothHealthCheckBranchesWithoutThrowing() {
        Map<String, Object> params = new HashMap<>();
        params.put(SupportedParameters.HEALTH_CHECK_TYPE, HTTP);
        params.put(SupportedParameters.HEALTH_CHECK_HTTP_ENDPOINT, ENDPOINT);
        params.put(SupportedParameters.HEALTH_CHECK_INVOCATION_TIMEOUT, INVOCATION_TIMEOUT);
        params.put(SupportedParameters.HEALTH_CHECK_INTERVAL, INTERVAL);
        params.put(SupportedParameters.READINESS_HEALTH_CHECK_TYPE, HTTP);
        params.put(SupportedParameters.READINESS_HEALTH_CHECK_HTTP_ENDPOINT, ENDPOINT);
        params.put(SupportedParameters.READINESS_HEALTH_CHECK_INVOCATION_TIMEOUT, INVOCATION_TIMEOUT);
        params.put(SupportedParameters.READINESS_HEALTH_CHECK_INTERVAL, INTERVAL);
        Module module = Module.createV3()
                              .setName("m")
                              .setType(MODULE_TYPE)
                              .setParameters(params);

        assertDoesNotThrow(() -> reporter.reportUsageOfAdditionalParameters(module));
    }

    @Test
    void testReportToleratesEmptyParameterMap() {
        Module module = Module.createV3()
                              .setName("m")
                              .setType(MODULE_TYPE)
                              .setParameters(new HashMap<>());

        assertDoesNotThrow(() -> reporter.reportUsageOfAdditionalParameters(module));
    }

}
