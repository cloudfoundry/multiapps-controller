package com.sap.cloud.lm.sl.cf.web.util;

import static com.sap.cloud.lm.sl.cf.client.util.TokenFactory.EXPIRES_AT_KEY;
import static com.sap.cloud.lm.sl.cf.client.util.TokenProperties.USER_NAME_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.cloudfoundry.client.lib.oauth2.OAuth2AccessTokenWithAdditionalInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.sap.cloud.lm.sl.cf.core.Constants;
import com.sap.cloud.lm.sl.cf.core.dao.AccessTokenDao;
import com.sap.cloud.lm.sl.cf.core.model.AccessToken;
import com.sap.cloud.lm.sl.cf.core.security.token.TokenParserChain;

class OauthTokenGeneratorTest {

    private static final String TOKEN_STRING = "token";
    @Mock
    private TokenParserChain tokenParserChain;
    @Mock
    private TokenReuser tokenReuser;
    @Mock
    private AccessTokenDao accessTokenDao;

    private OauthTokenGenerator oauthTokenParsingStrategy;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        oauthTokenParsingStrategy = new OauthTokenGenerator(accessTokenDao, tokenParserChain, tokenReuser);
    }

    @Test
    void testParseToken() {
        OAuth2AccessTokenWithAdditionalInfo mockedToken = Mockito.mock(OAuth2AccessTokenWithAdditionalInfo.class);
        when(mockedToken.getExpiresAt()).thenReturn(Instant.now()
                                                           .plus(Duration.ofMinutes(2)));
        when(mockedToken.getDefaultValue()).thenReturn("token_value");
        Map<String, Object> additionalInfo = new HashMap<>();
        additionalInfo.put(EXPIRES_AT_KEY, 1000L);
        additionalInfo.put(USER_NAME_KEY, "user1");
        when(mockedToken.getAdditionalInfo()).thenReturn(additionalInfo);
        mockTokenParserChain(mockedToken);
        when(tokenReuser.getTokenWithExpirationAfterOrReuseCurrent("user1", Constants.OAUTH_TOKEN_RETENTION_TIME_IN_SECONDS,
                                                                   mockedToken)).thenReturn(Optional.empty());
        OAuth2AccessTokenWithAdditionalInfo token = oauthTokenParsingStrategy.generate(TOKEN_STRING);
        assertEquals(mockedToken, token);
        Mockito.verify(accessTokenDao)
               .add(any());
    }

    @Test
    void testParseTokenWithExpiredToken() {
        OAuth2AccessTokenWithAdditionalInfo mockedToken = Mockito.mock(OAuth2AccessTokenWithAdditionalInfo.class);
        Mockito.when(mockedToken.getExpiresAt())
               .thenReturn(Instant.now()
                                  .minus(Duration.ofMinutes(2)));
        mockTokenParserChain(mockedToken);
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                                                         () -> oauthTokenParsingStrategy.generate(TOKEN_STRING));
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }

    @Test
    void testParseTokenWithAlreadyValidToken() {
        OAuth2AccessTokenWithAdditionalInfo mockedToken = Mockito.mock(OAuth2AccessTokenWithAdditionalInfo.class);
        Mockito.when(mockedToken.getExpiresAt())
               .thenReturn(Instant.now()
                                  .plus(Duration.ofMinutes(2)));
        Mockito.when(mockedToken.getDefaultValue())
               .thenReturn("token_value");
        mockTokenParserChain(mockedToken);
        AccessToken mockedAccessToken = Mockito.mock(AccessToken.class);
        Mockito.when(mockedAccessToken.getValue())
               .thenReturn("token_value".getBytes(StandardCharsets.UTF_8));
        Optional<AccessToken> optionalAccessToken = Optional.of(mockedAccessToken);
        Mockito.when(tokenReuser.getTokenWithExpirationAfterOrReuseCurrent(any(), anyLong(), any()))
               .thenReturn(optionalAccessToken);
        oauthTokenParsingStrategy.generate(TOKEN_STRING);
        Mockito.verify(tokenParserChain, times(2))
               .parse(any());
    }

    private void mockTokenParserChain(OAuth2AccessTokenWithAdditionalInfo token) {
        when(tokenParserChain.parse(TOKEN_STRING)).thenReturn(token);
    }

}
