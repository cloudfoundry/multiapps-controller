package com.sap.cloud.lm.sl.cf.web.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.cloudfoundry.client.lib.oauth2.OAuth2AccessTokenWithAdditionalInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.InternalAuthenticationServiceException;

import com.sap.cloud.lm.sl.cf.core.Constants;
import com.sap.cloud.lm.sl.cf.core.dao.AccessTokenDao;
import com.sap.cloud.lm.sl.cf.core.dao.filters.OrderDirection;
import com.sap.cloud.lm.sl.cf.core.model.AccessToken;
import com.sap.cloud.lm.sl.cf.core.security.token.TokenParserChain;
import com.sap.cloud.lm.sl.cf.core.util.SingleThreadExecutor;

class TokenReuserTest {

    private static final String ADMIN_USERNAME = "XSA_ADMIN";

    @Mock
    private AccessTokenDao accessTokenDao;
    @Mock
    private TokenParserChain tokenParserChain;
    @Mock
    private SingleThreadExecutor singleThreadExecutor;
    private TokenReuser tokenReuser;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        tokenReuser = new TokenReuser(accessTokenDao, tokenParserChain, singleThreadExecutor);
    }

    @Test
    void testGetValidTokenWhenNoTokensAreStored() {
        assertFalse(tokenReuser.getValidTokenWithExpirationAfterIfPresent(ADMIN_USERNAME, Constants.OAUTH_TOKEN_RETENTION_TIME_IN_SECONDS)
                               .isPresent());
    }

    @Test
    void testGetValidTokenWhenOnlyExpiredTokensAreStored() {
        AccessToken accessToken = mock(AccessToken.class);
        when(accessToken.getExpiresAt()).thenReturn(LocalDateTime.now()
                                                                 .minusSeconds(600));
        when(accessTokenDao.getTokensByUsernameSortedByExpirationDate(ADMIN_USERNAME,
                                                                      OrderDirection.DESCENDING)).thenReturn(Collections.singletonList(accessToken));
        assertFalse(tokenReuser.getValidTokenWithExpirationAfterIfPresent(ADMIN_USERNAME, Constants.OAUTH_TOKEN_RETENTION_TIME_IN_SECONDS)
                               .isPresent());
    }

    @Test
    void testGetValidTokenWhenOnlyRevokedTokensAreStored() {
        AccessToken accessToken = mock(AccessToken.class);
        when(accessToken.getExpiresAt()).thenReturn(LocalDateTime.now()
                                                                 .plusSeconds(600));
        when(accessToken.getValue()).thenReturn("token-value".getBytes());
        when(accessTokenDao.getTokensByUsernameSortedByExpirationDate(ADMIN_USERNAME,
                                                                      OrderDirection.DESCENDING)).thenReturn(Collections.singletonList(accessToken));
        when(tokenParserChain.parse(any())).thenThrow(new InternalAuthenticationServiceException("Token was revoked!"));
        assertFalse(tokenReuser.getValidTokenWithExpirationAfterIfPresent(ADMIN_USERNAME, Constants.OAUTH_TOKEN_RETENTION_TIME_IN_SECONDS)
                               .isPresent());
        verify(singleThreadExecutor).submitTask(any());
    }

    @Test
    void testGetValidTokenWhenValidTokensAreStored() {
        AccessToken accessToken = mock(AccessToken.class);
        when(accessToken.getExpiresAt()).thenReturn(LocalDateTime.now()
                                                                 .plusSeconds(600));
        when(accessToken.getValue()).thenReturn("token-value".getBytes());
        when(accessTokenDao.getTokensByUsernameSortedByExpirationDate(ADMIN_USERNAME,
                                                                      OrderDirection.DESCENDING)).thenReturn(Collections.singletonList(accessToken));
        OAuth2AccessTokenWithAdditionalInfo expectedToken = mock(OAuth2AccessTokenWithAdditionalInfo.class);
        when(expectedToken.getUserName()).thenReturn(ADMIN_USERNAME);
        when(tokenParserChain.parse(any())).thenReturn(expectedToken);
        Optional<OAuth2AccessTokenWithAdditionalInfo> actualToken = tokenReuser.getValidTokenWithExpirationAfterIfPresent(ADMIN_USERNAME,
                                                                                                                          Constants.OAUTH_TOKEN_RETENTION_TIME_IN_SECONDS);
        assertTrue(actualToken.isPresent());
        assertEquals(expectedToken, actualToken.get());
    }

    @Test
    void testGetValidTokenWhenTwoTokensAreStoredOnlyOneIsValid() {
        AccessToken revokedAccessToken = mock(AccessToken.class);
        when(revokedAccessToken.getExpiresAt()).thenReturn(LocalDateTime.now()
                                                                        .plusSeconds(600));
        when(revokedAccessToken.getValue()).thenReturn("revoked-value".getBytes());
        AccessToken validAccessToken = mock(AccessToken.class);
        when(validAccessToken.getExpiresAt()).thenReturn(LocalDateTime.now()
                                                                      .plusSeconds(600));
        when(validAccessToken.getValue()).thenReturn("valid-value".getBytes());
        when(accessTokenDao.getTokensByUsernameSortedByExpirationDate(ADMIN_USERNAME,
                                                                      OrderDirection.DESCENDING)).thenReturn(Arrays.asList(revokedAccessToken,
                                                                                                                           validAccessToken));
        OAuth2AccessTokenWithAdditionalInfo expectedToken = mock(OAuth2AccessTokenWithAdditionalInfo.class);
        when(expectedToken.getUserName()).thenReturn(ADMIN_USERNAME);
        when(tokenParserChain.parse(any())).thenThrow(new InternalAuthenticationServiceException("Token was revoked!"))
                                           .thenReturn(expectedToken);
        Optional<OAuth2AccessTokenWithAdditionalInfo> actualToken = tokenReuser.getValidTokenWithExpirationAfterIfPresent(ADMIN_USERNAME,
                                                                                                                          Constants.OAUTH_TOKEN_RETENTION_TIME_IN_SECONDS);
        assertTrue(actualToken.isPresent());
        assertEquals(expectedToken, actualToken.get());
    }
}
