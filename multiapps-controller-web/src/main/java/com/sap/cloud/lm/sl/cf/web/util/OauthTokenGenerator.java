package com.sap.cloud.lm.sl.cf.web.util;

import java.text.MessageFormat;
import java.time.Instant;
import java.util.Optional;

import org.cloudfoundry.client.lib.oauth2.OAuth2AccessTokenWithAdditionalInfo;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.sap.cloud.lm.sl.cf.core.Constants;
import com.sap.cloud.lm.sl.cf.core.dao.AccessTokenDao;
import com.sap.cloud.lm.sl.cf.core.security.token.TokenParserChain;
import com.sap.cloud.lm.sl.cf.web.message.Messages;

public class OauthTokenGenerator extends TokenGenerator {

    private final TokenParserChain tokenParserChain;
    private final TokenReuser tokenReuser;

    public OauthTokenGenerator(AccessTokenDao accessTokenDao, TokenParserChain tokenParserChain, TokenReuser tokenReuser) {
        super(accessTokenDao);
        this.tokenParserChain = tokenParserChain;
        this.tokenReuser = tokenReuser;
    }

    @Override
    public OAuth2AccessTokenWithAdditionalInfo generate(String tokenString) {
        OAuth2AccessTokenWithAdditionalInfo oAuth2AccessTokenWithAdditionalInfo = tokenParserChain.parse(tokenString);
        validateTokenExpiration(oAuth2AccessTokenWithAdditionalInfo);
        Optional<OAuth2AccessTokenWithAdditionalInfo> existingAccessToken = tokenReuser.getValidTokenWithExpirationAfterIfPresent(oAuth2AccessTokenWithAdditionalInfo.getUserName(),
                                                                                                                                  Constants.OAUTH_TOKEN_RETENTION_TIME_IN_SECONDS);
        if (existingAccessToken.isPresent()) {
            return existingAccessToken.get();
        }
        storeAccessToken(buildAccessToken(oAuth2AccessTokenWithAdditionalInfo));
        return oAuth2AccessTokenWithAdditionalInfo;
    }

    private void validateTokenExpiration(OAuth2AccessTokenWithAdditionalInfo oAuth2AccessTokenWithAdditionalInfo) {
        if (oAuth2AccessTokenWithAdditionalInfo.expiresBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                                              MessageFormat.format(Messages.THE_TOKEN_HAS_EXPIRED_ON_0,
                                                                   oAuth2AccessTokenWithAdditionalInfo.getExpiresAt()));
        }
    }
}
