package com.sap.cloud.lm.sl.cf.core.cf.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.cloudfoundry.client.lib.oauth2.OAuth2AccessTokenWithAdditionalInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.InternalAuthenticationServiceException;

import com.sap.cloud.lm.sl.cf.core.dao.AccessTokenDao;
import com.sap.cloud.lm.sl.cf.core.dao.filters.OrderDirection;
import com.sap.cloud.lm.sl.cf.core.model.AccessToken;
import com.sap.cloud.lm.sl.cf.core.security.token.TokenParserChain;
import com.sap.cloud.lm.sl.cf.core.util.SingleThreadExecutor;

class TokenServiceTest {

    private static final String ADMIN_USERNAME = "XSA_ADMIN";

    @Mock
    private AccessTokenDao accessTokenDao;
    @Mock
    private TokenParserChain tokenParserChain;
    @Mock
    private SingleThreadExecutor singleThreadExecutor;
    private TokenService tokenService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        tokenService = new TokenServiceMock(accessTokenDao, tokenParserChain, singleThreadExecutor);
    }

    @Test
    void testGetTokenWhenTokenIsCached() throws NoSuchFieldException, IllegalAccessException {
        Map<String, OAuth2AccessTokenWithAdditionalInfo> cachedOauth2AccessTokens = getCachedTokensMap();
        OAuth2AccessTokenWithAdditionalInfo oAuth2AccessTokenWithAdditionalInfo = buildTokenWithAdditionalInfo(false);
        cachedOauth2AccessTokens.put(ADMIN_USERNAME, oAuth2AccessTokenWithAdditionalInfo);
        assertEquals(oAuth2AccessTokenWithAdditionalInfo, tokenService.getToken(ADMIN_USERNAME));
    }

    @Test
    void testGetTokenWhenTheCachedTokenIsExpired() throws NoSuchFieldException, IllegalAccessException {
        Map<String, OAuth2AccessTokenWithAdditionalInfo> cachedOauth2AccessTokens = getCachedTokensMap();
        OAuth2AccessTokenWithAdditionalInfo oAuth2AccessTokenWithAdditionalInfo = buildTokenWithAdditionalInfo(true);
        cachedOauth2AccessTokens.put(ADMIN_USERNAME, oAuth2AccessTokenWithAdditionalInfo);
        assertThrows(IllegalStateException.class, () -> tokenService.getToken(ADMIN_USERNAME));
    }

    @Test
    void testGetTokenWhenTheCachedTokenIsNotExpiredButRevoked() throws NoSuchFieldException, IllegalAccessException {
        Map<String, OAuth2AccessTokenWithAdditionalInfo> cachedOauth2AccessTokens = getCachedTokensMap();
        OAuth2AccessTokenWithAdditionalInfo oAuth2AccessTokenWithAdditionalInfo = buildTokenWithAdditionalInfo(false);
        cachedOauth2AccessTokens.put(ADMIN_USERNAME, oAuth2AccessTokenWithAdditionalInfo);
        when(tokenParserChain.parse(any())).thenThrow(new InternalAuthenticationServiceException("Token was revoked!"));
        assertThrows(IllegalStateException.class, () -> tokenService.getToken(ADMIN_USERNAME));

    }

    private Map<String, OAuth2AccessTokenWithAdditionalInfo> getCachedTokensMap() throws NoSuchFieldException, IllegalAccessException {
        Field cachedOauth2AccessTokensField = tokenService.getClass()
                                                          .getSuperclass()
                                                          .getDeclaredField("cachedOauth2AccessTokens");
        cachedOauth2AccessTokensField.setAccessible(true);
        return (Map<String, OAuth2AccessTokenWithAdditionalInfo>) cachedOauth2AccessTokensField.get(tokenService);
    }

    @Test
    void testGetTokenWhenTwoTokensAreInStored() {
        AccessToken firstToken = buildAccessToken(1L, LocalDateTime.now()
                                                                   .plusSeconds(100L));
        AccessToken secondToken = buildAccessToken(2L, LocalDateTime.now()
                                                                    .plusSeconds(1000L));
        OAuth2AccessTokenWithAdditionalInfo expectedToken = mock(OAuth2AccessTokenWithAdditionalInfo.class);
        when(tokenParserChain.parse(any())).thenReturn(expectedToken);
        when(accessTokenDao.getTokensByUsernameSortedByExpirationDate(ADMIN_USERNAME,
                                                                      OrderDirection.DESCENDING)).thenReturn(Arrays.asList(firstToken,
                                                                                                                           secondToken));
        OAuth2AccessTokenWithAdditionalInfo actualToken = tokenService.getToken(ADMIN_USERNAME);
        assertEquals(expectedToken, actualToken);
    }

    @Test
    void testGetTokenWhenThereIsOneInvalidTokenStored() {
        AccessToken expiredToken = buildAccessToken(1L, LocalDateTime.now()
                                                                     .minusSeconds(100L));
        when(tokenParserChain.parse(any())).thenThrow(new InternalAuthenticationServiceException("Token is expired!"));
        when(accessTokenDao.getTokensByUsernameSortedByExpirationDate(ADMIN_USERNAME,
                                                                      OrderDirection.DESCENDING)).thenReturn(Collections.singletonList(expiredToken));
        assertThrows(IllegalStateException.class, () -> tokenService.getToken(ADMIN_USERNAME));
    }

    @Test
    void testGetTokenWhenThereAreNoStoredTokens() {
        assertThrows(IllegalStateException.class, () -> tokenService.getToken(ADMIN_USERNAME));
    }

    private OAuth2AccessTokenWithAdditionalInfo buildTokenWithAdditionalInfo(boolean isExpired) {
        OAuth2AccessTokenWithAdditionalInfo oAuth2AccessTokenWithAdditionalInfo = mock(OAuth2AccessTokenWithAdditionalInfo.class);
        when(oAuth2AccessTokenWithAdditionalInfo.getValue()).thenReturn("some-value");
        when(oAuth2AccessTokenWithAdditionalInfo.expiresBefore(any())).thenReturn(isExpired);
        when(oAuth2AccessTokenWithAdditionalInfo.getUserName()).thenReturn(ADMIN_USERNAME);
        return oAuth2AccessTokenWithAdditionalInfo;
    }

    private AccessToken buildAccessToken(long id, LocalDateTime expirationDate) {
        AccessToken accessToken = mock(AccessToken.class);
        when(accessToken.getId()).thenReturn(id);
        when(accessToken.getUsername()).thenReturn(ADMIN_USERNAME);
        when(accessToken.getExpiresAt()).thenReturn(expirationDate);
        when(accessToken.getValue()).thenReturn("token-value".getBytes());
        return accessToken;
    }

    private static class TokenServiceMock extends TokenService {

        public TokenServiceMock(AccessTokenDao accessTokenDao, TokenParserChain tokenParserChain,
                                SingleThreadExecutor singleThreadExecutor) {
            super(accessTokenDao, tokenParserChain, singleThreadExecutor);
        }

        @Override
        protected void sleep(long millis) {
            // do nothing
        }

        @Override
        protected Map<String, OAuth2AccessTokenWithAdditionalInfo> createAccessTokenCache() {
            return new HashMap<>();
        }
    }

}
