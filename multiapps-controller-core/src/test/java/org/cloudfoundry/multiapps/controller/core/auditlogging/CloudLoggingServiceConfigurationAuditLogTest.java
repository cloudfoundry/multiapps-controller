package org.cloudfoundry.multiapps.controller.core.auditlogging;

import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.auditlogging.impl.DefaultCloudLoggingServiceConfigurationAuditLog;
import org.cloudfoundry.multiapps.controller.core.auditlogging.model.AuditLogConfiguration;
import org.cloudfoundry.multiapps.controller.core.auditlogging.model.ConfigurationChangeActions;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableLoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.LogLevel;
import org.cloudfoundry.multiapps.controller.persistence.model.LoggingConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

class CloudLoggingServiceConfigurationAuditLogTest {

    private static final String USERNAME = "test-user";
    private static final String SPACE_ID = "space-guid-1";

    @Mock
    private AuditLoggingFacade auditLoggingFacade;

    private CloudLoggingServiceConfigurationAuditLog auditLog;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        auditLog = new DefaultCloudLoggingServiceConfigurationAuditLog(auditLoggingFacade);
    }

    @Test
    void logCreateLoggingConfiguration_invokesConfigurationChangeWithCreateAction() {
        auditLog.logCreateLoggingConfiguration(USERNAME, SPACE_ID, buildLoggingConfiguration());

        verify(auditLoggingFacade).logConfigurationChangeAuditLog(any(AuditLogConfiguration.class),
                                                                  eq(ConfigurationChangeActions.CONFIGURATION_CREATE));
    }

    @Test
    void logCreateLoggingConfiguration_setsCreateMessageAsPerformedAction() {
        auditLog.logCreateLoggingConfiguration(USERNAME, SPACE_ID, buildLoggingConfiguration());

        assertEquals(Messages.LOGGING_CONFIGURATION_CREATE_AUDIT_LOG_CONFIG, captureChangeConfig(ConfigurationChangeActions.CONFIGURATION_CREATE).getPerformedAction());
    }

    @Test
    void logUpdateLoggingConfiguration_invokesConfigurationChangeWithUpdateAction() {
        auditLog.logUpdateLoggingConfiguration(USERNAME, SPACE_ID, buildLoggingConfiguration());

        verify(auditLoggingFacade).logConfigurationChangeAuditLog(any(AuditLogConfiguration.class),
                                                                  eq(ConfigurationChangeActions.CONFIGURATION_UPDATE));
    }

    @Test
    void logUpdateLoggingConfiguration_setsUpdateMessageAsPerformedAction() {
        auditLog.logUpdateLoggingConfiguration(USERNAME, SPACE_ID, buildLoggingConfiguration());

        assertEquals(Messages.LOGGING_CONFIGURATION_UPDATE_AUDIT_LOG_CONFIG, captureChangeConfig(ConfigurationChangeActions.CONFIGURATION_UPDATE).getPerformedAction());
    }

    @Test
    void logDeleteLoggingConfiguration_invokesConfigurationChangeWithDeleteAction() {
        auditLog.logDeleteLoggingConfiguration(USERNAME, SPACE_ID, buildLoggingConfiguration());

        verify(auditLoggingFacade).logConfigurationChangeAuditLog(any(AuditLogConfiguration.class),
                                                                  eq(ConfigurationChangeActions.CONFIGURATION_DELETE));
    }

    @Test
    void logDeleteLoggingConfiguration_setsDeleteMessageAsPerformedAction() {
        auditLog.logDeleteLoggingConfiguration(USERNAME, SPACE_ID, buildLoggingConfiguration());

        assertEquals(Messages.LOGGING_CONFIGURATION_DELETE_AUDIT_LOG_CONFIG, captureChangeConfig(ConfigurationChangeActions.CONFIGURATION_DELETE).getPerformedAction());
    }

    @Test
    void logGetLoggingConfiguration_invokesDataAccessAuditLog() {
        auditLog.logGetLoggingConfiguration(USERNAME, SPACE_ID, buildLoggingConfiguration());

        verify(auditLoggingFacade).logDataAccessAuditLog(any(AuditLogConfiguration.class));
    }

    @Test
    void logGetLoggingConfiguration_setsGetMessageAsPerformedAction() {
        auditLog.logGetLoggingConfiguration(USERNAME, SPACE_ID, buildLoggingConfiguration());

        assertEquals(Messages.LOGGING_CONFIGURATION_GET_AUDIT_LOG_CONFIG, captureDataAccessConfig().getPerformedAction());
    }

    private AuditLogConfiguration captureChangeConfig(ConfigurationChangeActions action) {
        ArgumentCaptor<AuditLogConfiguration> captor = ArgumentCaptor.forClass(AuditLogConfiguration.class);
        verify(auditLoggingFacade).logConfigurationChangeAuditLog(captor.capture(), eq(action));
        return captor.getValue();
    }

    private AuditLogConfiguration captureDataAccessConfig() {
        ArgumentCaptor<AuditLogConfiguration> captor = ArgumentCaptor.forClass(AuditLogConfiguration.class);
        verify(auditLoggingFacade).logDataAccessAuditLog(captor.capture());
        return captor.getValue();
    }

    private static LoggingConfiguration buildLoggingConfiguration() {
        return ImmutableLoggingConfiguration.builder()
                                            .id("logging-config-1")
                                            .mtaId("my-mta")
                                            .logLevel(LogLevel.INFO)
                                            .isFailSafe(true)
                                            .build();
    }
}
