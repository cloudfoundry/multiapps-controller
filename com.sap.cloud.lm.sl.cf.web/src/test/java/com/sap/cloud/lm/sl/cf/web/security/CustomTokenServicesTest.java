package com.sap.cloud.lm.sl.cf.web.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.TokenStore;

import com.sap.cloud.lm.sl.cf.client.util.TokenProperties;
import com.sap.cloud.lm.sl.cf.core.auditlogging.AuditLoggingFacade;
import com.sap.cloud.lm.sl.cf.core.auditlogging.AuditLoggingProvider;
import com.sap.cloud.lm.sl.cf.core.security.token.TokenParserChain;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.core.util.SecurityUtil;

public class CustomTokenServicesTest {

    private static final String DUMMY_TOKEN_STRING = "dummyTokenString";
    private static final String DEFAULT_SCOPE = "default";
    private static final String TEST_CLIENT_ID = "testClientId";
    private static final String USER_NAME = "testUser";
    private static final String USER_ID = "1";

    @Mock
    private TokenStore tokenStore;
    @Mock
    private TokenParserChain tokenParserChain;
    @Mock
    private ApplicationConfiguration configuration;
    @Mock
    private AuditLoggingFacade auditLoggingFacade;

    private CustomTokenServices customTokenServices;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        AuditLoggingProvider.setFacade(auditLoggingFacade);
        customTokenServices = new CustomTokenServices(tokenStore, configuration, tokenParserChain);
    }

    @Test
    public void testGetTokenFromCache() {
        OAuth2AccessToken token = buildValidToken();
        OAuth2Authentication auth = buildAuthentication(token);
        prepareTokenParserChain(token);
        prepareTokenStore(token, auth);

        OAuth2Authentication loadedAuthentication = customTokenServices.loadAuthentication(DUMMY_TOKEN_STRING);

        assertEquals(auth.getAuthorities(), loadedAuthentication.getAuthorities());
        assertEquals(auth.getCredentials(), loadedAuthentication.getCredentials());
        assertEquals(auth.getOAuth2Request(), loadedAuthentication.getOAuth2Request());
    }

    private OAuth2AccessToken buildValidToken() {
        long expirationTime = LocalDateTime.now()
                                           .plusMinutes(10)
                                           .atZone(ZoneId.systemDefault())
                                           .toEpochSecond();
        Map<String, Object> additionalTokenProperties = buildAdditionalTokenProperties();
        return buildToken(DUMMY_TOKEN_STRING, new Date(TimeUnit.SECONDS.toMillis(expirationTime)), additionalTokenProperties);
    }

    private OAuth2AccessToken buildToken(String tokenString, Date expiration, Map<String, Object> additionalTokenProperties) {
        DefaultOAuth2AccessToken token = new DefaultOAuth2AccessToken(tokenString);
        token.setExpiration(expiration);
        token.setScope(Collections.singleton(DEFAULT_SCOPE));
        token.setAdditionalInformation(additionalTokenProperties);
        return token;
    }

    private Map<String, Object> buildAdditionalTokenProperties() {
        Map<String, Object> additionalTokenProperties = new HashMap<>();
        additionalTokenProperties.put(TokenProperties.CLIENT_ID_KEY, TEST_CLIENT_ID);
        additionalTokenProperties.put(TokenProperties.USER_ID_KEY, USER_ID);
        additionalTokenProperties.put(TokenProperties.USER_NAME_KEY, USER_NAME);
        return additionalTokenProperties;
    }

    private OAuth2Authentication buildAuthentication(OAuth2AccessToken token) {
        TokenProperties tokenProperties = TokenProperties.fromToken(token);
        return SecurityUtil.createAuthentication(tokenProperties.getClientId(), token.getScope(), SecurityUtil.getTokenUserInfo(token));
    }

    private void prepareTokenParserChain(OAuth2AccessToken token) {
        when(tokenParserChain.parse(anyString())).thenReturn(token);
    }

    private void prepareTokenStore(OAuth2AccessToken token, OAuth2Authentication auth) {
        when(tokenStore.readAccessToken(DUMMY_TOKEN_STRING)).thenReturn(token);
        when(tokenStore.readAuthentication(token)).thenReturn(auth);
    }

    @Test
    public void testHandleExceptionForInvalidToken() {
        assertThrows(InvalidTokenException.class, () -> customTokenServices.loadAuthentication(DUMMY_TOKEN_STRING));
    }

    @Test
    public void testWithExpiredToken() {
        OAuth2AccessToken token = buildInvalidToken();

        prepareTokenParserChain(token);

        assertThrows(InvalidTokenException.class, () -> customTokenServices.loadAuthentication(DUMMY_TOKEN_STRING));
        verify(tokenStore).removeAccessToken(eq(token));
    }

    private OAuth2AccessToken buildInvalidToken() {
        long expirationTime = LocalDateTime.now()
                                           .minusMinutes(10)
                                           .atZone(ZoneId.systemDefault())
                                           .toEpochSecond();
        return buildToken(DUMMY_TOKEN_STRING, new Date(TimeUnit.SECONDS.toMillis(expirationTime)), Collections.emptyMap());
    }

    @Test
    public void testPersistTokenInCache() {
        OAuth2AccessToken token = buildValidToken();
        prepareTokenParserChain(token);

        OAuth2Authentication auth = customTokenServices.loadAuthentication(DUMMY_TOKEN_STRING);

        assertNotNull(auth.getAuthorities());
        assertNotNull(auth.getCredentials());
        assertNotNull(auth.getOAuth2Request());
        verify(tokenStore).storeAccessToken(token, auth);
    }

}
