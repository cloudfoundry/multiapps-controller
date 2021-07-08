package org.cloudfoundry.multiapps.controller.web.util;

import static com.sap.cloudfoundry.client.facade.oauth2.TokenFactory.EXPIRES_AT_KEY;
import static com.sap.cloudfoundry.client.facade.oauth2.TokenFactory.USER_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import org.cloudfoundry.multiapps.controller.core.security.token.parsers.TokenParserChain;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.AccessToken;
import org.cloudfoundry.multiapps.controller.persistence.services.AccessTokenService;
import org.cloudfoundry.multiapps.controller.web.Messages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import com.sap.cloudfoundry.client.facade.oauth2.OAuth2AccessTokenWithAdditionalInfo;
import com.sap.cloudfoundry.client.facade.oauth2.OAuthClient;
import com.sap.cloudfoundry.client.facade.util.RestUtil;

class BasicTokenGeneratorTest {

    private static final String TOKEN_STRING = "c29tZUBmYWtlLmZha2U6cGFzc3dvcmQ=";
    @Mock
    private RestUtil restUtil;
    @Mock
    private ApplicationConfiguration applicationConfiguration;
    @Mock
    private OAuthClient oAuthClient;
    @Mock
    private AccessTokenService accessTokenService;
    @Mock
    private TokenReuser tokenReuser;
    @Mock
    private TokenParserChain tokenParserChain;
    private BasicTokenGenerator basicTokenGenerator;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        basicTokenGenerator = new MockedBasicTokenGenerator(accessTokenService, applicationConfiguration, tokenReuser, tokenParserChain);
        Mockito.when(applicationConfiguration.isBasicAuthEnabled())
               .thenReturn(Boolean.TRUE);
        URL uaa = new URL("https://some-url.some");
        Mockito.when(applicationConfiguration.getControllerUrl())
               .thenReturn(uaa);
        Mockito.when(applicationConfiguration.shouldSkipSslValidation())
               .thenReturn(Boolean.TRUE);
        Mockito.when(restUtil.createOAuthClientByControllerUrl(uaa, Boolean.TRUE))
               .thenReturn(oAuthClient);
    }

    @Test
    void testThrowingExceptionIfBasicIsNotEnabled() {
        Mockito.when(applicationConfiguration.isBasicAuthEnabled())
               .thenReturn(Boolean.FALSE);
        Exception exception = assertThrows(InsufficientAuthenticationException.class, () -> basicTokenGenerator.generate("token"));
        assertEquals("Basic authentication is not enabled, use OAuth2", exception.getMessage());
    }

    @Test
    void testParseTokenNoCurrentOauthToken() {
        OAuth2AccessTokenWithAdditionalInfo mockedToken = Mockito.mock(OAuth2AccessTokenWithAdditionalInfo.class);
        String tokenValue = "token_value";
        OAuth2AccessToken oAuth2AccessToken = Mockito.mock(OAuth2AccessToken.class);
        Mockito.when(oAuth2AccessToken.getTokenValue())
               .thenReturn(tokenValue);
        Mockito.when(mockedToken.getOAuth2AccessToken())
               .thenReturn(oAuth2AccessToken);
        Map<String, Object> additionalInfo = Map.of(EXPIRES_AT_KEY, 1000L, USER_NAME, "user1");
        Mockito.when(mockedToken.getAdditionalInfo())
               .thenReturn(additionalInfo);
        mockOauthClient(mockedToken);
        OAuth2AccessTokenWithAdditionalInfo parsedToken = basicTokenGenerator.generate(TOKEN_STRING);
        assertEquals(mockedToken, parsedToken);
        Mockito.verify(accessTokenService)
               .add(any());
    }

    @Test
    void testParseTokenWithCurrentOauthToken() {
        AccessToken mockedAccessToken = Mockito.mock(AccessToken.class);
        Mockito.when(mockedAccessToken.getValue())
               .thenReturn("token_value".getBytes(StandardCharsets.UTF_8));
        Optional<AccessToken> optionalAccessToken = Optional.of(mockedAccessToken);
        Mockito.when(tokenReuser.getTokenWithExpirationAfter(any(), anyLong()))
               .thenReturn(optionalAccessToken);
        basicTokenGenerator.generate(TOKEN_STRING);
        Mockito.verify(tokenParserChain)
               .parse(any());
    }

    @Test
    void testParseTokenWhenInvalidTokenIsProvided() {
        Exception exception = assertThrows(InternalAuthenticationServiceException.class,
                                           () -> basicTokenGenerator.generate("4rdHFh%2BHYoS8oLdVvbUzEVqB8Lvm7kSPnuwF0AAABYQ%3D"));
        assertEquals("Illegal base64 character 25", exception.getMessage());
    }

    @Test
    void testGetUsernameWithPasswordWhenNormalPasswordIsProvided() {
        String[] usernameWithPassword = basicTokenGenerator.getUsernameWithPassword(TOKEN_STRING);
        assertEquals("some@fake.fake", usernameWithPassword[0]);
        assertEquals("password", usernameWithPassword[1]);
    }

    @Test
    void testGetUsernameWithPasswordWhenPasswordWithColonsIsProvided() {
        String[] usernameWithPassword = basicTokenGenerator.getUsernameWithPassword("ZmFrZTpwYXM6cGFzOnBhcw==");
        assertEquals("fake", usernameWithPassword[0]);
        assertEquals("pas:pas:pas", usernameWithPassword[1]);
    }

    @Test
    void testGetUsernameWithPasswordWhenPasswordWithInvalidToken() {
        Exception exception = assertThrows(InternalAuthenticationServiceException.class,
                                           () -> basicTokenGenerator.getUsernameWithPassword("invalid"));
        assertEquals(Messages.INVALID_AUTHENTICATION_PROVIDED, exception.getMessage());
    }

    private void mockOauthClient(OAuth2AccessTokenWithAdditionalInfo oAuth2AccessTokenWithAdditionalInfo) {
        Mockito.when(oAuthClient.getToken())
               .thenReturn(oAuth2AccessTokenWithAdditionalInfo);
    }

    private class MockedBasicTokenGenerator extends BasicTokenGenerator {

        public MockedBasicTokenGenerator(AccessTokenService accessTokenService, ApplicationConfiguration applicationConfiguration,
                                         TokenReuser tokenReuser, TokenParserChain tokenParserChain) {
            super(accessTokenService, applicationConfiguration, tokenReuser, tokenParserChain);
        }

        @Override
        protected RestUtil createRestUtil() {
            return BasicTokenGeneratorTest.this.restUtil;
        }
    }

}
