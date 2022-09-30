package com.sap.cloud.lm.sl.cf.core.cf.service;

import java.text.MessageFormat;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.token.store.JdbcTokenStore;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.message.Messages;

/**
 * Provides functionality for persisting, updating and removing tokens from a token store
 */
@Component
@Profile("cf")
public class TokenService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenService.class);

    @Autowired
    protected JdbcTokenStore tokenStore;

    /**
     * Chooses a token among all tokens for this user in the token store.
     * 
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
        logTokenInformation(userName, token);
        return token;
    }

    private void logTokenInformation(String userName, OAuth2AccessToken token) {
        if (token != null) {
            LOGGER.debug(MessageFormat.format(Messages.ACCESS_TOKEN_RETRIEVED, userName, token.getExpiration()));
        }
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
