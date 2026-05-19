package org.cloudfoundry.multiapps.controller.core.auditlogging;

import org.cloudfoundry.multiapps.controller.core.auditlogging.model.AuditLogConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class InfoApiServiceAuditLogTest {

    @Mock
    private AuditLoggingFacade auditLoggingFacade;

    private InfoApiServiceAuditLog auditLog;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        auditLog = new InfoApiServiceAuditLog(auditLoggingFacade);
    }

    @Test
    void testLogGetInfoEmitsDataAccessForUser() {
        auditLog.logGetInfo("alice");

        ArgumentCaptor<AuditLogConfiguration> captor = ArgumentCaptor.forClass(AuditLogConfiguration.class);
        Mockito.verify(auditLoggingFacade)
               .logDataAccessAuditLog(captor.capture());
        Assertions.assertEquals("alice", captor.getValue()
                                               .getUserId());
    }

}
