package org.cloudfoundry.multiapps.controller.core.security.token;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

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

import com.sap.cloudfoundry.client.facade.oauth2.OAuth2AccessTokenWithAdditionalInfo;

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
        Exception exception = assertThrows(IllegalStateException.class, () -> tokenService.getToken("deploy-service-user"));
        assertEquals("No valid access token was found for user \"deploy-service-user\"", exception.getMessage());
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
        Mockito.when(tokenParserChain.parse(any()))
               .thenReturn(mockedToken);
        OAuth2AccessTokenWithAdditionalInfo token = tokenService.getToken("deploy-service-user");
        assertEquals(mockedToken, token);
    }

    @Test
    void testGetTokenWhenThereIsNewerTokenInDb() {
        AccessTokenQuery accessTokenQuery = mockAccessTokenQuery();
        mockAccessTokenService(accessTokenQuery);
        OAuth2AccessTokenWithAdditionalInfo mockedToken = Mockito.mock(OAuth2AccessTokenWithAdditionalInfo.class);
        Mockito.when(tokenParserChain.parse(any()))
               .thenReturn(mockedToken);
        OAuth2AccessTokenWithAdditionalInfo token = tokenService.getToken("deploy-service-user");
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
        Mockito.when(tokenParserChain.parse(any()))
               .thenReturn(mockedToken);
        tokenService.getToken("deploy-service-user");
        OAuth2AccessTokenWithAdditionalInfo token = tokenService.getToken("deploy-service-user");
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
        Mockito.when(accessTokenQuery.username(any()))
               .thenReturn(accessTokenQuery);
        Mockito.when(accessTokenQuery.orderByExpiresAt(OrderDirection.DESCENDING))
               .thenReturn(accessTokenQuery);
        Mockito.when(accessTokenQuery.id(any()))
               .thenReturn(accessTokenQuery);
    }

}
