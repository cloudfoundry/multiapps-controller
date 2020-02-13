package com.sap.cloud.lm.sl.cf.web.security;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.client.lib.CloudOperationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.TokenStore;

import com.sap.cloud.lm.sl.cf.client.TokenProvider;
import com.sap.cloud.lm.sl.cf.client.util.TokenFactory;
import com.sap.cloud.lm.sl.cf.core.auditlogging.AuditLoggingProvider;
import com.sap.cloud.lm.sl.cf.core.cf.TokenProviderFactory;
import com.sap.cloud.lm.sl.cf.core.security.token.parsers.TokenParserChain;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.core.util.SecurityUtil;
import com.sap.cloud.lm.sl.cf.web.Messages;

@Named("customAuthenticationProvider")
public class CustomAuthenticationProvider implements AuthenticationProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomAuthenticationProvider.class);

    @Inject
    @Qualifier("tokenStore")
    TokenStore tokenStore;

    @Inject
    @Qualifier("tokenProviderFactory")
    TokenProviderFactory cloudFoundryTokenProviderFactory;

    @Inject
    ApplicationConfiguration configuration;

    @Inject
    TokenFactory tokenFactory;

    @Inject
    TokenParserChain tokenParserChain;

    @Override
    public Authentication authenticate(Authentication authentication) {
        if (!configuration.isBasicAuthEnabled())
            throw new InsufficientAuthenticationException("Basic authentication is not enabled, use OAuth2");

        try {
            UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) authentication;
            String userName = (String) auth.getPrincipal();
            String password = (String) auth.getCredentials();

            TokenProvider tokenProvider = cloudFoundryTokenProviderFactory.createTokenProvider(userName, password);

            // Get a valid token from the client
            // If this works, consider the request authenticated
            OAuth2AccessToken token = (tokenProvider != null) ? tokenProvider.getToken() : null;

            if (token == null) {
                if (configuration.areDummyTokensEnabled()) {
                    token = tokenFactory.createDummyToken(userName, SecurityUtil.CLIENT_ID);
                } else {
                    String message = "Null access token returned by cloud controller";
                    AuditLoggingProvider.getFacade()
                                        .logSecurityIncident(message);
                    throw new AuthenticationServiceException(message);
                }
            }

            // Check if an authentication for this token already exists in the token store
            OAuth2Authentication auth2 = tokenStore.readAuthentication(token);
            if (auth2 == null) {
                token = tokenParserChain.parse(token.getValue());

                // Create an authentication for the token and store it in the token store
                auth2 = SecurityUtil.createAuthentication(SecurityUtil.CLIENT_ID, token.getScope(), SecurityUtil.getTokenUserInfo(token));
                storeAccessToken(token, auth2);
            }

            return auth2;
        } catch (CloudOperationException e) {
            String message = Messages.CANNOT_AUTHENTICATE_WITH_CLOUD_CONTROLLER;
            AuditLoggingProvider.getFacade()
                                .logSecurityIncident(message);
            throw new BadCredentialsException(message, e);
        }
    }

    private void storeAccessToken(OAuth2AccessToken token, OAuth2Authentication auth2) {
        try {
            tokenStore.storeAccessToken(token, auth2);
        } catch (DataIntegrityViolationException e) {
            LOGGER.debug(Messages.ERROR_STORING_TOKEN_DUE_TO_INTEGRITY_VIOLATION, e);
            // Ignoring the exception as the token and authentication are already persisted by another client.
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}
