package org.cloudfoundry.multiapps.controller.core.auditlogging;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.multiapps.controller.core.auditlogging.model.AuditLogConfiguration;
import org.cloudfoundry.multiapps.controller.core.auditlogging.model.ConfigurationChangeActions;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableLoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.LogLevel;
import org.cloudfoundry.multiapps.controller.persistence.model.LoggingConfiguration;
import org.cloudfoundry.multiapps.mta.model.ConfigurationIdentifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

class CloudLoggingServiceConfigurationAuditLogTest {

    private static final String USERNAME = "test-user";
    private static final String SPACE_ID = "space-guid-1";
    private static final String LOGGING_CONFIG_ID = "logging-config-1";
    private static final String MTA_ID = "my-mta";
    private static final String MTA_SPACE = "my-space";
    private static final String MTA_SPACE_ID = "mta-space-guid-1";
    private static final String MTA_ORG = "my-org";
    private static final String NAMESPACE = "dev";
    private static final String TARGET_SPACE = "target-space";
    private static final String TARGET_ORG = "target-org";
    private static final String SERVICE_INSTANCE_NAME = "my-cls-instance";
    private static final String SERVICE_KEY_NAME = "my-cls-key";

    @Mock
    private AuditLoggingFacade auditLoggingFacade;

    private CloudLoggingServiceConfigurationAuditLog auditLog;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        auditLog = new CloudLoggingServiceConfigurationAuditLog(auditLoggingFacade);
    }

    @Test
    void testLogCreateLoggingConfiguration_invokesFacadeWithCreateAction() {
        auditLog.logCreateLoggingConfiguration(USERNAME, SPACE_ID, buildLoggingConfiguration());

        verify(auditLoggingFacade).logConfigurationChangeAuditLog(any(AuditLogConfiguration.class),
                                                                  eqAction(ConfigurationChangeActions.CONFIGURATION_CREATE));
    }

    @Test
    void testLogCreateLoggingConfiguration_setsUserAndSpace() {
        auditLog.logCreateLoggingConfiguration(USERNAME, SPACE_ID, buildLoggingConfiguration());

        AuditLogConfiguration captured = captureCreate();
        assertEquals(USERNAME, captured.getUserId());
        assertEquals(SPACE_ID, captured.getSpaceId());
    }

    @Test
    void testLogCreateLoggingConfiguration_setsPerformedActionContainingSpaceId() {
        auditLog.logCreateLoggingConfiguration(USERNAME, SPACE_ID, buildLoggingConfiguration());

        AuditLogConfiguration captured = captureCreate();
        assertTrue(captured.getPerformedAction()
                           .contains(SPACE_ID));
    }

    @Test
    void testLogCreateLoggingConfiguration_includesAllIdentifiers() {
        auditLog.logCreateLoggingConfiguration(USERNAME, SPACE_ID, buildLoggingConfiguration());

        Map<String, String> identifiers = identifiersFromCreate();
        assertEquals(LOGGING_CONFIG_ID, identifiers.get("id"));
        assertEquals(MTA_ID, identifiers.get("mtaId"));
        assertEquals(MTA_SPACE, identifiers.get("mtaSpace"));
        assertEquals(MTA_SPACE_ID, identifiers.get("mtaSpaceId"));
        assertEquals(MTA_ORG, identifiers.get("mtaOrg"));
        assertEquals(NAMESPACE, identifiers.get("namespace"));
        assertEquals(TARGET_SPACE, identifiers.get("targetSpace"));
        assertEquals(TARGET_ORG, identifiers.get("targetOrg"));
        assertEquals(SERVICE_INSTANCE_NAME, identifiers.get("serviceInstanceName"));
        assertEquals(SERVICE_KEY_NAME, identifiers.get("serviceKeyName"));
        assertEquals("INFO", identifiers.get("logLevel"));
        assertEquals("true", identifiers.get("isFailSafe"));
    }

    @Test
    void testLogCreateLoggingConfiguration_logLevelIsNullStringWhenLogLevelIsNull() {
        LoggingConfiguration config = ImmutableLoggingConfiguration.builder()
                                                                   .from(buildLoggingConfiguration())
                                                                   .logLevel(null)
                                                                   .build();

        auditLog.logCreateLoggingConfiguration(USERNAME, SPACE_ID, config);

        assertEquals("null", identifiersFromCreate().get("logLevel"));
    }

    @Test
    void testLogUpdateLoggingConfiguration_invokesFacadeWithUpdateAction() {
        auditLog.logUpdateLoggingConfiguration(USERNAME, SPACE_ID, buildLoggingConfiguration());

        verify(auditLoggingFacade).logConfigurationChangeAuditLog(any(AuditLogConfiguration.class),
                                                                  eqAction(ConfigurationChangeActions.CONFIGURATION_UPDATE));
    }

    @Test
    void testLogUpdateLoggingConfiguration_setsUserAndSpace() {
        auditLog.logUpdateLoggingConfiguration(USERNAME, SPACE_ID, buildLoggingConfiguration());

        AuditLogConfiguration captured = captureUpdate();
        assertEquals(USERNAME, captured.getUserId());
        assertEquals(SPACE_ID, captured.getSpaceId());
    }

    @Test
    void testLogUpdateLoggingConfiguration_includesAllIdentifiers() {
        auditLog.logUpdateLoggingConfiguration(USERNAME, SPACE_ID, buildLoggingConfiguration());

        Map<String, String> identifiers = identifiersFromUpdate();
        assertEquals(LOGGING_CONFIG_ID, identifiers.get("id"));
        assertEquals(MTA_ID, identifiers.get("mtaId"));
        assertEquals(NAMESPACE, identifiers.get("namespace"));
    }

    @Test
    void testLogDeleteLoggingConfiguration_invokesFacadeWithDeleteAction() {
        auditLog.logDeleteLoggingConfiguration(USERNAME, SPACE_ID, buildLoggingConfiguration());

        verify(auditLoggingFacade).logConfigurationChangeAuditLog(any(AuditLogConfiguration.class),
                                                                  eqAction(ConfigurationChangeActions.CONFIGURATION_DELETE));
    }

    @Test
    void testLogDeleteLoggingConfiguration_setsUserAndSpace() {
        auditLog.logDeleteLoggingConfiguration(USERNAME, SPACE_ID, buildLoggingConfiguration());

        AuditLogConfiguration captured = captureDelete();
        assertEquals(USERNAME, captured.getUserId());
        assertEquals(SPACE_ID, captured.getSpaceId());
    }

    @Test
    void testLogDeleteLoggingConfiguration_includesAllIdentifiers() {
        auditLog.logDeleteLoggingConfiguration(USERNAME, SPACE_ID, buildLoggingConfiguration());

        Map<String, String> identifiers = identifiersFromDelete();
        assertEquals(LOGGING_CONFIG_ID, identifiers.get("id"));
        assertEquals(MTA_ID, identifiers.get("mtaId"));
        assertEquals(NAMESPACE, identifiers.get("namespace"));
    }

    @Test
    void testLogDeleteLoggingConfiguration_omitsNullValuesFromConfigurationIdentifiers() {
        LoggingConfiguration sparseConfig = ImmutableLoggingConfiguration.builder()
                                                                         .id(LOGGING_CONFIG_ID)
                                                                         .build();

        auditLog.logDeleteLoggingConfiguration(USERNAME, SPACE_ID, sparseConfig);

        AuditLogConfiguration captured = captureDelete();
        Map<String, String> identifiers = identifiersFromConfig(captured);
        assertEquals(LOGGING_CONFIG_ID, identifiers.get("id"));
        // null fields should not be exposed in getConfigurationIdentifiers
        for (ConfigurationIdentifier identifier : captured.getConfigurationIdentifiers()) {
            assertNotNull(identifier.getIdentifierValue(), "Configuration identifier value should not be null");
        }
    }

    @Test
    void testLogGetLoggingConfiguration_invokesDataAccessFacade() {
        auditLog.logGetLoggingConfiguration(USERNAME, SPACE_ID, buildLoggingConfiguration());

        verify(auditLoggingFacade).logDataAccessAuditLog(any(AuditLogConfiguration.class));
    }

    @Test
    void testLogGetLoggingConfiguration_setsUserAndSpace() {
        auditLog.logGetLoggingConfiguration(USERNAME, SPACE_ID, buildLoggingConfiguration());

        AuditLogConfiguration captured = captureDataAccess();
        assertEquals(USERNAME, captured.getUserId());
        assertEquals(SPACE_ID, captured.getSpaceId());
    }

    @Test
    void testLogGetLoggingConfiguration_includesAllConfigurationIdentifiers() {
        auditLog.logGetLoggingConfiguration(USERNAME, SPACE_ID, buildLoggingConfiguration());

        Map<String, String> identifiers = identifiersFromDataAccess();
        assertEquals(MTA_ID, identifiers.get("mtaId"));
        assertEquals(NAMESPACE, identifiers.get("namespace"));
        // The "get" variant logs the full set of configuration identifiers
        assertEquals(12, countNonReservedIdentifiers(captureDataAccess()));
    }

    private AuditLogConfiguration captureCreate() {
        ArgumentCaptor<AuditLogConfiguration> captor = ArgumentCaptor.forClass(AuditLogConfiguration.class);
        verify(auditLoggingFacade).logConfigurationChangeAuditLog(captor.capture(),
                                                                  eqAction(ConfigurationChangeActions.CONFIGURATION_CREATE));
        return captor.getValue();
    }

    private AuditLogConfiguration captureUpdate() {
        ArgumentCaptor<AuditLogConfiguration> captor = ArgumentCaptor.forClass(AuditLogConfiguration.class);
        verify(auditLoggingFacade).logConfigurationChangeAuditLog(captor.capture(),
                                                                  eqAction(ConfigurationChangeActions.CONFIGURATION_UPDATE));
        return captor.getValue();
    }

    private AuditLogConfiguration captureDelete() {
        ArgumentCaptor<AuditLogConfiguration> captor = ArgumentCaptor.forClass(AuditLogConfiguration.class);
        verify(auditLoggingFacade).logConfigurationChangeAuditLog(captor.capture(),
                                                                  eqAction(ConfigurationChangeActions.CONFIGURATION_DELETE));
        return captor.getValue();
    }

    private AuditLogConfiguration captureDataAccess() {
        ArgumentCaptor<AuditLogConfiguration> captor = ArgumentCaptor.forClass(AuditLogConfiguration.class);
        verify(auditLoggingFacade).logDataAccessAuditLog(captor.capture());
        return captor.getValue();
    }

    private Map<String, String> identifiersFromCreate() {
        return identifiersFromConfig(captureCreate());
    }

    private Map<String, String> identifiersFromUpdate() {
        return identifiersFromConfig(captureUpdate());
    }

    private Map<String, String> identifiersFromDelete() {
        return identifiersFromConfig(captureDelete());
    }

    private Map<String, String> identifiersFromDataAccess() {
        return identifiersFromConfig(captureDataAccess());
    }

    private Map<String, String> identifiersFromConfig(AuditLogConfiguration config) {
        Map<String, String> result = new HashMap<>();
        List<ConfigurationIdentifier> configurationIdentifiers = config.getConfigurationIdentifiers();
        for (ConfigurationIdentifier identifier : configurationIdentifiers) {
            result.put(identifier.getIdentifierName(), identifier.getIdentifierValue());
        }
        return result;
    }

    private int countNonReservedIdentifiers(AuditLogConfiguration config) {
        int count = 0;
        for (ConfigurationIdentifier identifier : config.getConfigurationIdentifiers()) {
            String name = identifier.getIdentifierName();
            if (!"performed_action".equals(name) && !"time".equals(name) && !"spaceId".equals(name)) {
                count++;
            }
        }
        return count;
    }

    private static LoggingConfiguration buildLoggingConfiguration() {
        return ImmutableLoggingConfiguration.builder()
                                            .id(LOGGING_CONFIG_ID)
                                            .mtaId(MTA_ID)
                                            .mtaSpace(MTA_SPACE)
                                            .mtaSpaceId(MTA_SPACE_ID)
                                            .mtaOrg(MTA_ORG)
                                            .namespace(NAMESPACE)
                                            .targetSpace(TARGET_SPACE)
                                            .targetOrg(TARGET_ORG)
                                            .serviceInstanceName(SERVICE_INSTANCE_NAME)
                                            .serviceKeyName(SERVICE_KEY_NAME)
                                            .logLevel(LogLevel.INFO)
                                            .isFailSafe(true)
                                            .build();
    }

    private static ConfigurationChangeActions eqAction(ConfigurationChangeActions action) {
        return org.mockito.ArgumentMatchers.eq(action);
    }
}
