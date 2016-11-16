package com.sap.cloud.lm.sl.cf.core.cf;

import java.util.Collection;

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.store.JdbcTokenStore;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.TokenProvider;
import com.sap.cloud.lm.sl.cf.client.util.TokenUtil;
import com.sap.cloud.lm.sl.cf.core.helpers.PortAllocator;
import com.sap.cloud.lm.sl.cf.core.helpers.PortAllocatorFactory;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.util.SecurityUtil;
import com.sap.cloud.lm.sl.cf.core.util.UserInfo;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.Pair;

@Component
public class CloudFoundryClientProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudFoundryClientProvider.class);

    @Autowired
    private CloudFoundryClientFactory cloudFoundryClientFactory;

    @Autowired(required = false)
    private PortAllocatorFactory portAllocatorFactory;

    @Autowired
    private JdbcTokenStore tokenStore;

    public CloudFoundryOperations getCloudFoundryClient(String userName, String org, String space, String processId) throws SLException {
        return getCloudFoundryClient(getValidToken(userName, org, space), org, space, processId);
    }

    public CloudFoundryOperations getCloudFoundryClient(OAuth2AccessToken token, String org, String space, String processId)
        throws SLException {
        Pair<CloudFoundryOperations, TokenProvider> client = createClientForToken(token, org, space, processId);
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
        cloudFoundryClientFactory.releaseClient(getValidToken(userName, org, space), org, space);
    }

    public OAuth2AccessToken getValidToken(String userName) throws SLException {
        return getValidToken(userName, null, null);
    }

    private OAuth2AccessToken getValidToken(String userName, String org, String space) throws SLException {
        OAuth2AccessToken token = chooseTokenFromTokenStore(userName);
        if (token == null) {
            throw new SLException(Messages.NO_VALID_TOKEN_FOUND, userName);
        }

        if (token.isExpired() && token.getRefreshToken() == null) {
            removeTokenFromTokenStore(token);
            if (org != null && space != null) {
                cloudFoundryClientFactory.releaseClient(token, org, space);
            }
            throw new SLException(Messages.TOKEN_EXPIRED, userName);
        }

        return token;
    }

    private Pair<CloudFoundryOperations, TokenProvider> createClientForToken(OAuth2AccessToken token, String org, String space,
        String processId) throws SLException {
        try {
            return cloudFoundryClientFactory.getClient(token, org, space, processId);
        } catch (CloudFoundryException e) {
            throw new SLException(e, Messages.CANT_CREATE_CLIENT_2, org, space);
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
            updateTokenInTokenStore(SecurityUtil.getTokenUserInfo(newToken), token, newToken);
            if (org != null && space != null) {
                cloudFoundryClientFactory.updateClient(token, newToken, org, space);
            }
        }
    }

    /**
     * Chooses a token among all tokens for this user in the token store.
     * 
     * @param tokenStore the token store to search in
     * @param userName the username
     * @return the chosen token, or null if no token was found
     */
    private OAuth2AccessToken chooseTokenFromTokenStore(String userName) {
        OAuth2AccessToken token = null;
        Collection<OAuth2AccessToken> tokens = tokenStore.findTokensByUserName(userName);
        for (OAuth2AccessToken tokenx : tokens) {
            // If a token is already found, overwrite it if the new token:
            // 1) has a refresh token, and the current token hasn't, or
            // 2) expires later than the current token
            if (token == null || ((tokenx.getRefreshToken() != null) && (token.getRefreshToken() == null))
                || (tokenx.getExpiresIn() > token.getExpiresIn())) {
                token = tokenx;
            }
        }
        return token;
    }

    private void updateTokenInTokenStore(UserInfo userInfo, OAuth2AccessToken oldToken, OAuth2AccessToken newToken) {
        // Create an authentication for the new token and add it to the token store
        OAuth2Authentication auth = SecurityUtil.createAuthentication(TokenUtil.getTokenClientId(newToken), newToken.getScope(), userInfo);
        try {
            tokenStore.storeAccessToken(newToken, auth);
        } catch (DataIntegrityViolationException e) {
            LOGGER.debug(Messages.ERROR_STORING_TOKEN_DUE_TO_INTEGRITY_VIOLATION, e);
            // Ignoring the exception as the token and authentication are already persisted
            // by another client.
        }

        // Remove the old token from the token store
        tokenStore.removeAccessToken(oldToken);
    }

    private void removeTokenFromTokenStore(OAuth2AccessToken token) {
        tokenStore.removeAccessToken(token);
    }
}
