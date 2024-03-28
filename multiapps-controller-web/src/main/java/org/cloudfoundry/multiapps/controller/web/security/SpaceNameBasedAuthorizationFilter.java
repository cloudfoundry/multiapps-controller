package org.cloudfoundry.multiapps.controller.web.security;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cloudfoundry.multiapps.controller.core.auditlogging.AuditLoggingProvider;
import org.cloudfoundry.multiapps.controller.core.auditlogging.model.ExtentensionAuditLog;
import org.cloudfoundry.multiapps.controller.persistence.model.CloudTarget;
import org.cloudfoundry.multiapps.controller.web.Messages;
import org.cloudfoundry.multiapps.controller.web.util.SecurityContextUtil;
import org.cloudfoundry.multiapps.controller.web.util.ServletUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.server.ResponseStatusException;

public abstract class SpaceNameBasedAuthorizationFilter implements UriAuthorizationFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpaceNameBasedAuthorizationFilter.class);

    private final AuthorizationChecker authorizationChecker;

    protected SpaceNameBasedAuthorizationFilter(AuthorizationChecker authorizationChecker) {
        this.authorizationChecker = authorizationChecker;
    }

    @Override
    public final boolean ensureUserIsAuthorized(HttpServletRequest request, HttpServletResponse response) throws IOException {
        CloudTarget target = extractAndLogTarget(request);
        auditLogLoginAttempt(SecurityContextUtil.getUsername(), target.getSpaceName(), Messages.USER_TRYING_TO_LOGIN_AUDIT_LOG_MESSAGE);

        try {
            authorizationChecker.ensureUserIsAuthorized(request, SecurityContextUtil.getUserInfo(), target, null);
            auditLogLoginAttempt(SecurityContextUtil.getUsername(), target.getSpaceName(), Messages.USER_SUCCESSFULLY_LOGGED_IN_AUDIT_LOG_MESSAGE);
            return true;
        } catch (ResponseStatusException e) {
            auditLogLoginAttempt(SecurityContextUtil.getUsername(), target.getSpaceName(), Messages.USER_FAILED_TO_LOG_IN_AUDIT_LOG_MESSAGE);
            logUnauthorizedRequest(request, e);
            response.sendError(e.getStatus()
                                .value(),
                               MessageFormat.format(Messages.NOT_AUTHORIZED_TO_OPERATE_IN_ORGANIZATION_0_AND_SPACE_1,
                                                    target.getOrganizationName(), target.getSpaceName()));
            return false;
        }
    }

    private CloudTarget extractAndLogTarget(HttpServletRequest request) {
        CloudTarget target = extractTarget(request);
        LOGGER.trace("Extracted target from request to \"{}\": {}", ServletUtil.decodeUri(request), target);
        return target;
    }

    private void logUnauthorizedRequest(HttpServletRequest request, ResponseStatusException e) {
        if (LOGGER.isDebugEnabled()) {
            String userName = SecurityContextUtil.getUsername();
            LOGGER.debug(String.format("User \"%s\" is not authorized for request to \"%s\".", userName, ServletUtil.decodeUri(request)),
                         e);
        }
    }

    protected abstract CloudTarget extractTarget(HttpServletRequest request);

}
