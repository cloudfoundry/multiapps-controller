package org.cloudfoundry.multiapps.controller.web.security;

import static com.sap.cloudfoundry.client.facade.oauth2.TokenFactory.EXPIRES_AT_KEY;
import static org.cloudfoundry.multiapps.controller.client.util.TokenProperties.USER_NAME_KEY;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.AccessToken;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableAccessToken;
import org.cloudfoundry.multiapps.controller.persistence.query.AccessTokenQuery;
import org.cloudfoundry.multiapps.controller.persistence.services.AccessTokenService;
import org.cloudfoundry.multiapps.controller.web.util.TokenParsingStrategy;
import org.cloudfoundry.multiapps.controller.web.util.TokenParsingStrategyFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.web.server.ResponseStatusException;

import com.sap.cloudfoundry.client.facade.oauth2.OAuth2AccessTokenWithAdditionalInfo;

class AuthorizationLoaderFilterTest {

    private static final LocalDateTime DATE_1 = LocalDateTime.of(2021, Month.MARCH, 15, 9, 54, 10);
    private static final LocalDateTime DATE_2 = LocalDateTime.of(2021, Month.MARCH, 31, 9, 54, 20);
    private static final long EXPIRES_AT_IN_MILLIS = 1617114290L;

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;
    @Mock
    private AccessTokenService accessTokenService;
    @Mock
    private TokenParsingStrategyFactory tokenParsingStrategyFactory;
    @Mock
    private ApplicationConfiguration applicationConfiguration;
    @InjectMocks
    private AuthorizationLoaderFilter authorizationLoaderFilter;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
    }

    @Test
    void testNonAuthorizedRequest() throws ServletException, IOException {
        assertDoesNotThrow(() -> authorizationLoaderFilter.doFilterInternal(request, response, filterChain));
        Mockito.verify(filterChain)
               .doFilter(request, response);
    }

    @Test
    void testStoringTokenWhenThereAreNoTokensAssociatedWithUser() {
        Mockito.when(request.getHeader(HttpHeaders.AUTHORIZATION))
               .thenReturn("Bearer token");
        OAuth2AccessTokenWithAdditionalInfo token = getMockedOAuth2AccessTokenWithAdditionalInfo();
        mockTokenParsingStrategyFactory(token);
        mockAccessTokenService(Collections.emptyList());
        assertDoesNotThrow(() -> authorizationLoaderFilter.doFilterInternal(request, response, filterChain));
        Mockito.verify(accessTokenService)
               .add(any());
    }

    @Test
    void testStoringTokenCurrentTokenIsNewer() {
        Mockito.when(request.getHeader(HttpHeaders.AUTHORIZATION))
               .thenReturn("Bearer token");
        OAuth2AccessTokenWithAdditionalInfo token = getMockedOAuth2AccessTokenWithAdditionalInfo();
        mockTokenParsingStrategyFactory(token);
        AccessToken olderToken = ImmutableAccessToken.builder()
                                                     .username("test-user")
                                                     .value(new byte[0])
                                                     .expiresAt(DATE_1)
                                                     .build();
        mockAccessTokenService(List.of(olderToken));
        assertDoesNotThrow(() -> authorizationLoaderFilter.doFilterInternal(request, response, filterChain));
        Mockito.verify(accessTokenService)
               .add(any());
    }

    @Test
    void testStoringTokenCurrentTokenIsOlder() {
        Mockito.when(request.getHeader(HttpHeaders.AUTHORIZATION))
               .thenReturn("Bearer token");
        OAuth2AccessTokenWithAdditionalInfo token = getMockedOAuth2AccessTokenWithAdditionalInfo();
        mockTokenParsingStrategyFactory(token);
        AccessToken olderToken = ImmutableAccessToken.builder()
                                                     .username("test-user")
                                                     .value(new byte[0])
                                                     .expiresAt(DATE_2)
                                                     .build();
        mockAccessTokenService(List.of(olderToken));
        assertDoesNotThrow(() -> authorizationLoaderFilter.doFilterInternal(request, response, filterChain));
        Mockito.verify(accessTokenService, never())
               .add(any());
    }

    @Test
    void testStoringTokenCurrentTokenIsNewEnough() {
        Mockito.when(request.getHeader(HttpHeaders.AUTHORIZATION))
               .thenReturn("Bearer token");
        OAuth2AccessTokenWithAdditionalInfo token = getMockedOAuth2AccessTokenWithAdditionalInfo();
        mockTokenParsingStrategyFactory(token);
        AccessToken olderToken = ImmutableAccessToken.builder()
                                                     .username("test-user")
                                                     .value(new byte[0])
                                                     .expiresAt(ZonedDateTime.now()
                                                                             .plus(Duration.ofSeconds(130))
                                                                             .toLocalDateTime())
                                                     .build();
        mockAccessTokenService(List.of(olderToken));
        assertDoesNotThrow(() -> authorizationLoaderFilter.doFilterInternal(request, response, filterChain));
        Mockito.verify(accessTokenService, never())
               .add(any());
    }

    @Test
    void testCallWithExpiredToken() {
        Mockito.when(request.getHeader(HttpHeaders.AUTHORIZATION))
               .thenReturn("Bearer token");
        OAuth2AccessTokenWithAdditionalInfo token = Mockito.mock(OAuth2AccessTokenWithAdditionalInfo.class);
        OAuth2AccessToken oAuth2AccessToken = Mockito.mock(OAuth2AccessToken.class);
        Mockito.when(oAuth2AccessToken.getTokenValue())
               .thenReturn("Bearer token");
        Mockito.when(oAuth2AccessToken.getScopes())
               .thenReturn(Set.of("some-scope"));
        Mockito.when(oAuth2AccessToken.getExpiresAt())
               .thenReturn(Instant.now()
                                  .minus(Duration.ofMinutes(3)));
        Mockito.when(token.getOAuth2AccessToken())
               .thenReturn(oAuth2AccessToken);
        mockTokenParsingStrategyFactory(token);
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                                                 () -> authorizationLoaderFilter.doFilterInternal(request, response, filterChain));
        assertEquals(HttpStatus.UNAUTHORIZED, e.getStatus());
    }

    private OAuth2AccessTokenWithAdditionalInfo getMockedOAuth2AccessTokenWithAdditionalInfo() {
        OAuth2AccessTokenWithAdditionalInfo token = Mockito.mock(OAuth2AccessTokenWithAdditionalInfo.class);
        OAuth2AccessToken oAuth2AccessToken = Mockito.mock(OAuth2AccessToken.class);
        Mockito.when(oAuth2AccessToken.getTokenValue())
               .thenReturn("Bearer token");
        Mockito.when(oAuth2AccessToken.getScopes())
               .thenReturn(Set.of("some-scope"));
        Mockito.when(oAuth2AccessToken.getExpiresAt())
               .thenReturn(Instant.now()
                                  .plus(Duration.ofMinutes(3)));
        Mockito.when(token.getOAuth2AccessToken())
               .thenReturn(oAuth2AccessToken);
        Map<String, Object> tokenProperties = Map.of(USER_NAME_KEY, "test-user", EXPIRES_AT_KEY, EXPIRES_AT_IN_MILLIS);
        Mockito.when(token.getAdditionalInfo())
               .thenReturn(tokenProperties);
        return token;
    }

    private void mockTokenParsingStrategyFactory(OAuth2AccessTokenWithAdditionalInfo token) {

        TokenParsingStrategy tokenParsingStrategy = Mockito.mock(TokenParsingStrategy.class);
        Mockito.when(tokenParsingStrategy.parseToken(any()))
               .thenReturn(token);
        Mockito.when(tokenParsingStrategyFactory.createStrategy(any()))
               .thenReturn(tokenParsingStrategy);
    }

    private void mockAccessTokenService(List<AccessToken> accessTokensToReturn) {
        AccessTokenQuery accessTokenQuery = Mockito.mock(AccessTokenQuery.class);
        Mockito.when(accessTokenQuery.username(any()))
               .thenReturn(accessTokenQuery);
        Mockito.when(accessTokenQuery.list())
               .thenReturn(accessTokensToReturn);
        Mockito.when(accessTokenService.createQuery())
               .thenReturn(accessTokenQuery);
    }

}
