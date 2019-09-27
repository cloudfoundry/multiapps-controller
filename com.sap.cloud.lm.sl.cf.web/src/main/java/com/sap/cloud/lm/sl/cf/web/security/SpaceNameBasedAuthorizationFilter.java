package com.sap.cloud.lm.sl.cf.web.security;

import java.io.IOException;
import java.text.MessageFormat;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.web.message.Messages;
import com.sap.cloud.lm.sl.cf.web.util.SecurityContextUtil;

public abstract class SpaceNameBasedAuthorizationFilter implements UriAuthorizationFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpaceNameBasedAuthorizationFilter.class);

    private final AuthorizationChecker authorizationChecker;

    public SpaceNameBasedAuthorizationFilter(AuthorizationChecker authorizationChecker) {
        this.authorizationChecker = authorizationChecker;
    }

    @Override
    public final void ensureUserIsAuthorized(HttpCommunication communication) throws IOException {
        CloudTarget target = extractAndLogTarget(communication.getRequest());
        try {
            authorizationChecker.ensureUserIsAuthorized(communication.getRequest(), SecurityContextUtil.getUserInfo(), target, null);
        } catch (WebApplicationException e) {
            logUnauthorizedRequest(communication.getRequest(), e);
            communication.getResponse()
                         .sendError(HttpStatus.UNAUTHORIZED.value(),
                                    MessageFormat.format(Messages.NOT_AUTHORIZED_TO_OPERATE_IN_ORGANIZATION_0_AND_SPACE_1,
                                                         target.getOrganizationName(), target.getSpaceName()));
        }
    }

    private CloudTarget extractAndLogTarget(HttpServletRequest request) {
        CloudTarget target = extractTarget(request);
        LOGGER.trace("Extracted target from request to \"{}\": {}", request.getRequestURI(), target);
        return target;
    }

    private void logUnauthorizedRequest(HttpServletRequest request, WebApplicationException e) {
        if (LOGGER.isDebugEnabled()) {
            String userName = SecurityContextUtil.getUserName();
            LOGGER.debug(String.format("User \"%s\" is not authorized for requst to \"%s\".", userName, request.getRequestURI()), e);
        }
    }

    protected abstract CloudTarget extractTarget(HttpServletRequest request);

}
