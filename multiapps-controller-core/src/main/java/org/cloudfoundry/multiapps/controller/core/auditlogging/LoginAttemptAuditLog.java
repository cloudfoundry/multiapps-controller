package org.cloudfoundry.multiapps.controller.core.auditlogging;

import java.text.MessageFormat;

import org.cloudfoundry.multiapps.controller.core.auditlogging.model.AuditLogConfiguration;

public class LoginAttemptAuditLog {

    private final AuditLoggingFacade auditLoggingFacade;

    public LoginAttemptAuditLog(AuditLoggingFacade auditLoggingFacade) {
        this.auditLoggingFacade = auditLoggingFacade;
    }

    public void logLoginAttempt(String username, String spaceGuid, String message, String configuration) {
        String performedAction = MessageFormat.format(message, username, spaceGuid);
        auditLoggingFacade.logSecurityIncident(new AuditLogConfiguration(username, spaceGuid, performedAction, configuration));
    }
}
