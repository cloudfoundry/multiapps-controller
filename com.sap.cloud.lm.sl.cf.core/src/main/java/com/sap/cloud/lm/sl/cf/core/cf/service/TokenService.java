package com.sap.cloud.lm.sl.cf.core.cf.service;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.security.token.store.SingleUserTokenStore;

/**
 * Provides functionality for persisting, updating and removing tokens from a token store
 */
@Component
@Profile("cf")
public class TokenService {

    @Autowired
    protected SingleUserTokenStore tokenStore;

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
     * Removes specific token from the tokenStore
     * 
     * @param token the token to be removed from the database
     */
    public void removeToken(OAuth2AccessToken token) {
        tokenStore.removeAccessToken(token);
    }
}
