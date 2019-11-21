package com.sap.cloud.lm.sl.cf.web.security;

import java.io.IOException;
import java.text.MessageFormat;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.sap.cloud.lm.sl.cf.web.message.Messages;
import com.sap.cloud.lm.sl.cf.web.util.SecurityContextUtil;
import com.sap.cloud.lm.sl.cf.web.util.ServletUtils;

public abstract class SpaceGuidBasedAuthorizationFilter implements UriAuthorizationFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpaceGuidBasedAuthorizationFilter.class);

    private final AuthorizationChecker authorizationChecker;

    public SpaceGuidBasedAuthorizationFilter(AuthorizationChecker authorizationChecker) {
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
            response.sendError(HttpStatus.UNAUTHORIZED.value(),
                               MessageFormat.format(Messages.NOT_AUTHORIZED_TO_OPERATE_IN_SPACE_WITH_GUID_0, spaceGuid));
            return false;
        }
    }

    private String extractAndLogSpaceGuid(HttpServletRequest request) {
        String spaceGuid = extractSpaceGuid(request);
        LOGGER.trace("Extracted space GUID \"{}\" from request to \"{}\".", spaceGuid, ServletUtils.decodeUri(request));
        return spaceGuid;
    }

    private void logUnauthorizedRequest(HttpServletRequest request, ResponseStatusException e) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("User \"%s\" is not authorized for request to \"%s\".", SecurityContextUtil.getUserName(),
                                       ServletUtils.decodeUri(request)),
                         e);
        }
    }

    protected abstract String extractSpaceGuid(HttpServletRequest request);

}
