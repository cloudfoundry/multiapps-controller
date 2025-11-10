package org.cloudfoundry.multiapps.controller.core.cf;

import java.time.Duration;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.client.facade.CloudControllerClient;
import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.model.CachedMap;
import org.cloudfoundry.multiapps.controller.core.security.token.TokenService;
import org.springframework.beans.factory.DisposableBean;

@Named
public class CloudControllerClientProvider implements DisposableBean {

    @Inject
    private CloudControllerClientFactory clientFactory;
    @Inject
    private TokenService tokenService;

    private final CachedMap<String, CloudControllerClient> clients = new CachedMap<>(Duration.ofMinutes(30));

    /**
     * Returns a client for the specified user guid and space id by either getting it from the clients cache or creating a new one.
     *
     * @param userGuid the userGuid associated with the client
     * @param spaceGuid the space guid associated with the client
     * @param correlationId of the process which is used to tag HTTP requests
     * @return a CF client for the specified access token, organization, and space
     */
    public CloudControllerClient getControllerClient(String userGuid, String spaceGuid, String correlationId) {
        try {
            return getClientFromCache(userGuid, spaceGuid, correlationId);
        } catch (CloudOperationException e) {
            throw new SLException(e, Messages.CANT_CREATE_CLIENT_FOR_SPACE_ID, spaceGuid);
        }
    }

    /**
     * Returns a client for the specified user and space id by either getting it from the clients cache or creating a new one.
     *
     * @param userGuid the userGuid associated with the client
     * @param spaceGuid the space guid associated with the client
     * @return a CF client for the specified access token, organization, and space
     */
    public CloudControllerClient getControllerClientWithNoCorrelation(String userGuid, String spaceGuid) {
        try {
            return getClientFromCacheWithNoCorrelation(userGuid, spaceGuid);
        } catch (CloudOperationException e) {
            throw new SLException(e, Messages.CANT_CREATE_CLIENT_FOR_SPACE_ID, spaceGuid);
        }
    }

    /**
     * Releases the client for the specified user and space id by removing it from the clients cache.
     *
     * @param userGuid the userGuid associated with the client
     * @param spaceGuid the space id associated with the client
     */
    public void releaseClient(String userGuid, String spaceGuid) {
        clients.remove(getKey(spaceGuid, userGuid));
    }

    private CloudControllerClient getClientFromCacheWithNoCorrelation(String userGuid, String spaceId) {
        String key = getKey(spaceId, userGuid);
        return clients.computeIfAbsent(key, () -> clientFactory.createClient(tokenService.getToken(userGuid), spaceId, null));
    }

    private CloudControllerClient getClientFromCache(String userGuid, String spaceId, String correlationId) {
        String key = getKey(spaceId, userGuid);
        return clients.computeIfAbsent(key,
                                       () -> clientFactory.createClient(tokenService.getToken(userGuid), spaceId, correlationId));
    }

    private String getKey(String spaceGuid, String userGuid) {
        return spaceGuid + "|" + userGuid;

    }

    @Override
    public void destroy() {
        clients.clear();
    }
}
