package org.cloudfoundry.multiapps.controller.core.security.token;

import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.sap.cloudfoundry.client.facade.oauth2.OAuth2AccessTokenWithAdditionalInfo;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.client.util.TokenProperties;
import org.cloudfoundry.multiapps.controller.core.Constants;
import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.model.CachedMap;
import org.cloudfoundry.multiapps.controller.core.security.token.parsers.TokenParserChain;
import org.cloudfoundry.multiapps.controller.persistence.OrderDirection;
import org.cloudfoundry.multiapps.controller.persistence.model.AccessToken;
import org.cloudfoundry.multiapps.controller.persistence.services.AccessTokenService;
import org.springframework.beans.factory.DisposableBean;

/**
 * Provides functionality for persisting, updating and removing tokens from a token store
 */
@Named
public class TokenService implements DisposableBean {

    private final AccessTokenService accessTokenService;
    private final TokenParserChain tokenParserChain;
    private final Duration tokenExpirationTime = Duration.ofMinutes(10);
    private final CachedMap<String, OAuth2AccessTokenWithAdditionalInfo> cachedTokens = new CachedMap<>(tokenExpirationTime);
    private final ExecutorService threadPoolForTokensDeletion = new ThreadPoolExecutor(Constants.TOKEN_SERVICE_DELETION_CORE_POOL_SIZE,
                                                                                       Constants.TOKEN_SERVICE_DELETION_MAXIMUM_POOL_SIZE,
                                                                                       Constants.TOKEN_SERVICE_DELETION_KEEP_ALIVE_THREAD_IN_SECONDS,
                                                                                       TimeUnit.SECONDS, new LinkedBlockingQueue<>());

    @Inject
    public TokenService(AccessTokenService accessTokenService, TokenParserChain tokenParserChain) {
        this.accessTokenService = accessTokenService;
        this.tokenParserChain = tokenParserChain;
    }

    /**
     * Chooses a token among all tokens for this user in the access token table.
     *
     * @param username the username
     * @param userGuid the userGuid
     * @return the latest token, or throw an exception if token is not found
     */
    public OAuth2AccessTokenWithAdditionalInfo getToken(String username, String userGuid) {
        if (userGuid != null) {
            OAuth2AccessTokenWithAdditionalInfo cachedAccessToken = cachedTokens.get(userGuid);
            if (shouldUseCachedToken(cachedAccessToken)) {
                return cachedAccessToken;
            }
            return getLatestAccessTokenByUserGuid(userGuid);
        }
        // TODO: If no tokens are found for the userGuid, try to find tokens by username. This is temporary and should be removed in the next release.
        return getLatestAccessTokenByUsername(username);
    }

    private boolean shouldUseCachedToken(OAuth2AccessTokenWithAdditionalInfo cachedAccessToken) {
        return cachedAccessToken != null && !cachedAccessToken.getOAuth2AccessToken()
                                                              .getExpiresAt()
                                                              .isBefore(Instant.now()
                                                                               .plus(120, ChronoUnit.SECONDS));
    }

    private OAuth2AccessTokenWithAdditionalInfo getLatestAccessTokenByUserGuid(String userGuid) {
        List<AccessToken> tokensByGuid = getSortedAccessTokensByUserGuid(userGuid);
        if (tokensByGuid.isEmpty()) {
            throw new IllegalStateException(MessageFormat.format(Messages.NO_VALID_TOKEN_FOUND, userGuid));
        }
        OAuth2AccessTokenWithAdditionalInfo latestToken = getLatestToken(tokensByGuid);
        addTokenToCache(latestToken, tokensByGuid);
        return latestToken;
    }

    private List<AccessToken> getSortedAccessTokensByUserGuid(String userGuid) {
        return accessTokenService.createQuery()
                                 .userGuid(userGuid)
                                 .orderByExpiresAt(OrderDirection.DESCENDING)
                                 .list();
    }

    private void addTokenToCache(OAuth2AccessTokenWithAdditionalInfo token, List<AccessToken> accessTokens) {
        cachedTokens.put((String) token.getAdditionalInfo()
                                       .get(TokenProperties.USER_ID_KEY), token);
        if (accessTokens.size() > 1) {
            deleteTokens(accessTokens.subList(1, accessTokens.size()));
        }
    }

    private OAuth2AccessTokenWithAdditionalInfo getLatestAccessTokenByUsername(String username) {
        List<AccessToken> tokensByUsername = getSortedAccessTokensByUsername(username);
        if (tokensByUsername.isEmpty()) {
            throw new IllegalStateException(MessageFormat.format(Messages.NO_VALID_TOKEN_FOUND, username));
        }
        return getLatestToken(tokensByUsername);
    }

    private List<AccessToken> getSortedAccessTokensByUsername(String username) {
        return accessTokenService.createQuery()
                                 .username(username)
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
        threadPoolForTokensDeletion.submit(() -> doDeleteTokens(accessTokens));
    }

    private void doDeleteTokens(List<AccessToken> tokens) {
        accessTokenService.createQuery()
                          .withIdAnyOf(tokens.stream()
                                             .map(AccessToken::getId)
                                             .collect(Collectors.toList()))
                          .delete();
    }

    @Override
    public void destroy() {
        cachedTokens.clear();
    }
}
