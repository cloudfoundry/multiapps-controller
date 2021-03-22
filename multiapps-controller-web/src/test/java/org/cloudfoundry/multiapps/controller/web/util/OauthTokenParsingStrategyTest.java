package org.cloudfoundry.multiapps.controller.web.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.cloudfoundry.multiapps.controller.core.security.token.parsers.TokenParserChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.sap.cloudfoundry.client.facade.oauth2.OAuth2AccessTokenWithAdditionalInfo;

class OauthTokenParsingStrategyTest {

    private static final String TOKEN_STRING = "token";
    @Mock
    private TokenParserChain tokenParserChain;
    @InjectMocks
    private OauthTokenParsingStrategy oauthTokenParsingStrategy;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
    }

    @Test
    void testParseToken() {
        OAuth2AccessTokenWithAdditionalInfo mockedToken = Mockito.mock(OAuth2AccessTokenWithAdditionalInfo.class);
        mockTokenParserChain(mockedToken);
        OAuth2AccessTokenWithAdditionalInfo token = oauthTokenParsingStrategy.parseToken(TOKEN_STRING);
        assertEquals(mockedToken, token);
    }

    private void mockTokenParserChain(OAuth2AccessTokenWithAdditionalInfo token) {
        Mockito.when(tokenParserChain.parse(TOKEN_STRING))
               .thenReturn(token);
    }

}
