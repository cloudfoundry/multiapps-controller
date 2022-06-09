package com.sap.cloud.lm.sl.cf.core.cf.service;

import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import org.cloudfoundry.client.lib.oauth2.OAuth2AccessTokenWithAdditionalInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.ConcurrentReferenceHashMap;

import com.sap.cloud.lm.sl.cf.core.dao.AccessTokenDao;
import com.sap.cloud.lm.sl.cf.core.dao.filters.OrderDirection;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.AccessToken;
import com.sap.cloud.lm.sl.cf.core.security.token.TokenParserChain;

@Component
public class TokenService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenService.class);

    private final AccessTokenDao accessTokenDao;
    private final TokenParserChain tokenParserChain;
    private final Map<String, OAuth2AccessTokenWithAdditionalInfo> cachedOauth2AccessTokens = new ConcurrentReferenceHashMap<>();

    @Inject
    public TokenService(AccessTokenDao accessTokenDao, TokenParserChain tokenParserChain) {
        this.accessTokenDao = accessTokenDao;
        this.tokenParserChain = tokenParserChain;
    }

    /**
     * Chooses a token among all tokens for this user in the access token table.
     *
     * @param username the username
     * @return the latest token, or throw an exception if token is not found
     */
    public OAuth2AccessTokenWithAdditionalInfo getToken(String username) {
        OAuth2AccessTokenWithAdditionalInfo cachedAccessToken = cachedOauth2AccessTokens.get(username);
        if (shouldUseCachedToken(cachedAccessToken)) {
            return cachedAccessToken;
        }
        List<AccessToken> accessTokens = getTokensByUsernameSortedByExpirationDateDescending(username);
        if (accessTokens.isEmpty()) {
            throw new IllegalStateException(MessageFormat.format(Messages.NO_VALID_TOKEN_FOUND, username));
        }
        OAuth2AccessTokenWithAdditionalInfo tokenByUser = getLatestToken(accessTokens);
        cachedOauth2AccessTokens.put(username, tokenByUser);
        deleteTokens(accessTokens.subList(1, accessTokens.size()));
        return tokenByUser;
    }

    private boolean shouldUseCachedToken(OAuth2AccessTokenWithAdditionalInfo cachedAccessToken) {
        return cachedAccessToken != null && !cachedAccessToken.getExpiresAt()
                                                              .isBefore(Instant.now()
                                                                               .plus(120, ChronoUnit.SECONDS));
    }

    private List<AccessToken> getTokensByUsernameSortedByExpirationDateDescending(String userName) {
        return accessTokenDao.getTokensByUsernameSortedByExpirationDate(userName, OrderDirection.DESCENDING);
    }

    private OAuth2AccessTokenWithAdditionalInfo getLatestToken(List<AccessToken> accessTokens) {
        AccessToken latestAccessToken = accessTokens.get(0);
        return tokenParserChain.parse(new String(latestAccessToken.getValue(), StandardCharsets.UTF_8));
    }

    private void deleteTokens(List<AccessToken> accessTokens) {
        if (accessTokens.isEmpty()) {
            return;
        }
        Executors.newSingleThreadExecutor()
                 .submit(() -> accessTokens.forEach(this::deleteToken));
    }

    private void deleteToken(AccessToken token) {
        try {
            accessTokenDao.remove(token);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }
}
