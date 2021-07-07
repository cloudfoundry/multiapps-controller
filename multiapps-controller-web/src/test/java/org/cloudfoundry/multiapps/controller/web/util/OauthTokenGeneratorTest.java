package org.cloudfoundry.multiapps.controller.web.util;

import static com.sap.cloudfoundry.client.facade.oauth2.TokenFactory.EXPIRES_AT_KEY;
import static com.sap.cloudfoundry.client.facade.oauth2.TokenFactory.USER_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import org.cloudfoundry.multiapps.controller.core.security.token.parsers.TokenParserChain;
import org.cloudfoundry.multiapps.controller.persistence.model.AccessToken;
import org.cloudfoundry.multiapps.controller.persistence.services.AccessTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.web.server.ResponseStatusException;

import com.sap.cloudfoundry.client.facade.oauth2.OAuth2AccessTokenWithAdditionalInfo;

class OauthTokenGeneratorTest {

    private static final String TOKEN_STRING = "token";
    @Mock
    private TokenParserChain tokenParserChain;
    @Mock
    private TokenReuser tokenReuser;
    @Mock
    private AccessTokenService accessTokenService;
    @InjectMocks
    private OauthTokenGenerator oauthTokenParsingStrategy;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
    }

    @Test
    void testParseToken() {
        OAuth2AccessTokenWithAdditionalInfo mockedToken = Mockito.mock(OAuth2AccessTokenWithAdditionalInfo.class);
        OAuth2AccessToken oAuth2AccessToken = Mockito.mock(OAuth2AccessToken.class);
        Mockito.when(oAuth2AccessToken.getExpiresAt())
               .thenReturn(Instant.now()
                                  .plus(Duration.ofMinutes(2)));
        Mockito.when(oAuth2AccessToken.getTokenValue())
               .thenReturn("token_value");
        Mockito.when(mockedToken.getOAuth2AccessToken())
               .thenReturn(oAuth2AccessToken);
        Map<String, Object> additionalInfo = Map.of(EXPIRES_AT_KEY, 1000L, USER_NAME, "user1");
        Mockito.when(mockedToken.getAdditionalInfo())
               .thenReturn(additionalInfo);
        mockTokenParserChain(mockedToken);
        OAuth2AccessTokenWithAdditionalInfo token = oauthTokenParsingStrategy.generate(TOKEN_STRING);
        assertEquals(mockedToken, token);
        Mockito.verify(accessTokenService)
               .add(any());
    }

    @Test
    void testParseTokenWithExpiredToken() {
        OAuth2AccessTokenWithAdditionalInfo mockedToken = Mockito.mock(OAuth2AccessTokenWithAdditionalInfo.class);
        OAuth2AccessToken oAuth2AccessToken = Mockito.mock(OAuth2AccessToken.class);
        Mockito.when(oAuth2AccessToken.getExpiresAt())
               .thenReturn(Instant.now()
                                  .minus(Duration.ofMinutes(2)));
        Mockito.when(mockedToken.getOAuth2AccessToken())
               .thenReturn(oAuth2AccessToken);
        mockTokenParserChain(mockedToken);
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                                                         () -> oauthTokenParsingStrategy.generate(TOKEN_STRING));
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }

    @Test
    void testParseTokenWithAlreadyValidToken() {
        OAuth2AccessTokenWithAdditionalInfo mockedToken = Mockito.mock(OAuth2AccessTokenWithAdditionalInfo.class);
        OAuth2AccessToken oAuth2AccessToken = Mockito.mock(OAuth2AccessToken.class);
        Mockito.when(oAuth2AccessToken.getExpiresAt())
               .thenReturn(Instant.now()
                                  .plus(Duration.ofMinutes(2)));
        Mockito.when(oAuth2AccessToken.getTokenValue())
               .thenReturn("token_value");
        Mockito.when(mockedToken.getOAuth2AccessToken())
               .thenReturn(oAuth2AccessToken);
        mockTokenParserChain(mockedToken);
        AccessToken mockedAccessToken = Mockito.mock(AccessToken.class);
        Mockito.when(mockedAccessToken.getValue())
               .thenReturn("token_value".getBytes(StandardCharsets.UTF_8));
        Optional<AccessToken> optionalAccessToken = Optional.of(mockedAccessToken);
        Mockito.when(tokenReuser.getTokenWithExpirationAfterOrReuseCurrent(any(), anyInt(), any()))
               .thenReturn(optionalAccessToken);
        oauthTokenParsingStrategy.generate(TOKEN_STRING);
        Mockito.verify(tokenParserChain, times(2))
               .parse(any());
    }

    private void mockTokenParserChain(OAuth2AccessTokenWithAdditionalInfo token) {
        Mockito.when(tokenParserChain.parse(TOKEN_STRING))
               .thenReturn(token);
    }

}
