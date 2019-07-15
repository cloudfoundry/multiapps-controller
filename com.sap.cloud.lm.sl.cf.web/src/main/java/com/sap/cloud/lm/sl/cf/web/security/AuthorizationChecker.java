package com.sap.cloud.lm.sl.cf.web.security;

import java.text.MessageFormat;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.util.TokenFactory;
import com.sap.cloud.lm.sl.cf.core.auditlogging.AuditLoggingProvider;
import com.sap.cloud.lm.sl.cf.core.cf.CloudControllerClientProvider;
import com.sap.cloud.lm.sl.cf.core.helpers.ClientHelper;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.CachedMap;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.core.util.UserInfo;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.ResponseRenderer;

@Component
public class AuthorizationChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationChecker.class);
    private CachedMap<UUID, List<UUID>> spaceDevelopersCache = null;

    private final CloudControllerClientProvider clientProvider;
    private final ApplicationConfiguration applicationConfiguration;

    @Inject
    public AuthorizationChecker(CloudControllerClientProvider clientProvider, ApplicationConfiguration applicationConfiguration) {
        this.clientProvider = clientProvider;
        this.applicationConfiguration = applicationConfiguration;
        initSpaceDevelopersCache();
    }

    private synchronized void initSpaceDevelopersCache() {
        if (spaceDevelopersCache != null) {
            return;
        }
        Integer cacheExpirationInSeconds = applicationConfiguration.getSpaceDeveloperCacheExpirationInSeconds();
        spaceDevelopersCache = new CachedMap<>(cacheExpirationInSeconds);
    }

    public void ensureUserIsAuthorized(HttpServletRequest request, UserInfo userInfo, String organization, String space, String action) {
        try {
            if (!checkPermissions(userInfo, organization, space, request.getMethod()
                .equals(HttpMethod.GET))) {
                String message = MessageFormat.format(Messages.UNAUTHORISED_OPERATION_ORG_SPACE, action, organization, space);
                failWithForbiddenStatus(message);
            }
        } catch (SLException e) {
            String message = MessageFormat.format(Messages.PERMISSION_CHECK_FAILED_ORG_SPACE, action, organization, space);
            failWithUnauthorizedStatus(message);
        }
    }

    public void ensureUserIsAuthorized(HttpServletRequest request, UserInfo userInfo, String spaceGuid, String action) {
        try {
            if (!checkPermissions(userInfo, spaceGuid, request.getMethod()
                .equals(HttpMethod.GET))) {
                String message = MessageFormat.format(Messages.UNAUTHORISED_OPERATION_SPACE_ID, action, spaceGuid);
                failWithForbiddenStatus(message);
            }
        } catch (SLException e) {
            String message = MessageFormat.format(Messages.PERMISSION_CHECK_FAILED_SPACE_ID, action, spaceGuid);
            failWithUnauthorizedStatus(message);
        }
    }

    boolean checkPermissions(UserInfo userInfo, String orgName, String spaceName, boolean readOnly) {
        if (applicationConfiguration.areDummyTokensEnabled() && isDummyToken(userInfo)) {
            return true;
        }
        if (hasAdminScope(userInfo)) {
            return true;
        }
        // TODO a lot of cpu time is lost in the getControllerCloient method
        CloudControllerClient client = clientProvider.getControllerClient(userInfo.getName());
        UUID userGuid = UUID.fromString(userInfo.getId());
        // TODO and some more cpu time in hasPermissions
        return hasPermissions(client, userGuid, orgName, spaceName, readOnly) && hasAccess(client, orgName, spaceName);
    }

    boolean checkPermissions(UserInfo userInfo, String spaceId, boolean readOnly) {
        if (applicationConfiguration.areDummyTokensEnabled() && isDummyToken(userInfo)) {
            return true;
        }
        if (hasAdminScope(userInfo)) {
            return true;
        }

        UUID spaceGuid = UUID.fromString(spaceId);
        CloudControllerClient client = clientProvider.getControllerClient(userInfo.getName());
        UUID userGuid = UUID.fromString(userInfo.getId());
        return hasPermissions(client, userGuid, spaceGuid, readOnly);
    }

    private boolean hasPermissions(CloudControllerClient client, UUID userGuid, UUID spaceGuid, boolean readOnly) {
        if (isUserInSpaceDevelopersUsingCache(client, userGuid, spaceGuid)) {
            return true;
        }
        if (isUserInSpaceDevelopersAfterCacheRefresh(client, userGuid, spaceGuid)) {
            return true;
        }

        if (readOnly) {
            return isUserInSpaceAuditors(client, userGuid, spaceGuid) || isUserInSpaceManagers(client, userGuid, spaceGuid);
        }
        return false;
    }

    private boolean isUserInSpaceAuditors(CloudControllerClient client, UUID userGuid, UUID spaceGuid) {
        return client.getSpaceAuditors(spaceGuid)
            .contains(userGuid);
    }

    private boolean isUserInSpaceManagers(CloudControllerClient client, UUID userGuid, UUID spaceGuid) {
        return client.getSpaceManagers(spaceGuid)
            .contains(userGuid);
    }

    private boolean isUserInSpaceDevelopersUsingCache(CloudControllerClient client, UUID userGuid, UUID spaceGuid) {
        return spaceDevelopersCache.get(spaceGuid, (() -> client.getSpaceDevelopers(spaceGuid)))
            .contains(userGuid);
    }

    private boolean isUserInSpaceDevelopersAfterCacheRefresh(CloudControllerClient client, UUID userGuid, UUID spaceGuid) {
        return spaceDevelopersCache.forceRefresh(spaceGuid, (() -> client.getSpaceDevelopers(spaceGuid)))
            .contains(userGuid);
    }

    private boolean hasPermissions(CloudControllerClient client, UUID userGuid, String orgName, String spaceName, boolean readOnly) {
        if (client.getSpaceDevelopers(orgName, spaceName)
            .contains(userGuid)) {
            return true;
        }
        if (readOnly) {
            if (client.getSpaceAuditors(orgName, spaceName)
                .contains(userGuid)) {
                return true;
            }
            return client.getSpaceManagers(orgName, spaceName)
                .contains(userGuid);
        }
        return false;
    }

    private boolean hasAccess(CloudControllerClient client, String orgName, String spaceName) {
        return client.getSpace(orgName, spaceName, false) != null;
    }

    private boolean isDummyToken(UserInfo userInfo) {
        return userInfo.getToken()
            .getValue()
            .equals(TokenFactory.DUMMY_TOKEN);
    }

    private boolean hasAdminScope(UserInfo userInfo) {
        return userInfo.getToken()
            .getScope()
            .contains(TokenFactory.SCOPE_CC_ADMIN);
    }

    private void failWithUnauthorizedStatus(String message) {
        failWithStatus(Status.UNAUTHORIZED, message);
    }

    private void failWithForbiddenStatus(String message) {
        failWithStatus(Status.FORBIDDEN, message);
    }

    private static void failWithStatus(Status status, String message) {
        LOGGER.warn(message);
        AuditLoggingProvider.getFacade()
            .logSecurityIncident(message);
        throw new WebApplicationException(ResponseRenderer.renderResponseForStatus(status, message));
    }

    public ClientHelper getClientHelper(CloudControllerClient client) {
        return new ClientHelper(client);
    }
}
