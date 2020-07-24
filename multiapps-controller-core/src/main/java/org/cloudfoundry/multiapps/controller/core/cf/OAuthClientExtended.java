package org.cloudfoundry.multiapps.controller.core.cf;

import java.net.URL;
import java.text.MessageFormat;

import org.cloudfoundry.client.lib.oauth2.OAuthClient;
import org.cloudfoundry.multiapps.controller.client.util.TokenProperties;
import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.security.token.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.web.client.RestTemplate;

public class OAuthClientExtended extends OAuthClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(OAuthClientExtended.class);
    private final TokenService tokenService;

    public OAuthClientExtended(URL authorizationUrl, RestTemplate restTemplate, TokenService tokenService) {
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
            LOGGER.info(MessageFormat.format(Messages.RETRIEVED_TOKEN_FOR_USER_0_WITH_EXPIRATION_TIME_1, tokenProperties.getUserName(),
                                             token.getExpiresIn()));
        }

        return token;
    }
}
