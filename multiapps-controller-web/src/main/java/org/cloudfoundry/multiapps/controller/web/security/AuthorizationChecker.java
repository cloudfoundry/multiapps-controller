package org.cloudfoundry.multiapps.controller.web.security;

import java.text.MessageFormat;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.auditlogging.AuditLoggingProvider;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientFactory;
import org.cloudfoundry.multiapps.controller.core.cf.clients.CfRolesGetter;
import org.cloudfoundry.multiapps.controller.core.cf.clients.WebClientFactory;
import org.cloudfoundry.multiapps.controller.core.model.CachedMap;
import org.cloudfoundry.multiapps.controller.core.security.token.TokenService;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.core.util.UserInfo;
import org.cloudfoundry.multiapps.controller.persistence.model.CloudTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.sap.cloudfoundry.client.facade.CloudCredentials;
import com.sap.cloudfoundry.client.facade.domain.UserRole;
import com.sap.cloudfoundry.client.facade.oauth2.OAuth2AccessTokenWithAdditionalInfo;
import com.sap.cloudfoundry.client.facade.oauth2.TokenFactory;

@Named
public class AuthorizationChecker implements DisposableBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationChecker.class);
    private CachedMap<SpaceWithUser, Set<UserRole>> userRolesCache = null;

    private final CloudControllerClientFactory clientFactory;
    private final TokenService tokenService;
    private final WebClientFactory webClientFactory;

    @Inject
    public AuthorizationChecker(CloudControllerClientFactory clientFactory, TokenService tokenService,
                                ApplicationConfiguration applicationConfiguration, WebClientFactory webClientFactory) {
        this.clientFactory = clientFactory;
        this.tokenService = tokenService;
        this.webClientFactory = webClientFactory;
        initSpaceDevelopersCache(applicationConfiguration);
    }

    private void initSpaceDevelopersCache(ApplicationConfiguration applicationConfiguration) {
        if (userRolesCache != null) {
            return;
        }
        Integer cacheExpirationInSeconds = applicationConfiguration.getSpaceDeveloperCacheExpirationInSeconds();
        userRolesCache = new CachedMap<>(Duration.ofSeconds(cacheExpirationInSeconds));
    }

    public void ensureUserIsAuthorized(HttpServletRequest request, UserInfo userInfo, CloudTarget target, String action) {
        try {
            if (!checkPermissions(userInfo, target.getOrganizationName(), target.getSpaceName(), isGetRequest(request))) {
                String message = MessageFormat.format(Messages.UNAUTHORISED_OPERATION_ORG_SPACE, action, target.getOrganizationName(),
                                                      target.getSpaceName());
                failWithForbiddenStatus(message);
            }
        } catch (SLException e) {
            String message = MessageFormat.format(Messages.PERMISSION_CHECK_FAILED_ORG_SPACE, action, target.getOrganizationName(),
                                                  target.getSpaceName());
            failWithUnauthorizedStatus(message);
        }
    }

    public void ensureUserIsAuthorized(HttpServletRequest request, UserInfo userInfo, String spaceGuid, String action) {
        try {
            if (!checkPermissions(userInfo, spaceGuid, isGetRequest(request))) {
                String message = MessageFormat.format(Messages.UNAUTHORISED_OPERATION_SPACE_ID, action, spaceGuid);
                failWithForbiddenStatus(message);
            }
        } catch (SLException e) {
            String message = MessageFormat.format(Messages.PERMISSION_CHECK_FAILED_SPACE_ID, action, spaceGuid);
            failWithUnauthorizedStatus(message);
        }
    }

    private boolean isGetRequest(HttpServletRequest request) {
        return HttpMethod.GET.matches(request.getMethod());
    }

    boolean checkPermissions(UserInfo userInfo, String orgName, String spaceName, boolean readOnly) {
        if (hasAdminScope(userInfo)) {
            return true;
        }
        var userToken = tokenService.getToken(userInfo.getName());
        var spaceClient = clientFactory.createSpaceClient(userToken);
        var space = spaceClient.getSpace(orgName, spaceName);

        CfRolesGetter rolesGetter = getRolesGetter(userToken);
        UUID userGuid = UUID.fromString(userInfo.getId());
        return hasPermissions(rolesGetter, getSpaceWithUser(userGuid, space.getGuid()), readOnly);
    }

    protected CfRolesGetter getRolesGetter(OAuth2AccessTokenWithAdditionalInfo token) {
        return new CfRolesGetter(webClientFactory, new CloudCredentials(token));
    }

    boolean checkPermissions(UserInfo userInfo, String spaceId, boolean readOnly) {
        if (hasAdminScope(userInfo)) {
            return true;
        }
        var userToken = tokenService.getToken(userInfo.getName());
        CfRolesGetter rolesGetter = getRolesGetter(userToken);
        UUID userGuid = UUID.fromString(userInfo.getId());
        UUID spaceGuid = convertSpaceIdToUUID(spaceId);
        return hasPermissions(rolesGetter, getSpaceWithUser(userGuid, spaceGuid), readOnly);
    }

    private UUID convertSpaceIdToUUID(String spaceId) {
        UUID spaceGuid = null;
        try {
            spaceGuid = UUID.fromString(spaceId);
        } catch (IllegalArgumentException e) {
            failWithNotFoundStatus(e.getMessage());
        }
        return spaceGuid;
    }

    private boolean hasPermissions(CfRolesGetter rolesGetter, SpaceWithUser spaceWithUser, boolean readOnly) {
        if (isSpaceDeveloperUsingCache(spaceWithUser)) {
            return true;
        }
        Set<UserRole> userRoles = refreshUserRoles(rolesGetter, spaceWithUser);
        if (userRoles.contains(UserRole.SPACE_DEVELOPER)) {
            return true;
        }
        return readOnly && (userRoles.contains(UserRole.SPACE_AUDITOR) || userRoles.contains(UserRole.SPACE_MANAGER));
    }

    private SpaceWithUser getSpaceWithUser(UUID userGuid, UUID spaceGuid) {
        return new SpaceWithUser(userGuid, spaceGuid);
    }

    private boolean isSpaceDeveloperUsingCache(SpaceWithUser spaceWithUser) {
        Set<UserRole> userRoles = userRolesCache.get(spaceWithUser);
        return userRoles != null && userRoles.contains(UserRole.SPACE_DEVELOPER);
    }

    private Set<UserRole> refreshUserRoles(CfRolesGetter rolesGetter, SpaceWithUser spaceWithUser) {
        Set<UserRole> userRoles = rolesGetter.getRoles(spaceWithUser.getSpaceGuid(), spaceWithUser.getUserGuid());
        userRolesCache.put(spaceWithUser, userRoles);
        return userRoles;
    }

    private boolean hasAdminScope(UserInfo userInfo) {
        return userInfo.getToken()
                       .getOAuth2AccessToken()
                       .getScopes()
                       .contains(TokenFactory.SCOPE_CC_ADMIN);
    }

    private void failWithNotFoundStatus(String message) {
        failWithStatus(HttpStatus.NOT_FOUND, message);
    }

    private void failWithUnauthorizedStatus(String message) {
        failWithStatus(HttpStatus.UNAUTHORIZED, message);
    }

    private void failWithForbiddenStatus(String message) {
        failWithStatus(HttpStatus.FORBIDDEN, message);
    }

    private static void failWithStatus(HttpStatus status, String message) {
        LOGGER.warn(message);
        AuditLoggingProvider.getFacade()
                            .logSecurityIncident(message);
        throw new ResponseStatusException(status, message);
    }

    @Override
    public void destroy() {
        userRolesCache.clear();
    }
}
