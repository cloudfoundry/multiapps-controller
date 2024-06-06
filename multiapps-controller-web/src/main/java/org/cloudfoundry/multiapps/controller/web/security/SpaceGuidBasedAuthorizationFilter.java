package org.cloudfoundry.multiapps.controller.web.security;

import java.io.IOException;
import java.text.MessageFormat;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cloudfoundry.multiapps.controller.core.auditlogging.LoginAttemptAuditLog;
import org.cloudfoundry.multiapps.controller.web.Messages;
import org.cloudfoundry.multiapps.controller.web.util.SecurityContextUtil;
import org.cloudfoundry.multiapps.controller.web.util.ServletUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.server.ResponseStatusException;

public abstract class SpaceGuidBasedAuthorizationFilter extends AbstractUriAuthorizationFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpaceGuidBasedAuthorizationFilter.class);

    private final AuthorizationChecker authorizationChecker;

    private final LoginAttemptAuditLog loginAttemptAuditLog;

    protected SpaceGuidBasedAuthorizationFilter(AuthorizationChecker authorizationChecker, LoginAttemptAuditLog loginAttemptAuditLog) {
        this.authorizationChecker = authorizationChecker;
        this.loginAttemptAuditLog = loginAttemptAuditLog;
    }

    @Override
    public final boolean ensureUserIsAuthorized(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String spaceGuid = extractAndLogSpaceGuid(request);
        loginAttemptAuditLog.logLoginAttempt(SecurityContextUtil.getUsername(), spaceGuid, Messages.USER_TRYING_TO_LOGIN_AUDIT_LOG_MESSAGE,
                                             Messages.LOGIN_ATTEMPT_AUDIT_LOG_CONFIG);
        try {
            authorizationChecker.ensureUserIsAuthorized(request, SecurityContextUtil.getUserInfo(), spaceGuid, null);
            loginAttemptAuditLog.logLoginAttempt(SecurityContextUtil.getUsername(), spaceGuid,
                                                 Messages.USER_SUCCESSFULLY_LOGGED_IN_AUDIT_LOG_MESSAGE,
                                                 Messages.LOGIN_ATTEMPT_AUDIT_LOG_CONFIG);
            return true;
        } catch (ResponseStatusException e) {
            loginAttemptAuditLog.logLoginAttempt(SecurityContextUtil.getUsername(), spaceGuid,
                                                 Messages.USER_FAILED_TO_LOG_IN_AUDIT_LOG_MESSAGE, Messages.LOGIN_ATTEMPT_AUDIT_LOG_CONFIG);
            logUnauthorizedRequest(request, e);
            response.sendError(e.getStatus()
                                .value(),
                               MessageFormat.format(Messages.NOT_AUTHORIZED_TO_OPERATE_IN_SPACE_WITH_GUID_0, spaceGuid));
            return false;
        }
    }

    private String extractAndLogSpaceGuid(HttpServletRequest request) {
        String spaceGuid = extractSpaceGuid(request);
        LOGGER.trace("Extracted space GUID \"{}\" from request to \"{}\".", spaceGuid, ServletUtil.decodeUri(request));
        return spaceGuid;
    }

    private void logUnauthorizedRequest(HttpServletRequest request, ResponseStatusException e) {
        LOGGER.error(String.format("User with GUID \"%s\" is not authorized for request to \"%s\".", extractUserGuid(),
                                   ServletUtil.decodeUri(request)),
                     e);
    }

    protected abstract String extractSpaceGuid(HttpServletRequest request);
}
