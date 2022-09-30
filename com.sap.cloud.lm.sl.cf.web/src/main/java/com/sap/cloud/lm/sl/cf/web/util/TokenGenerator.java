package com.sap.cloud.lm.sl.cf.web.util;

import static com.sap.cloud.lm.sl.cf.client.util.TokenFactory.EXPIRES_AT_KEY;
import static org.cloudfoundry.client.constants.Constants.EXCHANGED_TOKEN;

import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.apache.commons.collections4.MapUtils;
import org.cloudfoundry.client.lib.oauth2.OAuth2AccessTokenWithAdditionalInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.client.util.TokenProperties;
import com.sap.cloud.lm.sl.cf.core.Constants;
import com.sap.cloud.lm.sl.cf.core.dao.AccessTokenDao;
import com.sap.cloud.lm.sl.cf.core.model.AccessToken;
import com.sap.cloud.lm.sl.cf.web.message.Messages;

public abstract class TokenGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenGenerator.class);
    private final AccessTokenDao accessTokenDao;

    protected TokenGenerator(AccessTokenDao accessTokenDao) {
        this.accessTokenDao = accessTokenDao;
    }

    public void storeAccessToken(AccessToken accessToken) {
        LOGGER.info(MessageFormat.format(Messages.STORING_TOKEN_FOR_USER_0_WHICH_EXPIRES_AT_1, accessToken.getUsername(),
                                         accessToken.getExpiresAt()));
        accessTokenDao.add(accessToken);
    }

    public abstract OAuth2AccessTokenWithAdditionalInfo generate(String tokenString);

    protected AccessToken buildAccessToken(OAuth2AccessTokenWithAdditionalInfo oAuth2AccessTokenWithAdditionalInfo) {
        String exchangedTokenValue = MapUtils.getString(oAuth2AccessTokenWithAdditionalInfo.getAdditionalInfo(), EXCHANGED_TOKEN);
        String clientId = MapUtils.getString(oAuth2AccessTokenWithAdditionalInfo.getAdditionalInfo(), Constants.CLIENT_ID);
        return new AccessToken(oAuth2AccessTokenWithAdditionalInfo.getDefaultValue()
                                                                  .getBytes(StandardCharsets.UTF_8),
                               exchangedTokenValue != null ? exchangedTokenValue.getBytes(StandardCharsets.UTF_8) : null,
                               clientId,
                               extractUsername(oAuth2AccessTokenWithAdditionalInfo),
                               calculateAccessTokenExpirationDate(oAuth2AccessTokenWithAdditionalInfo));
    }

    protected String extractUsername(OAuth2AccessTokenWithAdditionalInfo token) {
        return (String) token.getAdditionalInfo()
                             .get(TokenProperties.USER_NAME_KEY);
    }

    private LocalDateTime calculateAccessTokenExpirationDate(OAuth2AccessTokenWithAdditionalInfo oAuth2AccessTokenWithAdditionalInfo) {
        long expirationInSeconds = ((Number) oAuth2AccessTokenWithAdditionalInfo.getAdditionalInfo()
                                                                                .get(EXPIRES_AT_KEY)).longValue();
        return Instant.ofEpochSecond(expirationInSeconds)
                      .atZone(ZoneOffset.UTC)
                      .toLocalDateTime();
    }
}
