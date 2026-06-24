package org.cloudfoundry.multiapps.controller.core.auditlogging;

import java.util.Map;

import org.cloudfoundry.multiapps.controller.core.auditlogging.model.AuditLogConfiguration;
import org.cloudfoundry.multiapps.controller.core.auditlogging.model.ConfigurationChangeActions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class FlowableSlppResourceAuditLogTest {

    private static final String USERNAME = "alice";
    private static final String SPACE_ID = "space";
    private static final String ACTION = "do-thing";
    private static final String CONFIGURATION = "the-config";

    @Mock
    private AuditLoggingFacade auditLoggingFacade;

    private FlowableSlppResourceAuditLog auditLog;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        auditLog = new FlowableSlppResourceAuditLog(auditLoggingFacade);
    }

    @Test
    void testAuditLogConfigurationChangeForwardsActionAndCapturedFields() {
        auditLog.auditLogConfigurationChange(USERNAME, SPACE_ID, ACTION, CONFIGURATION,
                                             ConfigurationChangeActions.CONFIGURATION_DELETE);

        ArgumentCaptor<AuditLogConfiguration> captor = ArgumentCaptor.forClass(AuditLogConfiguration.class);
        Mockito.verify(auditLoggingFacade)
               .logConfigurationChangeAuditLog(captor.capture(), Mockito.eq(ConfigurationChangeActions.CONFIGURATION_DELETE));
        AuditLogConfiguration captured = captor.getValue();
        Assertions.assertEquals(USERNAME, captured.getUserId());
        Assertions.assertEquals(SPACE_ID, captured.getSpaceId());
        Assertions.assertEquals(ACTION, captured.getPerformedAction());
        Assertions.assertEquals(CONFIGURATION, captured.getConfigurationName());
    }

    @Test
    void testAuditLogConfigurationChangeWithParametersIncludesParameters() {
        auditLog.auditLogConfigurationChange(USERNAME, SPACE_ID, ACTION, CONFIGURATION, Map.of("k", "v"),
                                             ConfigurationChangeActions.CONFIGURATION_UPDATE);

        ArgumentCaptor<AuditLogConfiguration> captor = ArgumentCaptor.forClass(AuditLogConfiguration.class);
        Mockito.verify(auditLoggingFacade)
               .logConfigurationChangeAuditLog(captor.capture(), Mockito.eq(ConfigurationChangeActions.CONFIGURATION_UPDATE));
        AuditLogConfiguration captured = captor.getValue();
        Assertions.assertEquals(USERNAME, captured.getUserId());
        Assertions.assertEquals(ACTION, captured.getPerformedAction());
        Assertions.assertTrue(captured.getConfigurationIdentifiers()
                                      .stream()
                                      .anyMatch(id -> "k".equals(id.getIdentifierName()) && "v".equals(id.getIdentifierValue())));
    }

    @Test
    void testAuditLogActionPerformedForwardsCapturedFieldsToDataAccess() {
        auditLog.auditLogActionPerformed(USERNAME, SPACE_ID, ACTION, CONFIGURATION);

        ArgumentCaptor<AuditLogConfiguration> captor = ArgumentCaptor.forClass(AuditLogConfiguration.class);
        Mockito.verify(auditLoggingFacade)
               .logDataAccessAuditLog(captor.capture());
        AuditLogConfiguration captured = captor.getValue();
        Assertions.assertEquals(USERNAME, captured.getUserId());
        Assertions.assertEquals(SPACE_ID, captured.getSpaceId());
        Assertions.assertEquals(ACTION, captured.getPerformedAction());
        Assertions.assertEquals(CONFIGURATION, captured.getConfigurationName());
    }

    @Test
    void testAuditLogActionPerformedWithParametersIncludesParameters() {
        auditLog.auditLogActionPerformed(USERNAME, SPACE_ID, ACTION, CONFIGURATION, Map.of("k", "v"));

        ArgumentCaptor<AuditLogConfiguration> captor = ArgumentCaptor.forClass(AuditLogConfiguration.class);
        Mockito.verify(auditLoggingFacade)
               .logDataAccessAuditLog(captor.capture());
        AuditLogConfiguration captured = captor.getValue();
        Assertions.assertEquals(USERNAME, captured.getUserId());
        Assertions.assertEquals(ACTION, captured.getPerformedAction());
        Assertions.assertTrue(captured.getConfigurationIdentifiers()
                                      .stream()
                                      .anyMatch(id -> "k".equals(id.getIdentifierName()) && "v".equals(id.getIdentifierValue())));
    }

}
