package org.cloudfoundry.multiapps.controller.web.util;

import static com.sap.cloudfoundry.client.facade.oauth2.TokenFactory.EXPIRES_AT_KEY;

import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.cloudfoundry.multiapps.controller.client.util.TokenProperties;
import org.cloudfoundry.multiapps.controller.persistence.model.AccessToken;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableAccessToken;
import org.cloudfoundry.multiapps.controller.persistence.services.AccessTokenService;
import org.cloudfoundry.multiapps.controller.web.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloudfoundry.client.facade.oauth2.OAuth2AccessTokenWithAdditionalInfo;

public abstract class TokenGenerator {

    private final AccessTokenService accessTokenService;

    protected TokenGenerator(AccessTokenService accessTokenService) {
        this.accessTokenService = accessTokenService;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenGenerator.class);

    public void storeAccessToken(AccessToken accessToken) {
        LOGGER.info(MessageFormat.format(Messages.STORING_TOKEN_FOR_USER_0_WHICH_EXPIRES_AT_1, accessToken.getUsername(),
                                         accessToken.getExpiresAt()));
        accessTokenService.add(accessToken);
    }

    public abstract OAuth2AccessTokenWithAdditionalInfo generate(String tokenString);

    protected AccessToken buildAccessToken(OAuth2AccessTokenWithAdditionalInfo oAuth2AccessTokenWithAdditionalInfo) {
        return ImmutableAccessToken.builder()
                                   .value(oAuth2AccessTokenWithAdditionalInfo.getOAuth2AccessToken()
                                                                             .getTokenValue()
                                                                             .getBytes(StandardCharsets.UTF_8))
                                   .username(extractUsername(oAuth2AccessTokenWithAdditionalInfo))
                                   .expiresAt(calculateAccessTokenExpirationDate(oAuth2AccessTokenWithAdditionalInfo))
                                   .build();
    }

    protected String extractUsername(OAuth2AccessTokenWithAdditionalInfo token) {
        return (String) token.getAdditionalInfo()
                             .get(TokenProperties.USER_NAME_KEY);
    }

    private LocalDateTime calculateAccessTokenExpirationDate(OAuth2AccessTokenWithAdditionalInfo oAuth2AccessTokenWithAdditionalInfo) {
        long expirationInSeconds = ((Number) oAuth2AccessTokenWithAdditionalInfo.getAdditionalInfo()
                                                                                .get(EXPIRES_AT_KEY)).longValue();
        return Instant.ofEpochSecond(expirationInSeconds)
                      .atZone(ZoneId.systemDefault())
                      .toLocalDateTime();
    }
}
