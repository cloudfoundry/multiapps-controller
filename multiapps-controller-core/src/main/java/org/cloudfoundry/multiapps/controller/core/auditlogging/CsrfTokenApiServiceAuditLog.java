package org.cloudfoundry.multiapps.controller.core.auditlogging;

import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.auditlogging.model.AuditLogConfiguration;

public class CsrfTokenApiServiceAuditLog {

    private final AuditLoggingFacade auditLoggingFacade;

    public CsrfTokenApiServiceAuditLog(AuditLoggingFacade auditLoggingFacade) {
        this.auditLoggingFacade = auditLoggingFacade;
    }

    public void logGetInfo(String username) {
        String performedAction = Messages.RETRIEVE_CSRF_TOKEN_AUDIT_LOG_MESSAGE;
        auditLoggingFacade.logDataAccessAuditLog(new AuditLogConfiguration(username,
                                                                           "",
                                                                           performedAction,
                                                                           Messages.GET_CSRF_TOKEN_AUDIT_LOG_CONFIG));
    }
}
