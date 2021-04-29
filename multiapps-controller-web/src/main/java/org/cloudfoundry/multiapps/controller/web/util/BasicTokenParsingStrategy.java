package org.cloudfoundry.multiapps.controller.web.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.web.Messages;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;

import com.sap.cloudfoundry.client.facade.CloudCredentials;
import com.sap.cloudfoundry.client.facade.oauth2.OAuth2AccessTokenWithAdditionalInfo;
import com.sap.cloudfoundry.client.facade.oauth2.OAuthClient;
import com.sap.cloudfoundry.client.facade.util.RestUtil;

public class BasicTokenParsingStrategy implements TokenParsingStrategy {

    private final RestUtil restUtil;
    private final ApplicationConfiguration applicationConfiguration;

    public BasicTokenParsingStrategy(ApplicationConfiguration applicationConfiguration, RestUtil restUtil) {
        this.applicationConfiguration = applicationConfiguration;
        this.restUtil = restUtil;
    }

    @Override
    public OAuth2AccessTokenWithAdditionalInfo parseToken(String tokenString) {
        if (!applicationConfiguration.isBasicAuthEnabled()) {
            throw new InsufficientAuthenticationException(Messages.BASIC_AUTHENTICATION_IS_NOT_ENABLED_USE_OAUTH_2);
        }
        OAuthClient oauthClient = restUtil.createOAuthClientByControllerUrl(applicationConfiguration.getControllerUrl(),
                                                                            applicationConfiguration.shouldSkipSslValidation());
        String[] usernameWithPassword = getUsernameWithPassword(tokenString);
        oauthClient.init(new CloudCredentials(usernameWithPassword[0], usernameWithPassword[1]));
        return oauthClient.getToken();
    }

    String[] getUsernameWithPassword(String tokenString) {
        String decodedToken = decodeToken(tokenString);
        int colonIndex = decodedToken.indexOf(":");
        if (colonIndex == -1) {
            throw new InternalAuthenticationServiceException(Messages.INVALID_AUTHENTICATION_PROVIDED);
        }
        String username = decodedToken.substring(0, colonIndex);
        String password = decodedToken.substring(colonIndex + 1);
        return new String[] { username, password };
    }

    private String decodeToken(String tokenString) {
        try {
            return new String(Base64.getDecoder()
                                    .decode(tokenString),
                              StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new InternalAuthenticationServiceException(e.getMessage(), e);
        }
    }
}
