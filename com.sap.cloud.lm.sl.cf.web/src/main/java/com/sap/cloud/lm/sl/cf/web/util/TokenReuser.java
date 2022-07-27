package com.sap.cloud.lm.sl.cf.web.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import javax.inject.Named;

import org.cloudfoundry.client.lib.oauth2.OAuth2AccessTokenWithAdditionalInfo;

import com.sap.cloud.lm.sl.cf.core.dao.AccessTokenDao;
import com.sap.cloud.lm.sl.cf.core.dao.filters.OrderDirection;
import com.sap.cloud.lm.sl.cf.core.model.AccessToken;

@Named
public class TokenReuser {

    private final AccessTokenDao accessTokenDao;

    public TokenReuser(AccessTokenDao accessTokenDao) {
        this.accessTokenDao = accessTokenDao;
    }

    public Optional<AccessToken> getTokenWithExpirationAfter(String username, long expiresAfterInSeconds) {
        List<AccessToken> accessTokens = getTokensOrderedByExpiresAt(username);
        if (accessTokens.isEmpty()) {
            return Optional.empty();
        }
        LocalDateTime dateAfter = calculateDateAfter(expiresAfterInSeconds);
        if (shouldUseLatestToken(accessTokens, dateAfter)) {
            return Optional.of(accessTokens.get(0));
        }
        return Optional.empty();
    }

    private List<AccessToken> getTokensOrderedByExpiresAt(String username) {
        return accessTokenDao.getTokensByUsernameSortedByExpirationDate(username, OrderDirection.DESCENDING);
    }

    private LocalDateTime calculateDateAfter(long expiresAfterInSeconds) {
        return ZonedDateTime.now()
                            .plus(Duration.ofSeconds(expiresAfterInSeconds))
                            .toLocalDateTime();
    }

    private boolean shouldUseLatestToken(List<AccessToken> accessTokens, LocalDateTime dateAfter) {
        return accessTokens.get(0)
                           .getExpiresAt()
                           .isAfter(dateAfter);
    }

    public Optional<AccessToken> getTokenWithExpirationAfterOrReuseCurrent(String username, long expiresAfterInSeconds,
                                                                           OAuth2AccessTokenWithAdditionalInfo currentToken) {
        List<AccessToken> accessTokens = getTokensOrderedByExpiresAt(username);
        if (accessTokens.isEmpty()) {
            return Optional.empty();
        }
        LocalDateTime dateAfter = calculateDateAfter(expiresAfterInSeconds);
        if (shouldUseLatestToken(accessTokens, dateAfter)) {
            return Optional.of(accessTokens.get(0));
        }
        LocalDateTime currentTokenExpirationDate = getExpirationDate(currentToken);
        if (currentTokenExpirationDate.equals(accessTokens.get(0)
                                                          .getExpiresAt())) {
            return Optional.of(accessTokens.get(0));
        }
        return Optional.empty();
    }

    private LocalDateTime getExpirationDate(OAuth2AccessTokenWithAdditionalInfo currentToken) {
        return LocalDateTime.ofInstant(currentToken.getExpiresAt(), ZoneId.systemDefault());
    }

}
