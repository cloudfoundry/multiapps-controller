package org.cloudfoundry.multiapps.controller.web.util;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.cloudfoundry.multiapps.controller.persistence.model.AccessToken;
import org.cloudfoundry.multiapps.controller.persistence.query.AccessTokenQuery;
import org.cloudfoundry.multiapps.controller.persistence.services.AccessTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import com.sap.cloudfoundry.client.facade.oauth2.OAuth2AccessTokenWithAdditionalInfo;

class TokenReuserTest {

    @Mock
    private AccessTokenService accessTokenService;
    @InjectMocks
    private TokenReuser tokenReuser;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
    }

    @Test
    void testGetTokenWithExpirationAfterNoTokensInDatabase() {
        AccessTokenQuery accessTokenQuery = getMockedAccessTokenQuery();
        Mockito.when(accessTokenQuery.list())
               .thenReturn(Collections.emptyList());
        Optional<AccessToken> token = tokenReuser.getTokenWithExpirationAfter("username", 100);
        assertTrue(token.isEmpty());
    }

    @Test
    void testGetTokenWithExpirationAfterTokenShouldBeReused() {
        AccessTokenQuery accessTokenQuery = getMockedAccessTokenQuery();
        LocalDateTime datePlus5Mins = ZonedDateTime.now()
                                                   .plus(Duration.ofSeconds(5 * 60))
                                                   .toLocalDateTime();
        AccessToken accessToken = getMockedAccessToken(datePlus5Mins);
        Mockito.when(accessTokenQuery.list())
               .thenReturn(List.of(accessToken));
        Optional<AccessToken> token = tokenReuser.getTokenWithExpirationAfter("test_user", 100);
        assertTrue(token.isPresent());
    }

    @Test
    void testGetTokenWithExpirationAfterTokenShouldNotBeReused() {
        AccessTokenQuery accessTokenQuery = getMockedAccessTokenQuery();
        LocalDateTime dateMinus5Mins = ZonedDateTime.now()
                                                    .minus(Duration.ofSeconds(5 * 60))
                                                    .toLocalDateTime();
        AccessToken accessToken = getMockedAccessToken(dateMinus5Mins);
        Mockito.when(accessTokenQuery.list())
               .thenReturn(List.of(accessToken));
        Optional<AccessToken> token = tokenReuser.getTokenWithExpirationAfter("test_user", 100);
        assertTrue(token.isEmpty());
    }

    @Test
    void testGetTokenWithExpirationAfterOrReuseCurrentNoTokensInDatabase() {
        AccessTokenQuery accessTokenQuery = getMockedAccessTokenQuery();
        Mockito.when(accessTokenQuery.list())
               .thenReturn(Collections.emptyList());
        OAuth2AccessTokenWithAdditionalInfo oauth2AccessToken = getMockedOauth2AccessToken(ZonedDateTime.now()
                                                                                                        .toLocalDateTime());
        Optional<AccessToken> token = tokenReuser.getTokenWithExpirationAfterOrReuseCurrent("username", 100, oauth2AccessToken);
        assertTrue(token.isEmpty());
    }

    @Test
    void testGetTokenWithExpirationAfterOrReuseTokenTokenShouldBeReused() {
        AccessTokenQuery accessTokenQuery = getMockedAccessTokenQuery();
        LocalDateTime datePlus5Mins = ZonedDateTime.now()
                                                   .plus(Duration.ofSeconds(5 * 60))
                                                   .toLocalDateTime();
        AccessToken accessToken = getMockedAccessToken(datePlus5Mins);
        Mockito.when(accessTokenQuery.list())
               .thenReturn(List.of(accessToken));
        OAuth2AccessTokenWithAdditionalInfo oauth2AccessToken = getMockedOauth2AccessToken(ZonedDateTime.now()
                                                                                                        .toLocalDateTime());
        Optional<AccessToken> token = tokenReuser.getTokenWithExpirationAfterOrReuseCurrent("test_user", 100, oauth2AccessToken);
        assertTrue(token.isPresent());
    }

    @Test
    void testGetTokenWithExpirationAfterOrReuseCurrentTokenShouldNotBeReused() {
        AccessTokenQuery accessTokenQuery = getMockedAccessTokenQuery();
        LocalDateTime dateMinus5Mins = ZonedDateTime.now()
                                                    .minus(Duration.ofSeconds(5 * 60))
                                                    .toLocalDateTime();
        AccessToken accessToken = getMockedAccessToken(dateMinus5Mins);
        Mockito.when(accessTokenQuery.list())
               .thenReturn(List.of(accessToken));
        OAuth2AccessTokenWithAdditionalInfo oauth2AccessToken = getMockedOauth2AccessToken(ZonedDateTime.now()
                                                                                                        .toLocalDateTime());
        Optional<AccessToken> token = tokenReuser.getTokenWithExpirationAfterOrReuseCurrent("test_user", 100, oauth2AccessToken);
        assertTrue(token.isEmpty());
    }

    @Test
    void testGetTokenWithExpirationAfterOrReuseCurrentTokensMatch() {
        AccessTokenQuery accessTokenQuery = getMockedAccessTokenQuery();
        LocalDateTime dateNow = ZonedDateTime.now()
                                             .toLocalDateTime();
        AccessToken accessToken = getMockedAccessToken(dateNow);
        Mockito.when(accessTokenQuery.list())
               .thenReturn(List.of(accessToken));
        OAuth2AccessTokenWithAdditionalInfo oauth2AccessToken = getMockedOauth2AccessToken(dateNow);
        Optional<AccessToken> token = tokenReuser.getTokenWithExpirationAfterOrReuseCurrent("test_user", 100, oauth2AccessToken);
        assertTrue(token.isPresent());
    }

    private AccessTokenQuery getMockedAccessTokenQuery() {
        AccessTokenQuery accessTokenQuery = Mockito.mock(AccessTokenQuery.class);
        Mockito.when(accessTokenQuery.username(anyString()))
               .thenReturn(accessTokenQuery);
        Mockito.when(accessTokenQuery.orderByExpiresAt(any()))
               .thenReturn(accessTokenQuery);
        Mockito.when(accessTokenService.createQuery())
               .thenReturn(accessTokenQuery);
        return accessTokenQuery;
    }

    private AccessToken getMockedAccessToken(LocalDateTime expiresAt) {
        AccessToken accessToken = Mockito.mock(AccessToken.class);
        Mockito.when(accessToken.getExpiresAt())
               .thenReturn(expiresAt);
        Mockito.when(accessToken.getUsername())
               .thenReturn("test_user");
        return accessToken;
    }

    private OAuth2AccessTokenWithAdditionalInfo getMockedOauth2AccessToken(LocalDateTime expiresAt) {
        OAuth2AccessTokenWithAdditionalInfo oAuth2AccessTokenWithAdditionalInfo = Mockito.mock(OAuth2AccessTokenWithAdditionalInfo.class);
        OAuth2AccessToken oAuth2AccessToken = Mockito.mock(OAuth2AccessToken.class);
        Mockito.when(oAuth2AccessToken.getExpiresAt())
               .thenReturn(expiresAt.toInstant(ZoneOffset.UTC));
        Mockito.when(oAuth2AccessTokenWithAdditionalInfo.getOAuth2AccessToken())
               .thenReturn(oAuth2AccessToken);
        return oAuth2AccessTokenWithAdditionalInfo;
    }

}
