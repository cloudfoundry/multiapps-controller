package org.cloudfoundry.multiapps.controller.core.auditlogging;

import java.util.Map;

import org.cloudfoundry.multiapps.controller.core.auditlogging.model.AuditLogConfiguration;
import org.cloudfoundry.multiapps.controller.core.auditlogging.model.ConfigurationChangeActions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class FlowableSlmpResourceAuditLogTest {

    @Mock
    private AuditLoggingFacade auditLoggingFacade;

    private FlowableSlmpResourceAuditLog auditLog;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        auditLog = new FlowableSlmpResourceAuditLog(auditLoggingFacade);
    }

    @Test
    void testAuditLogConfigurationChangeDelegatesWithProvidedAction() {
        auditLog.auditLogConfigurationChange("alice", "space", "do-thing", "the-config",
                                              ConfigurationChangeActions.CONFIGURATION_CREATE);

        Mockito.verify(auditLoggingFacade)
               .logConfigurationChangeAuditLog(Mockito.any(AuditLogConfiguration.class),
                                               Mockito.eq(ConfigurationChangeActions.CONFIGURATION_CREATE));
    }

    @Test
    void testAuditLogActionPerformedDelegatesToDataAccess() {
        auditLog.auditLogActionPerformed("alice", "space", "look", "the-config");

        Mockito.verify(auditLoggingFacade)
               .logDataAccessAuditLog(Mockito.any(AuditLogConfiguration.class));
    }

    @Test
    void testAuditLogActionPerformedWithParametersIncludesParameters() {
        auditLog.auditLogActionPerformed("alice", "space", "look", "the-config", Map.of("key", "value"));

        org.mockito.ArgumentCaptor<AuditLogConfiguration> captor = org.mockito.ArgumentCaptor.forClass(AuditLogConfiguration.class);
        Mockito.verify(auditLoggingFacade)
               .logDataAccessAuditLog(captor.capture());
        Assertions.assertTrue(captor.getValue()
                                    .getConfigurationIdentifiers()
                                    .stream()
                                    .anyMatch(identifier -> "key".equals(identifier.getIdentifierName())
                                        && "value".equals(identifier.getIdentifierValue())));
    }

}
