package com.sap.cloud.lm.sl.cf.core.cf.service;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.store.JdbcTokenStore;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.util.TokenUtil;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.util.SecurityUtil;
import com.sap.cloud.lm.sl.cf.core.util.UserInfo;

/**
 * Provides functionality for persisting, updating and removing tokens from a token store
 */
@Component
public class TokenService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenService.class);

    @Autowired
    private JdbcTokenStore tokenStore;

    /**
     * Chooses a token among all tokens for this user in the token store.
     * 
     * @param tokenStore the token store to search in
     * @param userName the username
     * @return the chosen token, or null if no token was found
     */
    public OAuth2AccessToken getToken(String userName) {
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

    /**
     * Update an existing token in the token store and remove the old one
     * 
     * @param userInfo the userInfo used for creating an authentication
     * @param oldToken the old token which is persisted in the database
     * @param newToken the new token which will be inserted into the database
     */
    public void updateToken(UserInfo userInfo, OAuth2AccessToken oldToken, OAuth2AccessToken newToken) {
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
        removeToken(oldToken);
    }

    /**
     * Removes specific token from the tokenStore
     * 
     * @param token the token to be removed from the database
     */
    public void removeToken(OAuth2AccessToken token) {
        tokenStore.removeAccessToken(token);
    }
}
