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

import com.sap.cloud.lm.sl.cf.client.TokenProvider;
import com.sap.cloud.lm.sl.cf.core.cf.service.TokenService;
import com.sap.cloud.lm.sl.cf.core.helpers.PortAllocator;
import com.sap.cloud.lm.sl.cf.core.helpers.PortAllocatorFactory;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.util.SecurityUtil;
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
    private Map<String, Pair<CloudFoundryOperations, TokenProvider>> clients = Collections.synchronizedMap(
        new ReferenceMap(ReferenceMap.HARD, ReferenceMap.SOFT));

    public CloudFoundryOperations getCloudFoundryClient(String userName, String org, String space, String processId) throws SLException {
        return getCloudFoundryClient(getValidToken(userName, org, space), org, space, processId);
    }

    public CloudFoundryOperations getCloudFoundryClient(OAuth2AccessToken token, String org, String space, String processId)
        throws SLException {
        Pair<CloudFoundryOperations, TokenProvider> client = retrieveClientForToken(token, org, space, processId);
        updateTokenIfNecessary(client._2, token);
        return client._1;
    }

    public CloudFoundryOperations getCloudFoundryClient(OAuth2AccessToken token, String spaceGuid, String processId) throws SLException {
        Pair<CloudFoundryOperations, TokenProvider> client = retrieveClientForToken(token, spaceGuid, processId);
        updateTokenIfNecessary(client._2, token);
        return client._1;
    }

    public CloudFoundryOperations getCloudFoundryClient(OAuth2AccessToken token, String spaceGuid) throws SLException {
        Pair<CloudFoundryOperations, TokenProvider> client = retrieveClientForToken(token, spaceGuid);
        updateTokenIfNecessary(client._2, token);
        return client._1;
    }

    public CloudFoundryOperations getCloudFoundryClient(String userName) throws SLException {
        return getCloudFoundryClient(getValidToken(userName));
    }

    public CloudFoundryOperations getCloudFoundryClient(OAuth2AccessToken token) throws SLException {
        Pair<CloudFoundryOperations, TokenProvider> client = createClientForToken(token);
        updateTokenIfNecessary(client._2, token);
        return client._1;
    }

    public PortAllocator getPortAllocator(CloudFoundryOperations client, String domain) {
        return portAllocatorFactory.createPortAllocator(client, domain);
    }

    public void releaseClient(String userName, String org, String space) throws SLException {
        releaseClientFromCache(getValidToken(userName, org, space), org, space);
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
                releaseClientFromCache(token, org, space);
            }
            throw new SLException(Messages.TOKEN_EXPIRED, userName);
        }

        return token;
    }

    private Pair<CloudFoundryOperations, TokenProvider> retrieveClientForToken(OAuth2AccessToken token, String org, String space,
        String processId) throws SLException {
        try {
            return getClientFromCache(token, org, space, processId);
        } catch (CloudFoundryException e) {
            throw new SLException(e, Messages.CANT_CREATE_CLIENT_2, org, space);
        }
    }

    private Pair<CloudFoundryOperations, TokenProvider> retrieveClientForToken(OAuth2AccessToken token, String spaceGuid, String processId)
        throws SLException {
        try {
            return getClientFromCache(token, spaceGuid, processId);
        } catch (CloudFoundryException e) {
            throw new SLException(e, Messages.CANT_CREATE_CLIENT_2, spaceGuid, "asd");
        }
    }

    private Pair<CloudFoundryOperations, TokenProvider> retrieveClientForToken(OAuth2AccessToken token, String spaceGuid)
        throws SLException {
        try {
            return getClientFromCache(token, spaceGuid);
        } catch (CloudFoundryException e) {
            throw new SLException(e, Messages.CANT_CREATE_CLIENT_2, spaceGuid, "asd");
        }
    }

    private Pair<CloudFoundryOperations, TokenProvider> createClientForToken(OAuth2AccessToken token) throws SLException {
        try {
            return cloudFoundryClientFactory.createClient(token);
        } catch (CloudFoundryException e) {
            throw new SLException(e, Messages.CANT_CREATE_CLIENT);
        }
    }

    private void updateTokenIfNecessary(TokenProvider client, OAuth2AccessToken token) {
        updateTokenIfNecessary(client, token, null, null);
    }

    private void updateTokenIfNecessary(TokenProvider client, OAuth2AccessToken token, String org, String space) {
        OAuth2AccessToken newToken = (client != null) ? client.getToken() : null;

        if (newToken != null && !newToken.getValue().equals(token.getValue())) {
            tokenService.updateToken(SecurityUtil.getTokenUserInfo(newToken), token, newToken);
            if (org != null && space != null) {
                updateClientInCache(token, newToken, org, space);
            }
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
    public Pair<CloudFoundryOperations, TokenProvider> getClientFromCache(OAuth2AccessToken token, String org, String space) {
        return getClientFromCache(token, org, space, null);
    }

    public Pair<CloudFoundryOperations, TokenProvider> getClientFromCache(OAuth2AccessToken token, String org, String space,
        String processId) {
        // Get a client from the cache or create a new one if needed
        String key = getKey(token, org, space);
        Pair<CloudFoundryOperations, TokenProvider> client = clients.get(key);
        if (client == null) {
            client = cloudFoundryClientFactory.createClient(token, org, space);
            if (processId != null) {
                clients.put(key, client);
            }
        }
        return client;
    }

    public Pair<CloudFoundryOperations, TokenProvider> getClientFromCache(OAuth2AccessToken token, String spaceId) {
        // Get a client from the cache or create a new one if needed
        String key = getKey(token, spaceId);
        Pair<CloudFoundryOperations, TokenProvider> client = clients.get(key);
        if (client == null) {
            client = cloudFoundryClientFactory.createClient(token, spaceId);
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
    public void updateClientInCache(OAuth2AccessToken oldToken, OAuth2AccessToken newToken, String org, String space) {
        String key = getKey(oldToken, org, space);
        Pair<CloudFoundryOperations, TokenProvider> client = clients.remove(key);
        if (client != null) {
            String key2 = getKey(newToken, org, space);
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
    public void releaseClientFromCache(OAuth2AccessToken token, String org, String space) {
        clients.remove(getKey(token, org, space));
    }

    private String getKey(OAuth2AccessToken token, String org, String space) {
        StringBuilder sb = new StringBuilder();
        sb.append(token.getValue()).append("|").append(org).append("|").append(space);
        return sb.toString();
    }

    private String getKey(OAuth2AccessToken token, String spaceId) {
        StringBuilder sb = new StringBuilder();
        sb.append(token.getValue()).append("|").append(spaceId);
        return sb.toString();
    }
}
