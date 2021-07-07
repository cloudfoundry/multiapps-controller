package org.cloudfoundry.multiapps.controller.web.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import org.cloudfoundry.multiapps.controller.core.security.token.parsers.TokenParserChain;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.AccessToken;
import org.cloudfoundry.multiapps.controller.persistence.services.AccessTokenService;
import org.cloudfoundry.multiapps.controller.web.Constants;
import org.cloudfoundry.multiapps.controller.web.Messages;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;

import com.sap.cloudfoundry.client.facade.CloudCredentials;
import com.sap.cloudfoundry.client.facade.oauth2.OAuth2AccessTokenWithAdditionalInfo;
import com.sap.cloudfoundry.client.facade.oauth2.OAuthClient;
import com.sap.cloudfoundry.client.facade.util.RestUtil;

public class BasicTokenGenerator extends TokenGenerator {

    private final RestUtil restUtil = createRestUtil();
    private final ApplicationConfiguration applicationConfiguration;
    private final TokenReuser tokenReuser;
    private final TokenParserChain tokenParserChain;

    public BasicTokenGenerator(AccessTokenService accessTokenService, ApplicationConfiguration applicationConfiguration,
                               TokenReuser tokenReuser, TokenParserChain tokenParserChain) {
        super(accessTokenService);
        this.applicationConfiguration = applicationConfiguration;
        this.tokenReuser = tokenReuser;
        this.tokenParserChain = tokenParserChain;
    }

    @Override
    public OAuth2AccessTokenWithAdditionalInfo generate(String tokenString) {
        if (!applicationConfiguration.isBasicAuthEnabled()) {
            throw new InsufficientAuthenticationException(Messages.BASIC_AUTHENTICATION_IS_NOT_ENABLED_USE_OAUTH_2);
        }
        OAuthClient oauthClient = restUtil.createOAuthClientByControllerUrl(applicationConfiguration.getControllerUrl(),
                                                                            applicationConfiguration.shouldSkipSslValidation());
        String[] usernameWithPassword = getUsernameWithPassword(tokenString);
        Optional<AccessToken> accessToken = tokenReuser.getTokenWithExpirationAfter(usernameWithPassword[0],
                                                                                    Constants.BASIC_TOKEN_RETENTION_TIME_IN_SECONDS);
        if (accessToken.isPresent()) {
            return tokenParserChain.parse(new String(accessToken.get()
                                                                .getValue(),
                                                     StandardCharsets.UTF_8));
        }
        oauthClient.init(new CloudCredentials(usernameWithPassword[0], usernameWithPassword[1]));
        OAuth2AccessTokenWithAdditionalInfo oAuth2AccessTokenWithAdditionalInfo = oauthClient.getToken();
        storeAccessToken(buildAccessToken(oAuth2AccessTokenWithAdditionalInfo));
        return oAuth2AccessTokenWithAdditionalInfo;
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

    protected RestUtil createRestUtil() {
        return new RestUtil();
    }
}
