package org.cloudfoundry.multiapps.controller.core.auditlogging;

import org.cloudfoundry.multiapps.controller.core.auditlogging.model.AuditLogConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class AuthenticationAuditLogTest {

    @Mock
    private AuditLoggingFacade auditLoggingFacade;

    private AuthenticationAuditLog auditLog;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        auditLog = new AuthenticationAuditLog(auditLoggingFacade);
    }

    @Test
    void testLogFetchTokenAttemptEmitsSecurityIncident() {
        auditLog.logFetchTokenAttempt("client-1", "space-guid", "service-1");

        AuditLogConfiguration captured = captureSecurityIncident();
        Assertions.assertEquals("client-1", captured.getUserId());
        Assertions.assertEquals("space-guid", captured.getSpaceId());
    }

    @Test
    void testLogFailedToFetchTokenAttemptEmitsSecurityIncident() {
        auditLog.logFailedToFetchTokenAttempt("client-1", "space-guid", "service-1");

        AuditLogConfiguration captured = captureSecurityIncident();
        Assertions.assertEquals("client-1", captured.getUserId());
        Assertions.assertEquals("space-guid", captured.getSpaceId());
    }

    private AuditLogConfiguration captureSecurityIncident() {
        ArgumentCaptor<AuditLogConfiguration> captor = ArgumentCaptor.forClass(AuditLogConfiguration.class);
        Mockito.verify(auditLoggingFacade)
               .logSecurityIncident(captor.capture());
        return captor.getValue();
    }

}
