package org.cloudfoundry.multiapps.controller.web.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URL;

import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.web.Messages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;

import com.sap.cloudfoundry.client.facade.oauth2.OAuth2AccessTokenWithAdditionalInfo;
import com.sap.cloudfoundry.client.facade.oauth2.OAuthClient;
import com.sap.cloudfoundry.client.facade.util.RestUtil;

class BasicTokenParsingStrategyTest {

    private static final String TOKEN_STRING = "c29tZUBmYWtlLmZha2U6cGFzc3dvcmQ=";
    @Mock
    private RestUtil restUtil;
    @Mock
    private ApplicationConfiguration applicationConfiguration;
    @Mock
    private OAuthClient oAuthClient;
    @InjectMocks
    private BasicTokenParsingStrategy basicTokenParsingStrategy;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
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
        Exception exception = assertThrows(InsufficientAuthenticationException.class, () -> basicTokenParsingStrategy.parseToken("token"));
        assertEquals("Basic authentication is not enabled, use OAuth2", exception.getMessage());
    }

    @Test
    void testParseToken() {
        OAuth2AccessTokenWithAdditionalInfo mockedToken = Mockito.mock(OAuth2AccessTokenWithAdditionalInfo.class);
        mockOauthClient(mockedToken);
        OAuth2AccessTokenWithAdditionalInfo parsedToken = basicTokenParsingStrategy.parseToken(TOKEN_STRING);
        assertEquals(mockedToken, parsedToken);
    }

    @Test
    void testGetUsernameWithPasswordWhenNormalPasswordIsProvided() {
        String[] usernameWithPassword = basicTokenParsingStrategy.getUsernameWithPassword(TOKEN_STRING);
        assertEquals("some@fake.fake", usernameWithPassword[0]);
        assertEquals("password", usernameWithPassword[1]);
    }

    @Test
    void testGetUsernameWithPasswordWhenPasswordWithColonsIsProvided() {
        String[] usernameWithPassword = basicTokenParsingStrategy.getUsernameWithPassword("ZmFrZTpwYXM6cGFzOnBhcw==");
        assertEquals("fake", usernameWithPassword[0]);
        assertEquals("pas:pas:pas", usernameWithPassword[1]);
    }

    @Test
    void testGetUsernameWithPasswordWhenPasswordWithInvalidToken() {
        Exception exception = assertThrows(InternalAuthenticationServiceException.class,
                                           () -> basicTokenParsingStrategy.getUsernameWithPassword("invalid"));
        assertEquals(Messages.INVALID_AUTHENTICATION_PROVIDED, exception.getMessage());
    }

    private void mockOauthClient(OAuth2AccessTokenWithAdditionalInfo oAuth2AccessTokenWithAdditionalInfo) {
        Mockito.when(oAuthClient.getToken())
               .thenReturn(oAuth2AccessTokenWithAdditionalInfo);
    }

}
