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
    void testAuditLogConfigurationChangeDelegates() {
        auditLog.auditLogConfigurationChange("alice", "space", "do-thing", "the-config",
                                              ConfigurationChangeActions.CONFIGURATION_DELETE);

        Mockito.verify(auditLoggingFacade)
               .logConfigurationChangeAuditLog(Mockito.any(AuditLogConfiguration.class),
                                               Mockito.eq(ConfigurationChangeActions.CONFIGURATION_DELETE));
    }

    @Test
    void testAuditLogConfigurationChangeWithParametersIncludesParameters() {
        auditLog.auditLogConfigurationChange("alice", "space", "do-thing", "the-config", Map.of("k", "v"),
                                              ConfigurationChangeActions.CONFIGURATION_UPDATE);

        ArgumentCaptor<AuditLogConfiguration> captor = ArgumentCaptor.forClass(AuditLogConfiguration.class);
        Mockito.verify(auditLoggingFacade)
               .logConfigurationChangeAuditLog(captor.capture(), Mockito.eq(ConfigurationChangeActions.CONFIGURATION_UPDATE));
        Assertions.assertTrue(captor.getValue()
                                    .getConfigurationIdentifiers()
                                    .stream()
                                    .anyMatch(id -> "k".equals(id.getIdentifierName()) && "v".equals(id.getIdentifierValue())));
    }

    @Test
    void testAuditLogActionPerformedDelegatesToDataAccess() {
        auditLog.auditLogActionPerformed("alice", "space", "look", "the-config");

        Mockito.verify(auditLoggingFacade)
               .logDataAccessAuditLog(Mockito.any(AuditLogConfiguration.class));
    }

    @Test
    void testAuditLogActionPerformedWithParametersDelegatesToDataAccess() {
        auditLog.auditLogActionPerformed("alice", "space", "look", "the-config", Map.of("k", "v"));

        Mockito.verify(auditLoggingFacade)
               .logDataAccessAuditLog(Mockito.any(AuditLogConfiguration.class));
    }

}
