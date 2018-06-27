package com.sap.cloud.lm.sl.cf.core.cf;

import java.net.MalformedURLException;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.collections.map.ReferenceMap;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.ClientExtensions;
import com.sap.cloud.lm.sl.cf.client.TokenProvider;
import com.sap.cloud.lm.sl.cf.core.cf.service.TokenService;
import com.sap.cloud.lm.sl.cf.core.helpers.PortAllocator;
import com.sap.cloud.lm.sl.cf.core.helpers.PortAllocatorFactory;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.Pair;

@Component
public class CloudFoundryClientProvider {

    @Autowired
    private ClientFactory cloudFoundryClientFactory;

    @Autowired(required = false)
    private PortAllocatorFactory portAllocatorFactory;

    @Autowired
    private TokenService tokenService;

    // Cached clients. These are stored in memory-sensitive cache, i.e. no OutOfMemory error would
    // occur before GC tries to release the not-used clients.
    @SuppressWarnings("unchecked")
    private Map<String, Pair<CloudFoundryOperations, TokenProvider>> clients = Collections
        .synchronizedMap(new ReferenceMap(ReferenceMap.HARD, ReferenceMap.SOFT));

    public CloudFoundryOperations getCloudFoundryClient(String userName, String org, String space, String processId) throws SLException {
        Pair<CloudFoundryOperations, TokenProvider> client = retrieveClientForToken(userName, org, space, processId);
        return client._1;
    }

    public CloudFoundryOperations getCloudFoundryClient(String userName, String spaceGuid, String processId) throws SLException {
        Pair<CloudFoundryOperations, TokenProvider> client = retrieveClientForToken(userName, spaceGuid, processId);
        return client._1;
    }

    public CloudFoundryOperations getCloudFoundryClient(String userName, String spaceGuid) throws SLException {
        Pair<CloudFoundryOperations, TokenProvider> client = retrieveClientForToken(userName, spaceGuid);
        return client._1;
    }

    public CloudFoundryOperations getCloudFoundryClient(String userName) throws SLException {
        Pair<CloudFoundryOperations, TokenProvider> client = createClientForToken(userName);
        return client._1;
    }

    public PortAllocator getPortAllocator(ClientExtensions client, String domain) {
        return portAllocatorFactory.createPortAllocator(client, domain);
    }

    public void releaseClient(String userName, String org, String space) throws SLException {
        releaseClientFromCache(userName, org, space);
    }
    
    public void releaseClient(String userName, String spaceGuid) throws SLException {
        releaseClientFromCache(userName, spaceGuid);
    }

    public OAuth2AccessToken getValidToken(String userName) throws SLException {
        return getValidToken(userName, null, null);
    }

    private OAuth2AccessToken getValidToken(String userName, String org, String space) throws SLException {
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

    private Pair<CloudFoundryOperations, TokenProvider> retrieveClientForToken(String userName, String org, String space, String processId)
        throws SLException {
        try {
            return getClientFromCache(userName, org, space, processId);
        } catch (CloudFoundryException e) {
            throw new SLException(e, Messages.CANT_CREATE_CLIENT_2, org, space);
        }
    }

    private Pair<CloudFoundryOperations, TokenProvider> retrieveClientForToken(String userName, String spaceGuid, String processId)
        throws SLException {
        try {
            return getClientFromCache(userName, spaceGuid, processId);
        } catch (CloudFoundryException e) {
            throw new SLException(e, Messages.CANT_CREATE_CLIENT_FOR_SPACE_ID, spaceGuid);
        }
    }

    private Pair<CloudFoundryOperations, TokenProvider> retrieveClientForToken(String userName, String spaceGuid) throws SLException {
        try {
            return getClientFromCache(userName, spaceGuid);
        } catch (CloudFoundryException e) {
            throw new SLException(e, Messages.CANT_CREATE_CLIENT_FOR_SPACE_ID, spaceGuid);
        }
    }

    private Pair<CloudFoundryOperations, TokenProvider> createClientForToken(String userName) throws SLException {
        try {
            return cloudFoundryClientFactory.createClient(getValidToken(userName));
        } catch (CloudFoundryException e) {
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
    public Pair<CloudFoundryOperations, TokenProvider> getClientFromCache(String userName, String org, String space) {
        return getClientFromCache(userName, org, space, null);
    }

    public Pair<CloudFoundryOperations, TokenProvider> getClientFromCache(String userName, String org, String space, String processId) {
        // Get a client from the cache or create a new one if needed
        String key = getKey(userName, org, space);
        Pair<CloudFoundryOperations, TokenProvider> client = clients.get(key);
        if (client == null) {
            client = cloudFoundryClientFactory.createClient(getValidToken(userName), org, space);
            if (processId != null) {
                clients.put(key, client);
            }
        }
        return client;
    }

    public Pair<CloudFoundryOperations, TokenProvider> getClientFromCache(String userName, String spaceId) {
        // Get a client from the cache or create a new one if needed
        String key = getKey(userName, spaceId);
        Pair<CloudFoundryOperations, TokenProvider> client = clients.get(key);
        if (client == null) {
            client = cloudFoundryClientFactory.createClient(getValidToken(userName), spaceId);
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
        Pair<CloudFoundryOperations, TokenProvider> client = clients.remove(key);
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
