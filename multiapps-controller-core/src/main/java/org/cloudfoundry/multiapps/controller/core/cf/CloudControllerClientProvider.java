package org.cloudfoundry.multiapps.controller.core.cf;

import java.time.Instant;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.security.token.TokenService;
import org.springframework.util.ConcurrentReferenceHashMap;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.oauth2.OAuth2AccessTokenWithAdditionalInfo;

@Named
public class CloudControllerClientProvider {

    private static final String NO_CORRELATION_ID = "NO-ID";

    @Inject
    private CloudControllerClientFactory clientFactory;

    @Inject
    private TokenService tokenService;

    // Cached clients. These are stored in memory-sensitive cache, i.e. no OutOfMemory error would
    // occur before GC tries to release the not-used clients.
    private final Map<String, CloudControllerClient> clients = new ConcurrentReferenceHashMap<>();

    /**
     * Returns a client for the specified user name, organization, space and process id by either getting it from the clients cache or
     * creating a new one.
     *
     * @param userName the user name associated with the client
     * @param org the organization associated with the client
     * @param space the space associated with the client
     * @param correlationId of the process which is used to tag HTTP requests
     * @return a CF client for the specified access token, organization, and space
     */
    public CloudControllerClient getControllerClient(String userName, String org, String space, String correlationId) {
        try {
            return getClientFromCache(userName, org, space, correlationId);
        } catch (CloudOperationException e) {
            throw new SLException(e, Messages.CANT_CREATE_CLIENT_2, org, space);
        }
    }

    /**
     * Returns a client for the specified user name and space id by either getting it from the clients cache or creating a new one.
     *
     * @param userName the user name associated with the client
     * @param spaceGuid the space guid associated with the client
     * @param correlationId of the process which is used to tag HTTP requests
     * @return a CF client for the specified access token, organization, and space
     */
    public CloudControllerClient getControllerClient(String userName, String spaceGuid, String correlationId) {
        try {
            return getClientFromCache(userName, spaceGuid, correlationId);
        } catch (CloudOperationException e) {
            throw new SLException(e, Messages.CANT_CREATE_CLIENT_FOR_SPACE_ID, spaceGuid);
        }
    }

    /**
     * Returns a client for the specified user name and space id by either getting it from the clients cache or creating a new one.
     *
     * @param userName the user name associated with the client
     * @param spaceGuid the space guid associated with the client
     * @return a CF client for the specified access token, organization, and space
     */
    public CloudControllerClient getControllerClientWithNoCorrelation(String userName, String spaceGuid) {
        try {
            return getClientFromCacheWithNoCorrelation(userName, spaceGuid);
        } catch (CloudOperationException e) {
            throw new SLException(e, Messages.CANT_CREATE_CLIENT_FOR_SPACE_ID, spaceGuid);
        }
    }

    /**
     * Returns a client for the specified user name, organization, space and process id by either getting it from the clients cache or
     * creating a new one.
     *
     * @param userName the user name associated with the client
     * @param org the organization associated with the client
     * @param space the space associated with the client
     * @return a CF client for the specified access token, organization, and space
     */
    public CloudControllerClient getControllerClientWithNoCorrelation(String userName, String org, String space) {
        try {
            return getClientFromCacheWithNoCorrelation(userName, org, space);
        } catch (CloudOperationException e) {
            throw new SLException(e, Messages.CANT_CREATE_CLIENT_2, org, space);
        }
    }

    /**
     * Returns a client for the specified user name by creating a new one.
     *
     * @param userName the user name associated with the client
     * @return a CF client for the specified access token
     */
    public CloudControllerClient getControllerClient(String userName) {
        try {
            return clientFactory.createClient(getValidToken(userName));
        } catch (CloudOperationException e) {
            throw new SLException(e, Messages.CANT_CREATE_CLIENT);
        }
    }

    /**
     * Releases the client for the specified user name, organization and space by removing it from the clients cache.
     *
     * @param userName the user name associated with the client
     * @param org the organization associated with the client
     * @param space the space associated with the client
     * @param correlationId of the process which is used to tag HTTP requests
     */
    public void releaseClient(String userName, String org, String space, String correlationId) {
        clients.remove(getKey(userName, org, space, correlationId));
    }

    /**
     * Releases the client for the specified user name and space id by removing it from the clients cache.
     *
     * @param userName the user name associated with the client
     * @param spaceGuid the space id associated with the client
     * @param correlationId of the process which is used to tag HTTP requests
     */
    public void releaseClient(String userName, String spaceGuid, String correlationId) {
        clients.remove(getKey(userName, spaceGuid, correlationId));
    }

    /**
     * Releases the client for the specified user name and space id by removing it from the clients cache.
     *
     * @param userName the user name associated with the client
     * @param spaceGuid the space id associated with the client
     */
    public void releaseClientWithNoCorrelation(String userName, String spaceGuid) {
        clients.remove(getKey(userName, spaceGuid, NO_CORRELATION_ID));
    }

    /**
     * Releases the client for the specified user name, organization and space by removing it from the clients cache.
     *
     * @param userName the user name associated with the client
     * @param org the organization associated with the client
     * @param space the space associated with the client
     */
    public void releaseClientWithNoCorrelation(String userName, String org, String space) {
        clients.remove(getKey(userName, org, space, NO_CORRELATION_ID));
    }

    private OAuth2AccessTokenWithAdditionalInfo getValidToken(String userName) {
        OAuth2AccessTokenWithAdditionalInfo token = tokenService.getToken(userName);
        if (token.getOAuth2AccessToken()
                 .getExpiresAt()
                 .isBefore(Instant.now())) {
            throw new SLException(Messages.TOKEN_EXPIRED, userName);
        }
        return token;
    }

    private CloudControllerClient getClientFromCacheWithNoCorrelation(String userName, String org, String space) {
        String key = getKey(userName, org, space, NO_CORRELATION_ID);
        return clients.computeIfAbsent(key,
                                       k -> clientFactory.createClient(getValidToken(userName), org, space, null));
    }

    private CloudControllerClient getClientFromCache(String userName, String org, String space, String correlationId) {
        String key = getKey(userName, org, space, correlationId);
        return clients.computeIfAbsent(key,
                                       k -> clientFactory.createClient(getValidToken(userName), org, space, correlationId));
    }

    private CloudControllerClient getClientFromCacheWithNoCorrelation(String userName, String spaceId) {
        String key = getKey(userName, spaceId, NO_CORRELATION_ID);
        return clients.computeIfAbsent(key,
                                       k -> clientFactory.createClient(getValidToken(userName), spaceId, null));
    }

    private CloudControllerClient getClientFromCache(String userName, String spaceId, String correlationId) {
        String key = getKey(userName, spaceId, correlationId);
        return clients.computeIfAbsent(key,
                                       k -> clientFactory.createClient(getValidToken(userName), spaceId, correlationId));
    }

    private String getKey(String... args) {
        return String.join("|", args);
    }
}
