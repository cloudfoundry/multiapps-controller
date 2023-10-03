package org.cloudfoundry.multiapps.controller.core.cf;

import java.time.Duration;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.model.CachedMap;
import org.cloudfoundry.multiapps.controller.core.security.token.TokenService;
import org.springframework.beans.factory.DisposableBean;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.CloudOperationException;

@Named
public class CloudControllerClientProvider implements DisposableBean {

    @Inject
    private CloudControllerClientFactory clientFactory;
    @Inject
    private TokenService tokenService;

    private final CachedMap<String, CloudControllerClient> clients = new CachedMap<>(Duration.ofMinutes(30));

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
     * Releases the client for the specified user name and space id by removing it from the clients cache.
     *
     * @param userName the user name associated with the client
     * @param spaceGuid the space id associated with the client
     */
    public void releaseClient(String userName, String spaceGuid) {
        clients.remove(getKey(spaceGuid, userName));
    }

    private CloudControllerClient getClientFromCacheWithNoCorrelation(String userName, String spaceId) {
        String key = getKey(spaceId, userName);
        return clients.computeIfAbsent(key,
                                       () -> clientFactory.createClient(tokenService.getToken(userName), spaceId, null));
    }

    private CloudControllerClient getClientFromCache(String userName, String spaceId, String correlationId) {
        String key = getKey(spaceId, userName);
        return clients.computeIfAbsent(key,
                                       () -> clientFactory.createClient(tokenService.getToken(userName), spaceId, correlationId));
    }

    private String getKey(String spaceGuid, String username) {
        return spaceGuid + "|" + username;
    }

    @Override
    public void destroy() {
        clients.clear();
    }
}
