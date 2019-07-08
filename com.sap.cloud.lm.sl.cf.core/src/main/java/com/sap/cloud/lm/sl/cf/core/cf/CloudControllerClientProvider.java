package com.sap.cloud.lm.sl.cf.core.cf;

import java.util.Collections;
import java.util.Map;

import org.apache.commons.collections4.map.AbstractReferenceMap.ReferenceStrength;
import org.apache.commons.collections4.map.ReferenceMap;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.cf.service.TokenService;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;

@Component
public class CloudControllerClientProvider {

    @Autowired
    private ClientFactory clientFactory;

    @Autowired
    private TokenService tokenService;

    // Cached clients. These are stored in memory-sensitive cache, i.e. no OutOfMemory error would
    // occur before GC tries to release the not-used clients.
    private Map<String, CloudControllerClient> clients = Collections
        .synchronizedMap(new ReferenceMap<>(ReferenceStrength.HARD, ReferenceStrength.SOFT));

    public CloudControllerClient getControllerClient(String userName, String org, String space, String processId) {
        try {
            return getClientFromCache(userName, org, space, processId);
        } catch (CloudOperationException e) {
            throw new SLException(e, Messages.CANT_CREATE_CLIENT_2, org, space);
        }
    }

    public CloudControllerClient getControllerClient(String userName, String spaceGuid, String processId) {
        try {
            return getClientFromCache(userName, spaceGuid, processId);
        } catch (CloudOperationException e) {
            throw new SLException(e, Messages.CANT_CREATE_CLIENT_FOR_SPACE_ID, spaceGuid);
        }
    }

    public CloudControllerClient getControllerClient(String userName, String spaceGuid) {
        try {
            return getClientFromCache(userName, spaceGuid);
        } catch (CloudOperationException e) {
            throw new SLException(e, Messages.CANT_CREATE_CLIENT_FOR_SPACE_ID, spaceGuid);
        }
    }

    public CloudControllerClient getControllerClient(String userName) {
        try {
            return clientFactory.createClient(getValidToken(userName));
        } catch (CloudOperationException e) {
            throw new SLException(e, Messages.CANT_CREATE_CLIENT);
        }
    }

    public void releaseClient(String userName, String org, String space) {
        releaseClientFromCache(userName, org, space);
    }

    public void releaseClient(String userName, String spaceGuid) {
        releaseClientFromCache(userName, spaceGuid);
    }

    private OAuth2AccessToken getValidToken(String userName) {
        OAuth2AccessToken token = tokenService.getToken(userName);
        if (token == null) {
            throw new SLException(Messages.NO_VALID_TOKEN_FOUND, userName);
        }

        if (token.isExpired() && token.getRefreshToken() == null) {
            tokenService.removeToken(token);
            throw new SLException(Messages.TOKEN_EXPIRED, userName);
        }

        return token;
    }

    /**
     * Returns a client for the specified access token, organization, and space by either getting it from the clients cache or creating a
     * new one.
     * 
     * @param userName the user name associated with the client
     * @param org the organization associated with the client
     * @param space the space associated with the client
     * @return a CF client for the specified access token, organization, and space
     */
    public CloudControllerClient getClientFromCache(String userName, String org, String space) {
        return getClientFromCache(userName, org, space, null);
    }

    public CloudControllerClient getClientFromCache(String userName, String org, String space, String processId) {
        // Get a client from the cache or create a new one if needed
        String key = getKey(userName, org, space);
        CloudControllerClient client = clients.get(key);
        if (client == null) {
            client = clientFactory.createClient(getValidToken(userName), org, space);
            if (processId != null) {
                clients.put(key, client);
            }
        }
        return client;
    }

    public CloudControllerClient getClientFromCache(String userName, String spaceId) {
        // Get a client from the cache or create a new one if needed
        String key = getKey(userName, spaceId);
        return clients.computeIfAbsent(key, k -> clientFactory.createClient(getValidToken(userName), spaceId));
    }

    /**
     * Releases the client for the specified user name, organization, and space by removing it from the clients cache.
     * 
     * @param userName the user name associated with the client
     * @param org the organization associated with the client
     * @param space the space associated with the client
     */
    public void releaseClientFromCache(String userName, String org, String space) {
        clients.remove(getKey(userName, org, space));
    }

    public void releaseClientFromCache(String userName, String spaceGuid) {
        clients.remove(getKey(userName, spaceGuid));
    }

    private String getKey(String userName, String org, String space) {
        StringBuilder sb = new StringBuilder();
        sb.append(userName)
            .append('|')
            .append(org)
            .append('|')
            .append(space);
        return sb.toString();
    }

    private String getKey(String userName, String spaceId) {
        StringBuilder sb = new StringBuilder();
        sb.append(userName)
            .append('|')
            .append(spaceId);
        return sb.toString();
    }
}
