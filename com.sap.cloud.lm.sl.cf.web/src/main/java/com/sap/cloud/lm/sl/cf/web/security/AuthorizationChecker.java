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

import com.sap.cloud.lm.sl.cf.client.CloudControllerClientSupportingCustomUserIds;
import com.sap.cloud.lm.sl.cf.client.util.ExecutionRetrier;
import com.sap.cloud.lm.sl.cf.client.util.TokenFactory;
import com.sap.cloud.lm.sl.cf.core.auditlogging.AuditLoggingProvider;
import com.sap.cloud.lm.sl.cf.core.cf.CloudControllerClientProvider;
import com.sap.cloud.lm.sl.cf.core.cf.PlatformType;
import com.sap.cloud.lm.sl.cf.core.cf.clients.SpaceGetter;
import com.sap.cloud.lm.sl.cf.core.helpers.ClientHelper;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.CachedMap;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.core.util.UserInfo;
import com.sap.cloud.lm.sl.common.NotFoundException;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.Pair;
import com.sap.cloud.lm.sl.common.util.ResponseRenderer;

@Component
public class AuthorizationChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationChecker.class);
    private CachedMap<UUID, List<String>> spaceDevelopersCache = null;

    private final ExecutionRetrier retrier = new ExecutionRetrier();
    private final CloudControllerClientProvider clientProvider;
    private final SpaceGetter spaceGetter;
    private final ApplicationConfiguration applicationConfiguration;

    @Inject
    public AuthorizationChecker(CloudControllerClientProvider clientProvider, SpaceGetter spaceGetter,
        ApplicationConfiguration applicationConfiguration) {
        this.clientProvider = clientProvider;
        this.spaceGetter = spaceGetter;
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
        CloudControllerClientSupportingCustomUserIds client = (CloudControllerClientSupportingCustomUserIds) clientProvider
            .getControllerClient(userInfo.getName());
        // TODO and some more cpu time in hasPermissions
        return hasPermissions(client, userInfo.getId(), orgName, spaceName, readOnly) && hasAccess(client, orgName, spaceName);
    }

    boolean checkPermissions(UserInfo userInfo, String spaceGuid, boolean readOnly) {
        if (applicationConfiguration.areDummyTokensEnabled() && isDummyToken(userInfo)) {
            return true;
        }
        if (hasAdminScope(userInfo)) {
            return true;
        }

        UUID spaceUUID = UUID.fromString(spaceGuid);
        CloudControllerClientSupportingCustomUserIds client = (CloudControllerClientSupportingCustomUserIds) clientProvider
            .getControllerClient(userInfo.getName());
        if (PlatformType.CF.equals(applicationConfiguration.getPlatformType())) {
            return hasPermissions(client, userInfo.getId(), spaceUUID, readOnly);
        }

        Pair<String, String> location = getClientHelper(client).computeOrgAndSpace(spaceGuid);
        if (location == null) {
            throw new NotFoundException(Messages.ORG_AND_SPACE_NOT_FOUND, spaceGuid);
        }
        return hasPermissions(client, userInfo.getId(), location._1, location._2, readOnly);
    }

    private boolean hasPermissions(CloudControllerClientSupportingCustomUserIds client, String userId, UUID spaceGuid, boolean readOnly) {
        if (isUserInSpaceDevelopersUsingCache(client, userId, spaceGuid)) {
            return true;
        }
        if (isUserInSpaceDevelopersAfterCacheRefresh(client, userId, spaceGuid)) {
            return true;
        }

        if (readOnly) {
            return isUserInSpaceAuditors(client, userId, spaceGuid) || isUserInSpaceManagers(client, userId, spaceGuid);
        }
        return false;
    }

    private boolean isUserInSpaceAuditors(CloudControllerClientSupportingCustomUserIds client, String userId, UUID spaceGuid) {
        return client.getSpaceAuditorIdsAsStrings(spaceGuid)
            .contains(userId);
    }

    private boolean isUserInSpaceManagers(CloudControllerClientSupportingCustomUserIds client, String userId, UUID spaceGuid) {
        return client.getSpaceManagerIdsAsStrings(spaceGuid)
            .contains(userId);
    }

    private boolean isUserInSpaceDevelopersUsingCache(CloudControllerClientSupportingCustomUserIds client, String userId, UUID spaceGuid) {
        return spaceDevelopersCache.get(spaceGuid, (() -> client.getSpaceDeveloperIdsAsStrings(spaceGuid)))
            .contains(userId);
    }

    private boolean isUserInSpaceDevelopersAfterCacheRefresh(CloudControllerClientSupportingCustomUserIds client, String userId,
        UUID spaceGuid) {
        return spaceDevelopersCache.forceRefresh(spaceGuid, (() -> client.getSpaceDeveloperIdsAsStrings(spaceGuid)))
            .contains(userId);
    }

    private boolean hasPermissions(CloudControllerClientSupportingCustomUserIds client, String userId, String orgName, String spaceName,
        boolean readOnly) {
        if (client.getSpaceDeveloperIdsAsStrings(orgName, spaceName)
            .contains(userId)) {
            return true;
        }
        if (readOnly) {
            if (client.getSpaceAuditorIdsAsStrings(orgName, spaceName)
                .contains(userId)) {
                return true;
            }
            if (client.getSpaceManagerIdsAsStrings(orgName, spaceName)
                .contains(userId)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAccess(CloudControllerClientSupportingCustomUserIds client, String orgName, String spaceName) {
        return retrier.executeWithRetry(() -> spaceGetter.findSpace(client, orgName, spaceName)) != null;
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
        return new ClientHelper(client, spaceGetter);
    }
}
