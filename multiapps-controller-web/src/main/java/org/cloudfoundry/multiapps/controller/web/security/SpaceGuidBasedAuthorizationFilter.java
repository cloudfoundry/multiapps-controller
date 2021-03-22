package org.cloudfoundry.multiapps.controller.web.security;

import java.io.IOException;
import java.text.MessageFormat;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cloudfoundry.multiapps.controller.web.Messages;
import org.cloudfoundry.multiapps.controller.web.util.SecurityContextUtil;
import org.cloudfoundry.multiapps.controller.web.util.ServletUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.server.ResponseStatusException;

public abstract class SpaceGuidBasedAuthorizationFilter implements UriAuthorizationFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpaceGuidBasedAuthorizationFilter.class);

    private final AuthorizationChecker authorizationChecker;

    protected SpaceGuidBasedAuthorizationFilter(AuthorizationChecker authorizationChecker) {
        this.authorizationChecker = authorizationChecker;
    }

    @Override
    public final boolean ensureUserIsAuthorized(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String spaceGuid = extractAndLogSpaceGuid(request);
        try {
            authorizationChecker.ensureUserIsAuthorized(request, SecurityContextUtil.getUserInfo(), spaceGuid, null);
            return true;
        } catch (ResponseStatusException e) {
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
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("User \"%s\" is not authorized for request to \"%s\".", SecurityContextUtil.getUsername(),
                                       ServletUtil.decodeUri(request)),
                         e);
        }
    }

    protected abstract String extractSpaceGuid(HttpServletRequest request);

}
