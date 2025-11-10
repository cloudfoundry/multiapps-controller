package org.cloudfoundry.multiapps.controller.core.security.token;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.multiapps.controller.client.facade.oauth2.OAuth2AccessTokenWithAdditionalInfo;
import org.cloudfoundry.multiapps.controller.client.util.TokenProperties;
import org.cloudfoundry.multiapps.controller.core.security.token.parsers.TokenParserChain;
import org.cloudfoundry.multiapps.controller.persistence.OrderDirection;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

class TokenServiceTest {

    private static final LocalDateTime DATE_1 = LocalDateTime.of(2021, Month.MARCH, 15, 9, 54, 10);
    @Mock
    private AccessTokenService accessTokenService;
    @Mock
    private TokenParserChain tokenParserChain;
    @InjectMocks
    private TokenService tokenService;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
    }

    @Test
    void testGetTokenWhenThereAreNoTokensForUser() {
        AccessTokenQuery accessTokenQuery = Mockito.mock(AccessTokenQuery.class);
        mockAccessTokenService(accessTokenQuery);
        Exception exception = assertThrows(IllegalStateException.class, () -> tokenService.getToken("123"));
        assertEquals("No valid access token was found for user guid \"123\"", exception.getMessage());
    }

    @Test
    void testGetTokenWhenThereIsOnlyOneTokenInDb() {
        AccessTokenQuery accessTokenQuery = Mockito.mock(AccessTokenQuery.class);
        AccessToken accessToken = Mockito.mock(AccessToken.class);
        Mockito.when(accessToken.getValue())
               .thenReturn(new byte[0]);
        Mockito.when(accessTokenQuery.list())
               .thenReturn(List.of(accessToken));
        mockAccessTokenService(accessTokenQuery);
        OAuth2AccessTokenWithAdditionalInfo mockedToken = Mockito.mock(OAuth2AccessTokenWithAdditionalInfo.class);
        Mockito.when(mockedToken.getAdditionalInfo())
               .thenReturn(Map.of(TokenProperties.USER_ID_KEY, "123"));
        Mockito.when(tokenParserChain.parse(any()))
               .thenReturn(mockedToken);
        OAuth2AccessTokenWithAdditionalInfo token = tokenService.getToken("123");
        assertEquals(mockedToken, token);
    }

    @Test
    void testGetTokenWhenThereIsNewerTokenInDb() {
        AccessTokenQuery accessTokenQuery = mockAccessTokenQuery();
        mockAccessTokenService(accessTokenQuery);
        OAuth2AccessTokenWithAdditionalInfo mockedToken = Mockito.mock(OAuth2AccessTokenWithAdditionalInfo.class);
        Mockito.when(mockedToken.getAdditionalInfo())
               .thenReturn(Map.of(TokenProperties.USER_ID_KEY, "123"));
        Mockito.when(tokenParserChain.parse(any()))
               .thenReturn(mockedToken);
        OAuth2AccessTokenWithAdditionalInfo token = tokenService.getToken("123");
        assertEquals(mockedToken, token);
    }

    @Test
    void testGetTokenWhenCachedIsAvailable() {
        AccessTokenQuery accessTokenQuery = mockAccessTokenQuery();
        mockAccessTokenService(accessTokenQuery);
        OAuth2AccessTokenWithAdditionalInfo mockedToken = Mockito.mock(OAuth2AccessTokenWithAdditionalInfo.class);
        OAuth2AccessToken oAuth2AccessToken = Mockito.mock(OAuth2AccessToken.class);
        Mockito.when(oAuth2AccessToken.getExpiresAt())
               .thenReturn(Instant.now()
                                  .plus(90, ChronoUnit.SECONDS));
        Mockito.when(mockedToken.getOAuth2AccessToken())
               .thenReturn(oAuth2AccessToken);
        Mockito.when(mockedToken.getAdditionalInfo())
               .thenReturn(Map.of(TokenProperties.USER_ID_KEY, "123"));
        Mockito.when(tokenParserChain.parse(any()))
               .thenReturn(mockedToken);
        tokenService.getToken("123");
        OAuth2AccessTokenWithAdditionalInfo token = tokenService.getToken("123");
        assertEquals(mockedToken, token);
    }

    private AccessTokenQuery mockAccessTokenQuery() {
        AccessTokenQuery accessTokenQuery = Mockito.mock(AccessTokenQuery.class);
        AccessToken olderAccessToken = Mockito.mock(AccessToken.class);
        Mockito.when(olderAccessToken.getValue())
               .thenReturn(new byte[0]);
        Mockito.when(olderAccessToken.getExpiresAt())
               .thenReturn(DATE_1);
        AccessToken newerAccessToken = Mockito.mock(AccessToken.class);
        Mockito.when(newerAccessToken.getValue())
               .thenReturn(new byte[0]);
        Mockito.when(newerAccessToken.getExpiresAt())
               .thenReturn(Instant.now()
                                  .atZone(ZoneId.systemDefault())
                                  .toLocalDateTime());
        Mockito.when(accessTokenQuery.list())
               .thenReturn(List.of(olderAccessToken, newerAccessToken));
        return accessTokenQuery;
    }

    private void mockAccessTokenService(AccessTokenQuery accessTokenQuery) {
        Mockito.when(accessTokenService.createQuery())
               .thenReturn(accessTokenQuery);
        Mockito.when(accessTokenQuery.userGuid(any()))
               .thenReturn(accessTokenQuery);
        Mockito.when(accessTokenQuery.username(any()))
               .thenReturn(accessTokenQuery);
        Mockito.when(accessTokenQuery.orderByExpiresAt(OrderDirection.DESCENDING))
               .thenReturn(accessTokenQuery);
        Mockito.when(accessTokenQuery.id(any()))
               .thenReturn(accessTokenQuery);
    }

}
