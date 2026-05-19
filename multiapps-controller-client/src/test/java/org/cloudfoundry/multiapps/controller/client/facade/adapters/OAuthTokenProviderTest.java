package org.cloudfoundry.multiapps.controller.client.facade.adapters;

import org.cloudfoundry.multiapps.controller.client.facade.oauth2.OAuth2AccessTokenWithAdditionalInfo;
import org.cloudfoundry.multiapps.controller.client.facade.oauth2.OAuthClient;
import org.cloudfoundry.reactor.ConnectionContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class OAuthTokenProviderTest {

    @Mock
    private OAuthClient oAuthClient;
    @Mock
    private OAuth2AccessTokenWithAdditionalInfo token;
    @Mock
    private ConnectionContext connectionContext;

    private OAuthTokenProvider provider;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        provider = new OAuthTokenProvider(oAuthClient);
    }

    @Test
    void testGetTokenReturnsAuthorizationHeaderValueFromOAuthClient() {
        Mockito.when(oAuthClient.getToken())
               .thenReturn(token);
        Mockito.when(token.getAuthorizationHeaderValue())
               .thenReturn("Bearer abc123");

        String result = provider.getToken(connectionContext)
                                .block();

        Assertions.assertEquals("Bearer abc123", result);
        Mockito.verify(oAuthClient)
               .getToken();
    }
}
