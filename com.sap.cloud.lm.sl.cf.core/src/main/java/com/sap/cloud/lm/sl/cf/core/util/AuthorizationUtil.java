package com.sap.cloud.lm.sl.cf.core.util;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.cloudfoundry.client.lib.CloudFoundryOperations;

import com.sap.cloud.lm.sl.cf.client.CloudFoundryOperationsExtended;
import com.sap.cloud.lm.sl.cf.client.util.ExecutionRetrier;
import com.sap.cloud.lm.sl.cf.client.util.TokenFactory;
import com.sap.cloud.lm.sl.cf.core.cf.CloudFoundryClientProvider;
import com.sap.cloud.lm.sl.cf.core.cf.clients.SpaceGetter;
import com.sap.cloud.lm.sl.cf.core.cf.clients.SpaceGetterFactory;
import com.sap.cloud.lm.sl.cf.core.helpers.ClientHelper;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.common.NotFoundException;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.Pair;

public class AuthorizationUtil {

    private static final String SPACE_CACHE_SEPARATOR = "|";
    private static final ExecutionRetrier RETRIER = new ExecutionRetrier();

    public static String getSpaceId(CloudFoundryClientProvider clientProvider, UserInfo userInfo, String orgName, String spaceName,
        String processId) throws SLException {
        CloudFoundryOperations client = getCloudFoundryClient(clientProvider, userInfo, orgName, spaceName, processId);
        String spaceId = new ClientHelper(client).computeSpaceId(orgName, spaceName);
        if (spaceId == null) {
            throw new SLException(MessageFormat.format(Messages.COULD_NOT_COMPUTE_SPACE_ID, orgName, spaceName));
        }
        return spaceId;
    }

    private static Map<String, String> processSpaceCache = new HashMap<>();

    public static String getProcessSpaceId(String processId, CloudFoundryClientProvider clientProvider, UserInfo userInfo, String orgName,
        String spaceName) throws SLException {
        String spaceCacheKey = getSpaceCacheKey(orgName, spaceName, processId);
        String spaceId = processSpaceCache.get(spaceCacheKey);
        if (spaceId == null) {
            spaceId = getSpaceId(clientProvider, userInfo, orgName, spaceName, processId);
            if (processId != null) {
                processSpaceCache.put(spaceCacheKey, spaceId);
            }
        }
        return spaceId;
    }

    private static String getSpaceCacheKey(String orgName, String spaceName, String processId) {
        return new StringBuilder().append(orgName)
            .append(SPACE_CACHE_SEPARATOR)
            .append(spaceName)
            .append(SPACE_CACHE_SEPARATOR)
            .append(processId)
            .toString();
    }

    public static boolean checkPermissions(CloudFoundryClientProvider clientProvider, UserInfo userInfo, String orgName, String spaceName,
        boolean readOnly, String processId) throws SLException {
        if (Configuration.getInstance()
            .areDummyTokensEnabled() && isDummyToken(userInfo)) {
            return true;
        }
        if (hasAdminScope(userInfo)) {
            return true;
        }
        CloudFoundryOperations client = getCloudFoundryClient(clientProvider, userInfo);
        return checkPermissions(client, userInfo, orgName, spaceName, readOnly);
    }

    public static boolean checkPermissions(CloudFoundryClientProvider clientProvider, UserInfo userInfo, String spaceGuid, boolean readOnly)
        throws SLException {
        if (Configuration.getInstance()
            .areDummyTokensEnabled() && isDummyToken(userInfo)) {
            return true;
        }
        if (hasAdminScope(userInfo)) {
            return true;
        }
        CloudFoundryOperations client = getCloudFoundryClient(clientProvider, userInfo);
        Pair<String, String> location = new ClientHelper(client).computeOrgAndSpace(spaceGuid);
        if (location == null) {
            throw new NotFoundException(Messages.ORG_AND_SPACE_NOT_FOUND, spaceGuid);
        }
        return checkPermissions(client, userInfo, location._1, location._2, readOnly);
    }

    private static boolean checkPermissions(CloudFoundryOperations client, UserInfo userInfo, String orgName, String spaceName,
        boolean readOnly) {
        CloudFoundryOperationsExtended clientx = (CloudFoundryOperationsExtended) client;
        return hasPermissions(clientx, userInfo.getId(), orgName, spaceName, readOnly) && hasAccess(clientx, orgName, spaceName);
    }

    private static boolean hasPermissions(CloudFoundryOperationsExtended client, String userId, String orgName, String spaceName,
        boolean readOnly) {
        if (client.getSpaceDevelopers2(orgName, spaceName)
            .contains(userId)) {
            return true;
        }
        if (readOnly) {
            if (client.getSpaceAuditors2(orgName, spaceName)
                .contains(userId)) {
                return true;
            }
            if (client.getSpaceManagers2(orgName, spaceName)
                .contains(userId)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasAccess(CloudFoundryOperationsExtended client, String orgName, String spaceName) {
        SpaceGetter spaceGetter = new SpaceGetterFactory().createSpaceGetter();
        return RETRIER.executeWithRetry(() -> spaceGetter.findSpace(client, orgName, spaceName)) != null;
    }

    private static CloudFoundryOperations getCloudFoundryClient(CloudFoundryClientProvider clientProvider, UserInfo userInfo)
        throws SLException {
        return clientProvider.getCloudFoundryClient(userInfo.getToken());
    }

    private static CloudFoundryOperations getCloudFoundryClient(CloudFoundryClientProvider clientProvider, UserInfo userInfo,
        String orgName, String spaceName, String processId) throws SLException {
        return clientProvider.getCloudFoundryClient(userInfo.getToken());
    }

    private static boolean isDummyToken(UserInfo userInfo) {
        return userInfo.getToken()
            .getValue()
            .equals(TokenFactory.DUMMY_TOKEN);
    }

    private static boolean hasAdminScope(UserInfo userInfo) {
        return userInfo.getToken()
            .getScope()
            .contains(TokenFactory.SCOPE_CC_ADMIN);
    }
}
