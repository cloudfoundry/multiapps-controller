package com.sap.cloud.lm.sl.cf.core.cf.auth;

import java.net.URL;

import org.cloudfoundry.client.lib.oauth2.OauthClient;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.web.client.RestTemplate;

import com.sap.cloud.lm.sl.cf.core.cf.service.TokenService;

public class OauthClientExtended extends OauthClient {

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
            token = tokenService.getToken((String) token.getAdditionalInformation().get("user_name"));
        }

        return token;

    }

}
