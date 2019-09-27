package com.sap.cloud.lm.sl.cf.web.security;

import java.io.IOException;
import java.text.MessageFormat;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import com.sap.cloud.lm.sl.cf.web.message.Messages;
import com.sap.cloud.lm.sl.cf.web.util.SecurityContextUtil;

public abstract class SpaceGuidBasedAuthorizationFilter implements UriAuthorizationFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpaceGuidBasedAuthorizationFilter.class);

    private final AuthorizationChecker authorizationChecker;

    public SpaceGuidBasedAuthorizationFilter(AuthorizationChecker authorizationChecker) {
        this.authorizationChecker = authorizationChecker;
    }

    @Override
    public final void ensureUserIsAuthorized(HttpCommunication communication) throws IOException {
        String spaceGuid = extractAndLogSpaceGuid(communication.getRequest());
        try {
            authorizationChecker.ensureUserIsAuthorized(communication.getRequest(), SecurityContextUtil.getUserInfo(), spaceGuid, null);
        } catch (WebApplicationException e) {
            logUnauthorizedRequest(communication.getRequest(), e);
            communication.getResponse()
                         .sendError(HttpStatus.UNAUTHORIZED.value(),
                                    MessageFormat.format(Messages.NOT_AUTHORIZED_TO_OPERATE_IN_SPACE_WITH_GUID_0, spaceGuid));
        }
    }

    private String extractAndLogSpaceGuid(HttpServletRequest request) {
        String spaceGuid = extractSpaceGuid(request);
        LOGGER.trace("Extracted space GUID \"{}\" from request to \"{}\".", spaceGuid, request.getRequestURI());
        return spaceGuid;
    }

    private void logUnauthorizedRequest(HttpServletRequest request, WebApplicationException e) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("User \"%s\" is not authorized for requst to \"%s\".", SecurityContextUtil.getUserName(),
                                       request.getRequestURI()),
                         e);
        }
    }

    protected abstract String extractSpaceGuid(HttpServletRequest request);

}
