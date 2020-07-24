package org.cloudfoundry.multiapps.controller.web.security;

import java.text.MessageFormat;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.client.util.TokenProperties;
import org.cloudfoundry.multiapps.controller.core.auditlogging.AuditLoggingProvider;
import org.cloudfoundry.multiapps.controller.core.security.token.parsers.TokenParserChain;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.core.util.SSLUtil;
import org.cloudfoundry.multiapps.controller.core.util.SecurityUtil;
import org.cloudfoundry.multiapps.controller.web.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;

@Named
public class CustomTokenServices implements ResourceServerTokenServices {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomTokenServices.class);

    private final TokenStore tokenStore;
    private final TokenParserChain tokenParserChain;

    @Inject
    public CustomTokenServices(TokenStore tokenStore, ApplicationConfiguration configuration, TokenParserChain tokenParserChain) {
        this.tokenStore = tokenStore;
        this.tokenParserChain = tokenParserChain;
        if (configuration.shouldSkipSslValidation()) {
            SSLUtil.disableSSLValidation();
        }
    }

    @Override
    public OAuth2Authentication loadAuthentication(String tokenString) {

        // Get an access token for the specified token string
        OAuth2AccessToken token = readAccessToken(tokenString);

        // Check if a valid access token has been obtained
        if (token == null) {
            logToAuditLogAndThrow("Invalid access token");
        }

        // Check if the token has expired and there is no refresh token
        if (token.isExpired() && token.getRefreshToken() == null) {
            tokenStore.removeAccessToken(token);
            logToAuditLogAndThrow(MessageFormat.format("The access token has expired on {0}", token.getExpiration()));
        }

        // Check if an authentication for this token already exists in the token store
        OAuth2Authentication auth = tokenStore.readAuthentication(token);
        if (auth == null) {
            // Create an authentication for the token and store it in the token store
            TokenProperties tokenProperties = TokenProperties.fromToken(token);
            auth = SecurityUtil.createAuthentication(tokenProperties.getClientId(), token.getScope(), SecurityUtil.getTokenUserInfo(token));
            try {
                LOGGER.info(MessageFormat.format(Messages.STORING_TOKEN_FOR_USER_0_WITH_EXPIRATION_TIME_1, tokenProperties.getUserName(),
                                                 token.getExpiresIn()));
                tokenStore.storeAccessToken(token, auth);
            } catch (DataIntegrityViolationException e) {
                LOGGER.debug(Messages.ERROR_STORING_TOKEN_DUE_TO_INTEGRITY_VIOLATION, e);
                // Ignoring the exception as the token and authentication are already persisted by another client.
            }
        }

        return auth;
    }

    @Override
    public OAuth2AccessToken readAccessToken(String tokenString) {
        // Check if an access token for the received token string already exists in the token store
        OAuth2AccessToken token = tokenStore.readAccessToken(tokenString);
        if (token != null) {
            LOGGER.debug("Stored token value: " + token.getValue());
            LOGGER.debug("Stored token type: " + token.getTokenType());
            LOGGER.debug("Stored token expires in: " + token.getExpiresIn());
        } else {
            token = tokenParserChain.parse(tokenString);
        }
        return token;
    }

    private void logToAuditLogAndThrow(String message) {
        AuditLoggingProvider.getFacade()
                            .logSecurityIncident(message);
        throw new InvalidTokenException(message);
    }

}
