package com.sap.cloud.lm.sl.cf.web.resources;

import java.text.MessageFormat;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.core.auditlogging.AuditLoggingProvider;
import com.sap.cloud.lm.sl.cf.core.cf.CloudFoundryClientProvider;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.util.AuthorizationUtil;
import com.sap.cloud.lm.sl.cf.core.util.UserInfo;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.ResponseRenderer;

public class AuthorizationChecker {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationChecker.class);

    protected static void ensureUserIsAuthorized(HttpServletRequest request, CloudFoundryClientProvider clientProvider, UserInfo userInfo,
        String organization, String space, String action) {
        try {
            if (!AuthorizationUtil.checkPermissions(clientProvider, userInfo, organization, space,
                request.getMethod().equals(HttpMethod.GET), null)) {
                String message = MessageFormat.format(Messages.UNAUTHORISED_OPERATION_ORG_SPACE, action, organization, space);
                failWithForbiddenStatus(message);
            }
        } catch (SLException e) {
            String message = MessageFormat.format(Messages.PERMISSION_CHECK_FAILED_ORG_SPACE, action, organization, space);
            failWithUnauthorizedStatus(message);
        }
    }

    protected static void ensureUserIsAuthorized(HttpServletRequest request, CloudFoundryClientProvider clientProvider, UserInfo userInfo,
        String spaceGuid, String action) {
        try {
            if (!AuthorizationUtil.checkPermissions(clientProvider, userInfo, spaceGuid, request.getMethod().equals(HttpMethod.GET))) {
                String message = MessageFormat.format(Messages.UNAUTHORISED_OPERATION_SPACE_ID, action, spaceGuid);
                failWithForbiddenStatus(message);
            }
        } catch (SLException e) {
            String message = MessageFormat.format(Messages.PERMISSION_CHECK_FAILED_SPACE_ID, action, spaceGuid);
            failWithUnauthorizedStatus(message);
        }
    }

    private static void failWithUnauthorizedStatus(String message) {
        failWithStatus(Status.UNAUTHORIZED, message);
    }

    private static void failWithForbiddenStatus(String message) {
        failWithStatus(Status.FORBIDDEN, message);
    }

    private static void failWithStatus(Status status, String message) {
        LOGGER.warn(message);
        AuditLoggingProvider.getFacade().logSecurityIncident(message);
        throw new WebApplicationException(ResponseRenderer.renderResponseForStatus(status, message));
    }

}
