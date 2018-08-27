package com.sap.cloud.lm.sl.cf.core.cf;

import java.net.MalformedURLException;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.collections.map.ReferenceMap;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.XsCloudControllerClient;
import com.sap.cloud.lm.sl.cf.client.TokenProvider;
import com.sap.cloud.lm.sl.cf.core.cf.service.TokenService;
import com.sap.cloud.lm.sl.cf.core.helpers.PortAllocator;
import com.sap.cloud.lm.sl.cf.core.helpers.PortAllocatorFactory;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.Pair;

@Component
public class CloudControllerClientProvider {

    @Autowired
    private ClientFactory clientFactory;

    @Autowired(required = false)
    private PortAllocatorFactory portAllocatorFactory;

    @Autowired
    private TokenService tokenService;

    // Cached clients. These are stored in memory-sensitive cache, i.e. no OutOfMemory error would
    // occur before GC tries to release the not-used clients.
    @SuppressWarnings("unchecked")
    private Map<String, Pair<CloudControllerClient, TokenProvider>> clients = Collections
        .synchronizedMap(new ReferenceMap(ReferenceMap.HARD, ReferenceMap.SOFT));

    public CloudControllerClient getControllerClient(String userName, String org, String space, String processId) {
        Pair<CloudControllerClient, TokenProvider> client = retrieveClientForToken(userName, org, space, processId);
        return client._1;
    }

    public CloudControllerClient getControllerClient(String userName, String spaceGuid, String processId) {
        Pair<CloudControllerClient, TokenProvider> client = retrieveClientForToken(userName, spaceGuid, processId);
        return client._1;
    }

    public CloudControllerClient getControllerClient(String userName, String spaceGuid) {
        Pair<CloudControllerClient, TokenProvider> client = retrieveClientForToken(userName, spaceGuid);
        return client._1;
    }

    public CloudControllerClient getControllerClient(String userName) {
        Pair<CloudControllerClient, TokenProvider> client = createClientForToken(userName);
        return client._1;
    }

    public PortAllocator getPortAllocator(XsCloudControllerClient client, String domain) {
        return portAllocatorFactory.createPortAllocator(client, domain);
    }

    public void releaseClient(String userName, String org, String space) {
        releaseClientFromCache(userName, org, space);
    }

    public void releaseClient(String userName, String spaceGuid) {
        releaseClientFromCache(userName, spaceGuid);
    }

    public OAuth2AccessToken getValidToken(String userName) {
        return getValidToken(userName, null, null);
    }

    private OAuth2AccessToken getValidToken(String userName, String org, String space) {
        OAuth2AccessToken token = tokenService.getToken(userName);
        if (token == null) {
            throw new SLException(Messages.NO_VALID_TOKEN_FOUND, userName);
        }

        if (token.isExpired() && token.getRefreshToken() == null) {
            tokenService.removeToken(token);
            if (org != null && space != null) {
                releaseClientFromCache(userName, org, space);
            }
            throw new SLException(Messages.TOKEN_EXPIRED, userName);
        }

        return token;
    }

    private Pair<CloudControllerClient, TokenProvider> retrieveClientForToken(String userName, String org, String space, String processId) {
        try {
            return getClientFromCache(userName, org, space, processId);
        } catch (CloudOperationException e) {
            throw new SLException(e, Messages.CANT_CREATE_CLIENT_2, org, space);
        }
    }

    private Pair<CloudControllerClient, TokenProvider> retrieveClientForToken(String userName, String spaceGuid, String processId) {
        try {
            return getClientFromCache(userName, spaceGuid, processId);
        } catch (CloudOperationException e) {
            throw new SLException(e, Messages.CANT_CREATE_CLIENT_FOR_SPACE_ID, spaceGuid);
        }
    }

    private Pair<CloudControllerClient, TokenProvider> retrieveClientForToken(String userName, String spaceGuid) {
        try {
            return getClientFromCache(userName, spaceGuid);
        } catch (CloudOperationException e) {
            throw new SLException(e, Messages.CANT_CREATE_CLIENT_FOR_SPACE_ID, spaceGuid);
        }
    }

    private Pair<CloudControllerClient, TokenProvider> createClientForToken(String userName) {
        try {
            return clientFactory.createClient(getValidToken(userName));
        } catch (CloudOperationException e) {
            throw new SLException(e, Messages.CANT_CREATE_CLIENT);
        }
    }

    /**
     * Returns a client for the specified access token, organization, and space by either getting it from the clients cache or creating a
     * new one.
     * 
     * @param token the access token to be used when getting the client
     * @param org the organization associated with the client
     * @param space the space associated with the client
     * @return a CF client for the specified access token, organization, and space
     * @throws MalformedURLException if the configured target URL is malformed
     */
    public Pair<CloudControllerClient, TokenProvider> getClientFromCache(String userName, String org, String space) {
        return getClientFromCache(userName, org, space, null);
    }

    public Pair<CloudControllerClient, TokenProvider> getClientFromCache(String userName, String org, String space, String processId) {
        // Get a client from the cache or create a new one if needed
        String key = getKey(userName, org, space);
        Pair<CloudControllerClient, TokenProvider> client = clients.get(key);
        if (client == null) {
            client = clientFactory.createClient(getValidToken(userName), org, space);
            if (processId != null) {
                clients.put(key, client);
            }
        }
        return client;
    }

    public Pair<CloudControllerClient, TokenProvider> getClientFromCache(String userName, String spaceId) {
        // Get a client from the cache or create a new one if needed
        String key = getKey(userName, spaceId);
        Pair<CloudControllerClient, TokenProvider> client = clients.get(key);
        if (client == null) {
            client = clientFactory.createClient(getValidToken(userName), spaceId);
            clients.put(key, client);
        }
        return client;
    }

    /**
     * Updates the client cache for the specified old access token, organization, and space by associating the existing client with the new
     * access token.
     * 
     * @param oldToken the old access token to be used when getting the client
     * @param newToken the new access token to associate the client with
     * @param org the organization associated with the client
     * @param space the space associated with the client
     */
    public void updateClientInCache(String userName, String org, String space) {
        String key = getKey(userName, org, space);
        Pair<CloudControllerClient, TokenProvider> client = clients.remove(key);
        if (client != null) {
            String key2 = getKey(userName, org, space);
            clients.put(key2, client);
        }
    }

    /**
     * Releases the client for the specified token, organization, and space by removing it from the clients cache.
     * 
     * @param token the access token to be used when releasing the client
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
            .append("|")
            .append(org)
            .append("|")
            .append(space);
        return sb.toString();
    }

    private String getKey(String userName, String spaceId) {
        StringBuilder sb = new StringBuilder();
        sb.append(userName)
            .append("|")
            .append(spaceId);
        return sb.toString();
    }
}
