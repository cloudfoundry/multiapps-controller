package org.cloudfoundry.multiapps.controller.web.util;

import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.Optional;

import org.cloudfoundry.multiapps.controller.core.security.token.parsers.TokenParserChain;
import org.cloudfoundry.multiapps.controller.persistence.model.AccessToken;
import org.cloudfoundry.multiapps.controller.persistence.services.AccessTokenService;
import org.cloudfoundry.multiapps.controller.web.Constants;
import org.cloudfoundry.multiapps.controller.web.Messages;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.sap.cloudfoundry.client.facade.oauth2.OAuth2AccessTokenWithAdditionalInfo;

public class OauthTokenGenerator extends TokenGenerator {

    private final TokenParserChain tokenParserChain;
    private final TokenReuser tokenReuser;

    public OauthTokenGenerator(AccessTokenService accessTokenService, TokenParserChain tokenParserChain, TokenReuser tokenReuser) {
        super(accessTokenService);
        this.tokenParserChain = tokenParserChain;
        this.tokenReuser = tokenReuser;
    }

    @Override
    public OAuth2AccessTokenWithAdditionalInfo generate(String tokenString) {
        OAuth2AccessTokenWithAdditionalInfo oAuth2AccessTokenWithAdditionalInfo = tokenParserChain.parse(tokenString);
        validateTokenExpiration(oAuth2AccessTokenWithAdditionalInfo);
        String username = extractUsername(oAuth2AccessTokenWithAdditionalInfo);
        Optional<AccessToken> accessToken = tokenReuser.getTokenWithExpirationAfterOrReuseCurrent(username,
                                                                                                  Constants.OAUTH_TOKEN_RETENTION_TIME_IN_SECONDS,
                                                                                                  oAuth2AccessTokenWithAdditionalInfo);
        if (accessToken.isPresent()) {
            return tokenParserChain.parse(new String(accessToken.get()
                                                                .getValue(),
                                                     StandardCharsets.UTF_8));
        }
        storeAccessToken(buildAccessToken(oAuth2AccessTokenWithAdditionalInfo));
        return oAuth2AccessTokenWithAdditionalInfo;
    }

    private void validateTokenExpiration(OAuth2AccessTokenWithAdditionalInfo oAuth2AccessTokenWithAdditionalInfo) {
        if (oAuth2AccessTokenWithAdditionalInfo.getOAuth2AccessToken()
                                               .getExpiresAt()
                                               .isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                                              MessageFormat.format(Messages.THE_TOKEN_HAS_EXPIRED_ON_0,
                                                                   oAuth2AccessTokenWithAdditionalInfo.getOAuth2AccessToken()
                                                                                                      .getExpiresAt()));
        }
    }
}
