package org.cloudfoundry.multiapps.controller.core.auditlogging;

import java.text.MessageFormat;

import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.auditlogging.model.AuditLogConfiguration;

public class AuthenticationAuditLog {

    private final AuditLoggingFacade auditLoggingFacade;

    public AuthenticationAuditLog(AuditLoggingFacade auditLoggingFacade) {
        this.auditLoggingFacade = auditLoggingFacade;
    }

    public void logFetchTokenAttempt(String clientId, String spaceId, String serviceName) {
        String actionPerformed = MessageFormat.format(Messages.FETCH_TOKEN_AUDIT_LOG_MESSAGE, clientId, spaceId, serviceName);
        auditLoggingFacade.logSecurityIncident(new AuditLogConfiguration(clientId,
                                                                         spaceId,
                                                                         actionPerformed,
                                                                         Messages.FETCH_TOKEN_AUDIT_LOG_CONFIG));
    }

    public void logFailedToFetchTokenAttempt(String clientId, String spaceId, String serviceName) {
        String actionPerformed = MessageFormat.format(Messages.FAILED_TO_FETCH_TOKEN_AUDIT_LOG_MESSAGE, clientId, spaceId, serviceName);
        auditLoggingFacade.logSecurityIncident(new AuditLogConfiguration(clientId,
                                                                         spaceId,
                                                                         actionPerformed,
                                                                         Messages.FETCH_TOKEN_AUDIT_LOG_CONFIG));
    }
}
