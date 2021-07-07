package org.cloudfoundry.multiapps.controller.web.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.persistence.OrderDirection;
import org.cloudfoundry.multiapps.controller.persistence.model.AccessToken;
import org.cloudfoundry.multiapps.controller.persistence.services.AccessTokenService;

import com.sap.cloudfoundry.client.facade.oauth2.OAuth2AccessTokenWithAdditionalInfo;

@Named
public class TokenReuser {

    private final AccessTokenService accessTokenService;

    public TokenReuser(AccessTokenService accessTokenService) {
        this.accessTokenService = accessTokenService;
    }

    public Optional<AccessToken> getTokenWithExpirationAfter(String username, int expiresAfterInSeconds) {
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
        return accessTokenService.createQuery()
                                 .username(username)
                                 .orderByExpiresAt(OrderDirection.DESCENDING)
                                 .list();
    }

    private LocalDateTime calculateDateAfter(int expiresAfterInSeconds) {
        return ZonedDateTime.now()
                            .plus(Duration.ofSeconds(expiresAfterInSeconds))
                            .toLocalDateTime();
    }

    private boolean shouldUseLatestToken(List<AccessToken> accessTokens, LocalDateTime dateAfter) {
        return accessTokens.get(0)
                           .getExpiresAt()
                           .isAfter(dateAfter);
    }

    public Optional<AccessToken> getTokenWithExpirationAfterOrReuseCurrent(String username, int expiresAfterInSeconds,
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
        return LocalDateTime.ofInstant(currentToken.getOAuth2AccessToken()
                                                   .getExpiresAt(),
                                       ZoneOffset.UTC);
    }

}
