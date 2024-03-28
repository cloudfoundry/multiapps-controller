package org.cloudfoundry.multiapps.controller.web.security;

import org.cloudfoundry.multiapps.controller.core.auditlogging.AuditLoggingProvider;
import org.cloudfoundry.multiapps.controller.core.auditlogging.model.ExtentensionAuditLog;
import org.cloudfoundry.multiapps.controller.web.Messages;
import org.cloudfoundry.multiapps.controller.web.util.SecurityContextUtil;

import java.io.IOException;
import java.text.MessageFormat;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface UriAuthorizationFilter {

    String getUriRegex();

    /**
     * @return Whether or not the request should be forwarded to the rest of the filter chain and eventually to the appropriate handler.
     */
    boolean ensureUserIsAuthorized(HttpServletRequest request, HttpServletResponse response) throws IOException;

    default void auditLogLoginAttempt(String username, String spaceGuid, String message) {
        String performedAction = MessageFormat.format(message, username, spaceGuid);
        AuditLoggingProvider.getFacade()
                .logSecurityIncident(new ExtentensionAuditLog(SecurityContextUtil.getUsername(),
                        spaceGuid,
                        performedAction,
                        Messages.LOGIN_ATTEMPT_AUDIT_LOG_CONFIG));
    }
}
