package org.cloudfoundry.multiapps.controller.core.auditlogging;

import org.cloudfoundry.multiapps.controller.core.auditlogging.model.AuditLogConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class LoginAttemptAuditLogTest {

    @Mock
    private AuditLoggingFacade auditLoggingFacade;

    private LoginAttemptAuditLog auditLog;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        auditLog = new LoginAttemptAuditLog(auditLoggingFacade);
    }

    @Test
    void testLogLoginAttemptEmitsSecurityIncident() {
        auditLog.logLoginAttempt("alice", "space-guid", "User {0} attempted login on space {1}", "the-config");

        ArgumentCaptor<AuditLogConfiguration> captor = ArgumentCaptor.forClass(AuditLogConfiguration.class);
        Mockito.verify(auditLoggingFacade)
               .logSecurityIncident(captor.capture());
        Assertions.assertEquals("alice", captor.getValue()
                                               .getUserId());
        Assertions.assertEquals("space-guid", captor.getValue()
                                                    .getSpaceId());
        Assertions.assertEquals("User alice attempted login on space space-guid", captor.getValue()
                                                                                        .getPerformedAction());
    }

}
