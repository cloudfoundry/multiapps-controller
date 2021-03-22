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
        String decodedToken = decodeToken(tokenString);
        String[] usernameAndPassword = decodedToken.split(":");
        if (usernameAndPassword.length != 2) {
            throw new InternalAuthenticationServiceException(Messages.INVALID_AUTHENTICATION_PROVIDED);
        }
        String username = usernameAndPassword[0];
        String password = usernameAndPassword[1];
        OAuthClient oauthClient = restUtil.createOAuthClientByControllerUrl(applicationConfiguration.getControllerUrl(),
                                                                            applicationConfiguration.shouldSkipSslValidation());
        oauthClient.init(new CloudCredentials(username, password));
        return oauthClient.getToken();
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
