package org.cloudfoundry.multiapps.controller.web.security;

import static com.sap.cloudfoundry.client.facade.oauth2.TokenFactory.EXPIRES_AT_KEY;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.core.util.SSLUtil;
import org.cloudfoundry.multiapps.controller.core.util.SecurityUtil;
import org.cloudfoundry.multiapps.controller.core.util.UserInfo;
import org.cloudfoundry.multiapps.controller.persistence.model.AccessToken;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableAccessToken;
import org.cloudfoundry.multiapps.controller.persistence.services.AccessTokenService;
import org.cloudfoundry.multiapps.controller.web.Messages;
import org.cloudfoundry.multiapps.controller.web.util.TokenParsingStrategy;
import org.cloudfoundry.multiapps.controller.web.util.TokenParsingStrategyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

import com.sap.cloudfoundry.client.facade.oauth2.OAuth2AccessTokenWithAdditionalInfo;

@Named
public class AuthorizationLoaderFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationLoaderFilter.class);

    private final AccessTokenService accessTokenService;
    private final TokenParsingStrategyFactory tokenParsingStrategyFactory;

    @Inject
    public AuthorizationLoaderFilter(AccessTokenService accessTokenService, TokenParsingStrategyFactory tokenParsingStrategyFactory,
                                     ApplicationConfiguration applicationConfiguration) {
        this.accessTokenService = accessTokenService;
        this.tokenParsingStrategyFactory = tokenParsingStrategyFactory;
        if (applicationConfiguration.shouldSkipSslValidation()) {
            SSLUtil.disableSSLValidation();
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        String tokenStringWithType = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (tokenStringWithType == null) {
            failWithUnauthorized(Messages.NO_AUTHORIZATION_HEADER_WAS_PROVIDED);
        }
        OAuth2AccessTokenWithAdditionalInfo oAuth2AccessTokenWithAdditionalInfo = createOAuth2AccessToken(tokenStringWithType);
        validateTokenExpiration(oAuth2AccessTokenWithAdditionalInfo);
        UserInfo tokenUserInfo = SecurityUtil.getTokenUserInfo(oAuth2AccessTokenWithAdditionalInfo);
        loadAuthenticationInContext(tokenUserInfo);
        AccessToken accessToken = buildAccessToken(oAuth2AccessTokenWithAdditionalInfo, tokenUserInfo);
        List<AccessToken> accessTokensByUsername = findTokensByUsername(tokenUserInfo.getName());
        storeTokenInDatabaseIfNeeded(accessToken, accessTokensByUsername);
        filterChain.doFilter(request, response);
    }

    private void failWithUnauthorized(String message) {
        SecurityContextHolder.clearContext();
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
    }

    private OAuth2AccessTokenWithAdditionalInfo createOAuth2AccessToken(String tokenStringWithType) {
        String[] typeWithAuthorization = tokenStringWithType.split("\\s");
        TokenParsingStrategy tokenParsingStrategy = tokenParsingStrategyFactory.createStrategy(typeWithAuthorization[0]);
        return tokenParsingStrategy.parseToken(typeWithAuthorization[1]);
    }

    private void validateTokenExpiration(OAuth2AccessTokenWithAdditionalInfo oAuth2AccessTokenWithAdditionalInfo) {
        if (oAuth2AccessTokenWithAdditionalInfo.getOAuth2AccessToken()
                                               .getExpiresAt()
                                               .isBefore(Instant.now())) {
            failWithUnauthorized(MessageFormat.format(Messages.THE_TOKEN_HAS_EXPIRED_ON_0,
                                                      oAuth2AccessTokenWithAdditionalInfo.getOAuth2AccessToken()
                                                                                         .getExpiresAt()));
        }
    }

    private void loadAuthenticationInContext(UserInfo tokenUserInfo) {
        OAuth2AuthenticationToken authentication = SecurityUtil.createAuthentication(tokenUserInfo);
        SecurityContextHolder.getContext()
                             .setAuthentication(authentication);
    }

    private ImmutableAccessToken buildAccessToken(OAuth2AccessTokenWithAdditionalInfo oAuth2AccessTokenWithAdditionalInfo,
                                                  UserInfo tokenUserInfo) {
        return ImmutableAccessToken.builder()
                                   .value(oAuth2AccessTokenWithAdditionalInfo.getOAuth2AccessToken()
                                                                             .getTokenValue()
                                                                             .getBytes(StandardCharsets.UTF_8))
                                   .username(tokenUserInfo.getName())
                                   .expiresAt(calculateAccessTokenExpirationDate(oAuth2AccessTokenWithAdditionalInfo))
                                   .build();
    }

    private LocalDateTime calculateAccessTokenExpirationDate(OAuth2AccessTokenWithAdditionalInfo oAuth2AccessTokenWithAdditionalInfo) {
        long expirationInSeconds = ((Number) oAuth2AccessTokenWithAdditionalInfo.getAdditionalInfo()
                                                                                .get(EXPIRES_AT_KEY)).longValue();
        return Instant.ofEpochSecond(expirationInSeconds)
                      .atZone(ZoneId.systemDefault())
                      .toLocalDateTime();
    }

    private List<AccessToken> findTokensByUsername(String username) {
        return accessTokenService.createQuery()
                                 .username(username)
                                 .list();
    }

    private void storeTokenInDatabaseIfNeeded(AccessToken accessToken, List<AccessToken> accessTokens) {
        Optional<AccessToken> tokenWithTheLatestExpiration = accessTokens.stream()
                                                                         .max(Comparator.comparing(AccessToken::getExpiresAt));
        if (tokenWithTheLatestExpiration.isEmpty()) {
            storeAccessToken(accessToken);
            return;
        }
        if (expiresInMoreThan2Minutes(tokenWithTheLatestExpiration.get())) {
            return;
        }
        if (accessToken.getExpiresAt()
                       .isAfter(tokenWithTheLatestExpiration.get()
                                                            .getExpiresAt())) {
            storeAccessToken(accessToken);
        }
    }

    private boolean expiresInMoreThan2Minutes(AccessToken accessToken) {
        LocalDateTime dateAfter2Minutes = ZonedDateTime.now()
                                                       .plus(Duration.ofSeconds(120))
                                                       .toLocalDateTime();
        return accessToken.getExpiresAt()
                          .isAfter(dateAfter2Minutes);
    }

    private void storeAccessToken(AccessToken accessToken) {
        LOGGER.info(MessageFormat.format(Messages.STORING_TOKEN_FOR_USER_0_WHICH_EXPIRES_AT_1, accessToken.getUsername(),
                                         accessToken.getExpiresAt()));
        accessTokenService.add(accessToken);
    }
}
