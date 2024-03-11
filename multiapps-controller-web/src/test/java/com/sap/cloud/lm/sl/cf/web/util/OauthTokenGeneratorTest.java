package com.sap.cloud.lm.sl.cf.web.util;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

import org.cloudfoundry.client.lib.oauth2.OAuth2AccessTokenWithAdditionalInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import com.sap.cloud.lm.sl.cf.core.dao.AccessTokenDao;
import com.sap.cloud.lm.sl.cf.core.security.token.TokenParserChain;

class OauthTokenGeneratorTest {

    private static final String TOKEN_STRING = "token";
    @Mock
    private TokenParserChain tokenParserChain;
    @Mock
    private TokenReuser tokenReuser;
    @Mock
    private AccessTokenDao accessTokenDao;

    private OauthTokenGenerator oauthTokenGenerator;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        oauthTokenGenerator = new OauthTokenGenerator(accessTokenDao, tokenParserChain, tokenReuser);
    }

    @Test
    void testGenerateAccessTokenWithNoTokensInDb() {
        mockTokenParserChain();
        when(tokenReuser.getValidTokenWithExpirationAfterIfPresent(any(), anyLong())).thenReturn(Optional.empty());
        oauthTokenGenerator.generate(TOKEN_STRING);
        verify(accessTokenDao).add(any());
    }

    @Test
    void testGenerateAccessTokenWithAvailableTokenInDb() {
        mockTokenParserChain();
        OAuth2AccessTokenWithAdditionalInfo storedToken = getMockedToken();
        when(tokenReuser.getValidTokenWithExpirationAfterIfPresent(any(), anyLong())).thenReturn(Optional.of(storedToken));
        oauthTokenGenerator.generate(TOKEN_STRING);
        verify(accessTokenDao, never()).add(any());
    }

    @Test
    void testGenerateAccessTokenProvidingInvalid() {
        OAuth2AccessTokenWithAdditionalInfo token = getMockedToken();
        when(tokenParserChain.parse(anyString())).thenReturn(token);
        when(token.expiresBefore(any())).thenReturn(true);
        assertThrows(ResponseStatusException.class, () -> oauthTokenGenerator.generate(TOKEN_STRING));
    }

    private void mockTokenParserChain() {
        OAuth2AccessTokenWithAdditionalInfo tokenWithAdditionalInfo = getMockedToken();
        when(tokenParserChain.parse(anyString())).thenReturn(tokenWithAdditionalInfo);
    }

    private OAuth2AccessTokenWithAdditionalInfo getMockedToken() {
        OAuth2AccessTokenWithAdditionalInfo tokenWithAdditionalInfo = mock(OAuth2AccessTokenWithAdditionalInfo.class);
        when(tokenWithAdditionalInfo.getExpiresAt()).thenReturn(Instant.now()
                                                                       .plusSeconds(600));
        when(tokenWithAdditionalInfo.getValue()).thenReturn("some-value");
        when(tokenWithAdditionalInfo.getDefaultValue()).thenReturn("some-default-value");
        when(tokenWithAdditionalInfo.getUserName()).thenReturn("XSA_ADMIN");
        when(tokenWithAdditionalInfo.calculateExpirationDate()).thenReturn(LocalDateTime.now());
        return tokenWithAdditionalInfo;
    }

}
