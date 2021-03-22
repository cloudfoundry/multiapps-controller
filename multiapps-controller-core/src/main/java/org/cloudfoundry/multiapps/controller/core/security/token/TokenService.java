package org.cloudfoundry.multiapps.controller.core.security.token;

import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.security.token.parsers.TokenParserChain;
import org.cloudfoundry.multiapps.controller.persistence.OrderDirection;
import org.cloudfoundry.multiapps.controller.persistence.model.AccessToken;
import org.cloudfoundry.multiapps.controller.persistence.services.AccessTokenService;
import org.springframework.util.ConcurrentReferenceHashMap;

import com.sap.cloudfoundry.client.facade.oauth2.OAuth2AccessTokenWithAdditionalInfo;

/**
 * Provides functionality for persisting, updating and removing tokens from a token store
 */
@Named
public class TokenService {

    private final AccessTokenService accessTokenService;
    private final TokenParserChain tokenParserChain;
    private final Map<String, OAuth2AccessTokenWithAdditionalInfo> cachedOauth2AccessTokens = new ConcurrentReferenceHashMap<>();

    @Inject
    public TokenService(AccessTokenService accessTokenService, TokenParserChain tokenParserChain) {
        this.accessTokenService = accessTokenService;
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
        List<AccessToken> accessTokens = getSortedAccessTokensByUsername(username);
        if (accessTokens.isEmpty()) {
            throw new IllegalStateException(MessageFormat.format(Messages.NO_VALID_TOKEN_FOUND, username));
        }
        OAuth2AccessTokenWithAdditionalInfo tokenByUser = getLatestToken(accessTokens);
        cachedOauth2AccessTokens.put(username, tokenByUser);
        deleteTokens(accessTokens.subList(1, accessTokens.size()));
        return tokenByUser;
    }

    private boolean shouldUseCachedToken(OAuth2AccessTokenWithAdditionalInfo cachedAccessToken) {
        return cachedAccessToken != null && !cachedAccessToken.getOAuth2AccessToken()
                                                              .getExpiresAt()
                                                              .isBefore(Instant.now()
                                                                               .plus(120, ChronoUnit.SECONDS));
    }

    private List<AccessToken> getSortedAccessTokensByUsername(String userName) {
        return accessTokenService.createQuery()
                                 .username(userName)
                                 .orderByExpiresAt(OrderDirection.DESCENDING)
                                 .list();
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
        accessTokenService.createQuery()
                          .id(token.getId())
                          .delete();
    }
}
