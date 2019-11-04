package com.sap.cloud.lm.sl.cf.web.security;

import java.io.IOException;
import java.text.MessageFormat;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.web.message.Messages;
import com.sap.cloud.lm.sl.cf.web.util.SecurityContextUtil;
import com.sap.cloud.lm.sl.cf.web.util.ServletUtils;

public abstract class SpaceNameBasedAuthorizationFilter implements UriAuthorizationFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpaceNameBasedAuthorizationFilter.class);

    private final AuthorizationChecker authorizationChecker;

    public SpaceNameBasedAuthorizationFilter(AuthorizationChecker authorizationChecker) {
        this.authorizationChecker = authorizationChecker;
    }

    @Override
    public final boolean ensureUserIsAuthorized(HttpServletRequest request, HttpServletResponse response) throws IOException {
        CloudTarget target = extractAndLogTarget(request);
        try {
            authorizationChecker.ensureUserIsAuthorized(request, SecurityContextUtil.getUserInfo(), target, null);
            return true;
        } catch (ResponseStatusException e) {
            logUnauthorizedRequest(request, e);
            response.sendError(HttpStatus.UNAUTHORIZED.value(),
                               MessageFormat.format(Messages.NOT_AUTHORIZED_TO_OPERATE_IN_ORGANIZATION_0_AND_SPACE_1,
                                                    target.getOrganizationName(), target.getSpaceName()));
            return false;
        }
    }

    private CloudTarget extractAndLogTarget(HttpServletRequest request) {
        CloudTarget target = extractTarget(request);
        LOGGER.trace("Extracted target from request to \"{}\": {}", ServletUtils.getDecodedURI(request), target);
        return target;
    }

    private void logUnauthorizedRequest(HttpServletRequest request, ResponseStatusException e) {
        if (LOGGER.isDebugEnabled()) {
            String userName = SecurityContextUtil.getUserName();
            LOGGER.debug(String.format("User \"%s\" is not authorized for request to \"%s\".", userName,
                                       ServletUtils.getDecodedURI(request)),
                         e);
        }
    }

    protected abstract CloudTarget extractTarget(HttpServletRequest request);

}
