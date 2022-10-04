package com.sap.cloud.lm.sl.cf.core.security.token;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Matchers.any;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;

import org.cloudfoundry.client.lib.oauth2.OAuth2AccessTokenWithAdditionalInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import com.sap.cloud.lm.sl.cf.core.cf.service.TokenService;
import com.sap.cloud.lm.sl.cf.core.dao.AccessTokenDao;
import com.sap.cloud.lm.sl.cf.core.model.AccessToken;

class TokenServiceTest {

    @Mock
    private AccessTokenDao accessTokenDao;
    @Mock
    private TokenParserChain tokenParserChain;
    @InjectMocks
    private TokenService tokenService;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void testGetTokenWhenThereAreNoTokensForUser() {
        Mockito.when(accessTokenDao.getTokensByUsernameSortedByExpirationDate(any(), any()))
               .thenReturn(Collections.emptyList());
        Exception exception = assertThrows(IllegalStateException.class, () -> tokenService.getToken("deploy-service-user"));
        assertEquals("No valid access token was found for user \"deploy-service-user\"", exception.getMessage());
    }

    @Test
    void testGetTokenWhenThereIsOnlyOneTokenInDb() {
        AccessToken accessToken = Mockito.mock(AccessToken.class);
        Mockito.when(accessToken.getValue())
               .thenReturn(new byte[0]);
        Mockito.when(accessTokenDao.getTokensByUsernameSortedByExpirationDate(any(), any()))
               .thenReturn(Arrays.asList(accessToken));
        OAuth2AccessTokenWithAdditionalInfo mockedToken = Mockito.mock(OAuth2AccessTokenWithAdditionalInfo.class);
        Mockito.when(tokenParserChain.parse(any()))
               .thenReturn(mockedToken);
        OAuth2AccessTokenWithAdditionalInfo token = tokenService.getToken("deploy-service-user");
        assertEquals(mockedToken, token);
    }

    @Test
    void testGetTokenWhenThereIsNewerTokenInDb() {
        AccessToken accessToken = Mockito.mock(AccessToken.class);
        Mockito.when(accessToken.getValue())
               .thenReturn(new byte[0]);
        Mockito.when(accessTokenDao.getTokensByUsernameSortedByExpirationDate(any(), any()))
               .thenReturn(Arrays.asList(accessToken));
        OAuth2AccessTokenWithAdditionalInfo mockedToken = Mockito.mock(OAuth2AccessTokenWithAdditionalInfo.class);
        Mockito.when(tokenParserChain.parse(any()))
               .thenReturn(mockedToken);
        OAuth2AccessTokenWithAdditionalInfo token = tokenService.getToken("deploy-service-user");
        assertEquals(mockedToken, token);
    }

    @Test
    void testGetTokenWhenCachedIsAvailable() {
        AccessToken accessToken = Mockito.mock(AccessToken.class);
        Mockito.when(accessToken.getValue())
               .thenReturn(new byte[0]);
        Mockito.when(accessTokenDao.getTokensByUsernameSortedByExpirationDate(any(), any()))
               .thenReturn(Arrays.asList(accessToken));
        OAuth2AccessToken oAuth2AccessToken = Mockito.mock(OAuth2AccessToken.class);
        Mockito.when(oAuth2AccessToken.getExpiresAt())
               .thenReturn(Instant.now()
                                  .plus(90, ChronoUnit.SECONDS));
        OAuth2AccessTokenWithAdditionalInfo mockedToken = new OAuth2AccessTokenWithAdditionalInfo(oAuth2AccessToken,
                                                                                                  Collections.emptyMap());
        Mockito.when(tokenParserChain.parse(any()))
               .thenReturn(mockedToken);
        tokenService.getToken("deploy-service-user");
        OAuth2AccessTokenWithAdditionalInfo token = tokenService.getToken("deploy-service-user");
        assertEquals(mockedToken, token);
    }

}
