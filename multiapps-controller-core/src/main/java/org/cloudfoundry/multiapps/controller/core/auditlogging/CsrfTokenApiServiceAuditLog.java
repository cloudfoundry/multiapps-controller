package org.cloudfoundry.multiapps.controller.core.auditlogging;

import java.text.MessageFormat;

import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.auditlogging.model.ExtentensionAuditLog;

public class CsrfTokenApiServiceAuditLog {

    public static void auditLogGetInfo(String username) {
        String performedAction = Messages.RETRIEVE_CSRF_TOKEN_AUDIT_LOG_MESSAGE;
        AuditLoggingProvider.getFacade()
                            .logDataAccessAuditLog(new ExtentensionAuditLog(username,
                                                                            "",
                                                                            performedAction,
                                                                            Messages.GET_CSRF_TOKEN_AUDIT_LOG_CONFIG));
    }
}
