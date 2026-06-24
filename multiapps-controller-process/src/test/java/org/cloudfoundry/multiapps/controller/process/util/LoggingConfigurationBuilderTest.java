package org.cloudfoundry.multiapps.controller.process.util;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.client.facade.CloudControllerClient;
import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudMetadata;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudServiceInstance;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudServiceKey;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudSpace;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientFactory;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientProvider;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.core.security.token.TokenService;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableLoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.LogLevel;
import org.cloudfoundry.multiapps.controller.persistence.model.LoggingConfiguration;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoggingConfigurationBuilderTest {

    private static final String CORRELATION_ID = "op-1";
    private static final String SPACE_NAME = "my-space";
    private static final UUID SPACE_GUID_UUID = UUID.randomUUID();
    private static final String SPACE_GUID = SPACE_GUID_UUID.toString();
    private static final String ORG_NAME = "my-org";
    private static final String USER_GUID = "user-guid-1";
    private static final String MTA_ID = "my-mta";
    private static final String NAMESPACE = "dev";
    private static final String SERVICE_INSTANCE = "my-cls-instance";
    private static final String SERVICE_KEY_NAME = "my-cls-key";

    private static final Map<String, Object> SERVICE_KEY_CREDENTIALS = Map.of(
        "ingest-mtls-endpoint", "https://cls.example.com",
        "server-ca", "server-ca-cert",
        "ingest-mtls-cert", "client-cert",
        "ingest-mtls-key", "client-key"
    );

    @Mock
    private CloudControllerClientFactory clientFactory;
    @Mock
    private CloudControllerClient client;
    @Mock
    private CloudControllerClientProvider clientProvider;
    @Mock
    private TokenService tokenService;
    @Mock
    private StepLogger stepLogger;

    private ProcessContext context;
    private LoggingConfigurationBuilder calculator;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        DelegateExecution execution = MockDelegateExecution.createSpyInstance();
        when(clientProvider.getControllerClient(anyString(), anyString(), anyString())).thenReturn(client);
        when(client.getTarget()).thenReturn(ImmutableCloudSpace.builder()
                                                               .metadata(ImmutableCloudMetadata.builder()
                                                                                               .guid(SPACE_GUID_UUID)
                                                                                               .build())
                                                               .name(SPACE_NAME)
                                                               .build());
        context = new ProcessContext(execution, stepLogger, clientProvider);
        context.setVariable(Variables.CORRELATION_ID, CORRELATION_ID);
        context.setVariable(Variables.SPACE_NAME, SPACE_NAME);
        context.setVariable(Variables.SPACE_GUID, SPACE_GUID);
        context.setVariable(Variables.ORGANIZATION_NAME, ORG_NAME);
        context.setVariable(Variables.USER_GUID, USER_GUID);
        context.setVariable(Variables.MTA_ID, MTA_ID);
        context.setVariable(Variables.MTA_NAMESPACE, NAMESPACE);
        calculator = new LoggingConfigurationBuilder(clientFactory, context, tokenService);
    }

    // --- exportOperationLogsToExternalSystem(Resource) ---

    @Test
    void testExportWithResource_returnsNullWhenServiceKeyIsNull() {
        when(client.getServiceKey(SERVICE_INSTANCE, SERVICE_KEY_NAME)).thenReturn(null);
        Resource resource = buildResource(SERVICE_INSTANCE, SERVICE_KEY_NAME, true);

        LoggingConfiguration result = calculator.exportOperationLogsToExternalSystem(resource);

        assertNull(result);
    }

    @Test
    void testExportWithResource_throwsWhenServiceKeyIsNullAndNotFailSafe() {
        when(client.getServiceKey(SERVICE_INSTANCE, SERVICE_KEY_NAME)).thenReturn(null);
        Resource resource = buildResource(SERVICE_INSTANCE, SERVICE_KEY_NAME, false);

        assertThrows(SLException.class, () -> calculator.exportOperationLogsToExternalSystem(resource));
    }

    @Test
    void testExportWithResource_returnsNullWhenServiceInstanceNameIsBlank() {
        // NameUtil.getServiceInstanceNameOrDefault falls back to resource.getName() when SERVICE_NAME is blank,
        // so we must also blank the resource name to hit the "invalid params" branch.
        Resource resource = Resource.createV3()
                                    .setName("")
                                    .setOptional(true)
                                    .setParameters(Map.of(SupportedParameters.SERVICE_NAME, "",
                                                          SupportedParameters.SERVICE_KEY_NAME, SERVICE_KEY_NAME));

        LoggingConfiguration result = calculator.exportOperationLogsToExternalSystem(resource);

        assertNull(result);
        verify(client, never()).getServiceKey(anyString(), anyString());
    }

    @Test
    void testExportWithResource_returnsNullWhenServiceKeyNameIsBlank() {
        Resource resource = buildResource(SERVICE_INSTANCE, "", true);

        LoggingConfiguration result = calculator.exportOperationLogsToExternalSystem(resource);

        assertNull(result);
        verify(client, never()).getServiceKey(anyString(), anyString());
    }

    @Test
    void testExportWithResource_throwsWhenMissingParametersAndNotFailSafe() {
        Resource resource = buildResource(null, SERVICE_KEY_NAME, false);

        assertThrows(SLException.class, () -> calculator.exportOperationLogsToExternalSystem(resource));
    }

    @Test
    void testExportWithResource_returnsNullWhenCloudOperationExceptionAndFailSafe() {
        when(client.getServiceKey(SERVICE_INSTANCE, SERVICE_KEY_NAME)).thenThrow(new CloudOperationException(HttpStatus.NOT_FOUND));
        Resource resource = buildResource(SERVICE_INSTANCE, SERVICE_KEY_NAME, true);

        LoggingConfiguration result = calculator.exportOperationLogsToExternalSystem(resource);

        assertNull(result);
    }

    @Test
    void testExportWithResource_throwsWhenCloudOperationExceptionAndNotFailSafe() {
        when(client.getServiceKey(SERVICE_INSTANCE, SERVICE_KEY_NAME)).thenThrow(new CloudOperationException(HttpStatus.NOT_FOUND));
        Resource resource = buildResource(SERVICE_INSTANCE, SERVICE_KEY_NAME, false);

        assertThrows(SLException.class, () -> calculator.exportOperationLogsToExternalSystem(resource));
    }

    @Test
    void testExportWithResource_populatesCredentialsFromServiceKey() {
        when(client.getServiceKey(SERVICE_INSTANCE, SERVICE_KEY_NAME)).thenReturn(
            buildServiceKey(SERVICE_KEY_NAME, SERVICE_KEY_CREDENTIALS));
        Resource resource = buildResource(SERVICE_INSTANCE, SERVICE_KEY_NAME, true);

        LoggingConfiguration result = calculator.exportOperationLogsToExternalSystem(resource);

        assertNotNull(result);
        assertEquals("https://cls.example.com", result.getEndpointUrl());
        assertEquals("server-ca-cert", result.getServerCa());
        assertEquals("client-cert", result.getClientCert());
        assertEquals("client-key", result.getClientKey());
    }

    @Test
    void testExportWithResource_populatesContextFields() {
        when(client.getServiceKey(SERVICE_INSTANCE, SERVICE_KEY_NAME)).thenReturn(
            buildServiceKey(SERVICE_KEY_NAME, SERVICE_KEY_CREDENTIALS));
        Resource resource = buildResource(SERVICE_INSTANCE, SERVICE_KEY_NAME, true);

        LoggingConfiguration result = calculator.exportOperationLogsToExternalSystem(resource);

        assertNotNull(result);
        assertEquals(CORRELATION_ID, result.getOperationId());
        assertEquals(MTA_ID, result.getMtaId());
        assertEquals(SPACE_GUID, result.getMtaSpaceId());
        assertEquals(SPACE_NAME, result.getMtaSpace());
        assertEquals(ORG_NAME, result.getMtaOrg());
        assertEquals(NAMESPACE, result.getNamespace());
    }

    @Test
    void testExportWithResource_setsFailSafeFromResourceOptional() {
        when(client.getServiceKey(SERVICE_INSTANCE, SERVICE_KEY_NAME)).thenReturn(
            buildServiceKey(SERVICE_KEY_NAME, SERVICE_KEY_CREDENTIALS));
        Resource resource = buildResource(SERVICE_INSTANCE, SERVICE_KEY_NAME, true);

        LoggingConfiguration result = calculator.exportOperationLogsToExternalSystem(resource);

        assertNotNull(result);
        assertEquals(true, result.isFailSafe());
    }

    @Test
    void testExportWithResource_defaultLogLevelIsInfo() {
        when(client.getServiceKey(SERVICE_INSTANCE, SERVICE_KEY_NAME)).thenReturn(
            buildServiceKey(SERVICE_KEY_NAME, SERVICE_KEY_CREDENTIALS));
        Resource resource = buildResource(SERVICE_INSTANCE, SERVICE_KEY_NAME, true);

        LoggingConfiguration result = calculator.exportOperationLogsToExternalSystem(resource);

        assertNotNull(result);
        assertEquals(LogLevel.INFO, result.getLogLevel());
    }

    static Stream<Arguments> testExportWithResource_logLevelFromDescriptor() {
        return Stream.of(Arguments.of("INFO", LogLevel.INFO), Arguments.of("WARN", LogLevel.WARN), Arguments.of("DEBUG", LogLevel.DEBUG),
                         Arguments.of("ERROR", LogLevel.ERROR), Arguments.of("TRACE", LogLevel.TRACE));
    }

    @ParameterizedTest
    @MethodSource
    void testExportWithResource_logLevelFromDescriptor(String descriptorLevel, LogLevel expectedLevel) {
        when(client.getServiceKey(SERVICE_INSTANCE, SERVICE_KEY_NAME)).thenReturn(
            buildServiceKey(SERVICE_KEY_NAME, SERVICE_KEY_CREDENTIALS));
        Resource resource = buildResource(SERVICE_INSTANCE, SERVICE_KEY_NAME, true, Map.of(SupportedParameters.LOG_LEVEL, descriptorLevel));

        LoggingConfiguration result = calculator.exportOperationLogsToExternalSystem(resource);

        assertNotNull(result);
        assertEquals(expectedLevel, result.getLogLevel());
    }

    @Test
    void testExportWithResource_usesResourceNameAsServiceInstanceWhenNoServiceNameParameter() {
        when(client.getServiceKey("resource-name", SERVICE_KEY_NAME)).thenReturn(
            buildServiceKey(SERVICE_KEY_NAME, SERVICE_KEY_CREDENTIALS));
        Resource resource = Resource.createV3()
                                    .setName("resource-name")
                                    .setOptional(true)
                                    .setParameters(Map.of(SupportedParameters.SERVICE_KEY_NAME, SERVICE_KEY_NAME));

        LoggingConfiguration result = calculator.exportOperationLogsToExternalSystem(resource);

        assertNotNull(result);
        assertEquals("resource-name", result.getServiceInstanceName());
    }

    @Test
    void testExportWithResource_usesDestinationOrgAndSpace() {
        when(clientFactory.createClient(any(), anyString(), anyString(), anyString())).thenReturn(client);
        when(client.getServiceKey(SERVICE_INSTANCE, SERVICE_KEY_NAME)).thenReturn(
            buildServiceKey(SERVICE_KEY_NAME, SERVICE_KEY_CREDENTIALS));
        Resource resource = buildResource(SERVICE_INSTANCE, SERVICE_KEY_NAME, true,
                                          Map.of(SupportedParameters.DESTINATION,
                                                 Map.of("org-name", "other-org", "space-name", "other-space")));

        LoggingConfiguration result = calculator.exportOperationLogsToExternalSystem(resource);

        assertNotNull(result);
        assertEquals("other-org", result.getTargetOrg());
        assertEquals("other-space", result.getTargetSpace());
    }

    // --- exportOperationLogsToExternalSystem(LoggingConfiguration, ProcessContext) ---

    @Test
    void testExportWithLoggingConfiguration_returnsNullWhenServiceKeyIsNull() {
        when(client.getServiceKey(SERVICE_INSTANCE, SERVICE_KEY_NAME)).thenReturn(null);
        LoggingConfiguration incomingConfig = buildIncomingConfig(true);

        LoggingConfiguration result = calculator.exportOperationLogsToExternalSystem(incomingConfig, context);

        assertNull(result);
    }

    @Test
    void testExportWithLoggingConfiguration_throwsWhenServiceKeyIsNullAndNotFailSafe() {
        when(client.getServiceKey(SERVICE_INSTANCE, SERVICE_KEY_NAME)).thenReturn(null);
        LoggingConfiguration incomingConfig = buildIncomingConfig(false);

        assertThrows(SLException.class, () -> calculator.exportOperationLogsToExternalSystem(incomingConfig, context));
    }

    @Test
    void testExportWithLoggingConfiguration_populatesCredentialsFromServiceKey() {
        when(client.getServiceKey(SERVICE_INSTANCE, SERVICE_KEY_NAME)).thenReturn(
            buildServiceKey(SERVICE_KEY_NAME, SERVICE_KEY_CREDENTIALS));
        LoggingConfiguration incomingConfig = buildIncomingConfig(true);

        LoggingConfiguration result = calculator.exportOperationLogsToExternalSystem(incomingConfig, context);

        assertNotNull(result);
        assertEquals("https://cls.example.com", result.getEndpointUrl());
        assertEquals("server-ca-cert", result.getServerCa());
        assertEquals("client-cert", result.getClientCert());
        assertEquals("client-key", result.getClientKey());
    }

    @Test
    void testExportWithLoggingConfiguration_setsOperationIdAndSpaceIdFromContext() {
        when(client.getServiceKey(SERVICE_INSTANCE, SERVICE_KEY_NAME)).thenReturn(
            buildServiceKey(SERVICE_KEY_NAME, SERVICE_KEY_CREDENTIALS));
        LoggingConfiguration incomingConfig = buildIncomingConfig(true);

        LoggingConfiguration result = calculator.exportOperationLogsToExternalSystem(incomingConfig, context);

        assertNotNull(result);
        assertEquals(CORRELATION_ID, result.getOperationId());
        assertEquals(SPACE_GUID, result.getMtaSpaceId());
    }

    @Test
    void testExportWithLoggingConfiguration_throwsWhenMissingCredentialInServiceKey() {
        Map<String, Object> incompleteCredentials = Map.of("ingest-mtls-endpoint", "https://cls.example.com");
        when(client.getServiceKey(SERVICE_INSTANCE, SERVICE_KEY_NAME)).thenReturn(buildServiceKey(SERVICE_KEY_NAME, incompleteCredentials));
        LoggingConfiguration incomingConfig = buildIncomingConfig(true);

        assertThrows(SLException.class, () -> calculator.exportOperationLogsToExternalSystem(incomingConfig, context));
    }

    // --- Helpers ---

    private static Resource buildResource(String serviceInstanceName, String serviceKeyName, boolean optional) {
        return buildResource(serviceInstanceName, serviceKeyName, optional, Map.of());
    }

    private static Resource buildResource(String serviceInstanceName, String serviceKeyName, boolean optional,
                                          Map<String, Object> extraParameters) {
        java.util.Map<String, Object> params = new java.util.HashMap<>(extraParameters);
        if (serviceInstanceName != null) {
            params.put(SupportedParameters.SERVICE_NAME, serviceInstanceName);
        }
        if (serviceKeyName != null) {
            params.put(SupportedParameters.SERVICE_KEY_NAME, serviceKeyName);
        }
        return Resource.createV3()
                       .setName("cls-resource")
                       .setOptional(optional)
                       .setParameters(params);
    }

    private static LoggingConfiguration buildIncomingConfig(boolean failSafe) {
        return ImmutableLoggingConfiguration.builder()
                                            .serviceInstanceName(SERVICE_INSTANCE)
                                            .serviceKeyName(SERVICE_KEY_NAME)
                                            .targetOrg(ORG_NAME)
                                            .targetSpace(SPACE_NAME)
                                            .logLevel(LogLevel.INFO)
                                            .isFailSafe(failSafe)
                                            .build();
    }

    private static ImmutableCloudServiceKey buildServiceKey(String name, Map<String, Object> credentials) {
        return ImmutableCloudServiceKey.builder()
                                       .name(name)
                                       .metadata(ImmutableCloudMetadata.builder()
                                                                       .build())
                                       .credentials(credentials)
                                       .serviceInstance(ImmutableCloudServiceInstance.builder()
                                                                                     .name(SERVICE_INSTANCE)
                                                                                     .metadata(ImmutableCloudMetadata.builder()
                                                                                                                     .guid(
                                                                                                                         UUID.randomUUID())
                                                                                                                     .build())
                                                                                     .build())
                                       .build();
    }

}
