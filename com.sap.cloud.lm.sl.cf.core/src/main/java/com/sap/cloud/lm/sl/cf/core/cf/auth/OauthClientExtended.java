package com.sap.cloud.lm.sl.cf.core.cf.auth;

import java.net.URL;
import java.text.MessageFormat;

import org.cloudfoundry.client.lib.oauth2.OauthClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.web.client.RestTemplate;

import com.sap.cloud.lm.sl.cf.client.util.TokenProperties;
import com.sap.cloud.lm.sl.cf.core.cf.service.TokenService;

public class OauthClientExtended extends OauthClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(OauthClientExtended.class);
    private TokenService tokenService;

    public OauthClientExtended(URL authorizationUrl, RestTemplate restTemplate, TokenService tokenService) {
        super(authorizationUrl, restTemplate);
        this.tokenService = tokenService;
    }

    @Override
    public OAuth2AccessToken getToken() {
        if (token == null) {
            return null;
        }

        // If the current token will expire in the next 2 minutes, then get a new token from the token store
        if (token.getExpiresIn() < 120) {
            TokenProperties tokenProperties = TokenProperties.fromToken(token);
            token = tokenService.getToken(tokenProperties.getUserName());
            LOGGER.info(MessageFormat.format(com.sap.cloud.lm.sl.cf.core.message.Messages.RETRIEVED_USER_TOKEN,
                                             tokenProperties.getUserName(), token.getExpiresIn()));
        }

        return token;
    }
}
