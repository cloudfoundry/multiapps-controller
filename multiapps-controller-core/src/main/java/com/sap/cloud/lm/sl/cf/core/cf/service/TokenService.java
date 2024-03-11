package com.sap.cloud.lm.sl.cf.core.cf.service;

import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.cloudfoundry.client.lib.oauth2.OAuth2AccessTokenWithAdditionalInfo;
import org.cloudfoundry.client.lib.oauth2.OAuth2AccessTokenWithAdditionalInfoAndId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.stereotype.Component;
import org.springframework.util.ConcurrentReferenceHashMap;

import com.sap.cloud.lm.sl.cf.core.dao.AccessTokenDao;
import com.sap.cloud.lm.sl.cf.core.dao.filters.OrderDirection;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.AccessToken;
import com.sap.cloud.lm.sl.cf.core.security.token.TokenParserChain;
import com.sap.cloud.lm.sl.cf.core.util.SingleThreadExecutor;

@Component
public class TokenService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenService.class);

    private static final int MAX_RETRIES = 5;
    private static final int BACKOFF_IN_MILLIS = 10000;

    private final AccessTokenDao accessTokenDao;
    private final TokenParserChain tokenParserChain;
    private final SingleThreadExecutor singleThreadExecutor;
    private final Map<String, OAuth2AccessTokenWithAdditionalInfo> cachedOauth2AccessTokens = createAccessTokenCache();

    @Inject
    public TokenService(AccessTokenDao accessTokenDao, TokenParserChain tokenParserChain, SingleThreadExecutor singleThreadExecutor) {
        this.accessTokenDao = accessTokenDao;
        this.tokenParserChain = tokenParserChain;
        this.singleThreadExecutor = singleThreadExecutor;
    }

    /**
     * Chooses a token among all tokens for this user in the access token table.
     *
     * @param username the username
     * @return the latest token, or throws an exception if token is not found
     */
    public OAuth2AccessTokenWithAdditionalInfo getToken(String username) {
        int retriesLeft = MAX_RETRIES;
        int currentBackoffInMillis = BACKOFF_IN_MILLIS;
        while (true) {
            try {
                return getValidToken(username);
            } catch (Exception e) {
                LOGGER.warn(MessageFormat.format(Messages.ERROR_OCCURRED_WHILE_FETCHING_A_TOKEN_WILL_RETRY_AFTER_0,
                                                 TimeUnit.MILLISECONDS.toSeconds(currentBackoffInMillis)));
                LOGGER.error(e.getMessage(), e);
                if (retriesLeft == 0) {
                    throw e;
                }
                sleep(currentBackoffInMillis);
                currentBackoffInMillis *= 2;
                retriesLeft--;
            }
        }
    }

    private OAuth2AccessTokenWithAdditionalInfo getValidToken(String username) {
        OAuth2AccessTokenWithAdditionalInfo cachedAccessToken = cachedOauth2AccessTokens.get(username);
        if (shouldUseCachedToken(cachedAccessToken)) {
            return cachedAccessToken;
        }
        List<AccessToken> sortedAccessTokens = accessTokenDao.getTokensByUsernameSortedByExpirationDate(username,
                                                                                                        OrderDirection.DESCENDING);
        if (sortedAccessTokens.isEmpty()) {
            throw new IllegalStateException(MessageFormat.format(Messages.NO_TOKENS_FOUND_FOR_USER, username));
        }
        OAuth2AccessTokenWithAdditionalInfoAndId validToken = getLatestValidToken(sortedAccessTokens, username);
        cachedOauth2AccessTokens.put(username, validToken.getOAuth2AccessTokenWithAdditionalInfo());
        deleteTokens(getLeftoverTokens(sortedAccessTokens, validToken));
        return validToken.getOAuth2AccessTokenWithAdditionalInfo();
    }

    private boolean shouldUseCachedToken(OAuth2AccessTokenWithAdditionalInfo cachedAccessToken) {
        boolean isNotExpired = cachedAccessToken != null && !cachedAccessToken.expiresBefore(Instant.now()
                                                                                                    .plusSeconds(120));
        return isNotExpired && isTokenValid(cachedAccessToken);
    }

    private boolean isTokenValid(OAuth2AccessTokenWithAdditionalInfo token) {
        try {
            tokenParserChain.parse(token.getValue());
            return true;
        } catch (InternalAuthenticationServiceException e) {
            LOGGER.error(e.getMessage(), e);
            return false;
        }
    }

    private OAuth2AccessTokenWithAdditionalInfoAndId getLatestValidToken(List<AccessToken> accessTokens, String username) {
        for (AccessToken accessToken : accessTokens) {
            try {
                OAuth2AccessTokenWithAdditionalInfo token = tokenParserChain.parse(new String(accessToken.getValue(),
                                                                                              StandardCharsets.UTF_8));
                return new OAuth2AccessTokenWithAdditionalInfoAndId(accessToken.getId(), token);
            } catch (InternalAuthenticationServiceException e) {
                LOGGER.error(e.getMessage(), e);
                singleThreadExecutor.submitTask(() -> deleteToken(accessToken));
            }
        }
        throw new IllegalStateException(MessageFormat.format(Messages.NO_VALID_TOKEN_FOUND_FOR_USER, username));
    }

    private List<AccessToken> getLeftoverTokens(List<AccessToken> sortedAccessTokens, OAuth2AccessTokenWithAdditionalInfoAndId validToken) {
        return sortedAccessTokens.stream()
                                 .filter(accessToken -> !Objects.equals(validToken.getId(), accessToken.getId()))
                                 .collect(Collectors.toList());
    }

    private void deleteTokens(List<AccessToken> accessTokens) {
        if (accessTokens.isEmpty()) {
            return;
        }
        LOGGER.debug(MessageFormat.format(Messages.DELETED_TOKENS, accessTokens.size()));
        singleThreadExecutor.submitTask(() -> accessTokens.forEach(this::deleteToken));
    }

    private void deleteToken(AccessToken token) {
        try {
            accessTokenDao.remove(token);
        } catch (Exception e) {
            LOGGER.error(MessageFormat.format(Messages.ERROR_DURING_TOKEN_DELETION_FOR_USER, token.getUsername()));
            LOGGER.error(e.getMessage(), e);
        }
    }

    protected void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread()
                  .interrupt();
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    protected Map<String, OAuth2AccessTokenWithAdditionalInfo> createAccessTokenCache() {
        return new ConcurrentReferenceHashMap<>();
    }
}
