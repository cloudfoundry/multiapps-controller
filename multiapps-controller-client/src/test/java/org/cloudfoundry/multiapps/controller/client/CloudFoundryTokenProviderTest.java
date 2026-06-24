package org.cloudfoundry.multiapps.controller.client;

import org.cloudfoundry.multiapps.controller.client.facade.oauth2.OAuth2AccessTokenWithAdditionalInfo;
import org.cloudfoundry.multiapps.controller.client.facade.oauth2.OAuthClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class CloudFoundryTokenProviderTest {

    @Mock
    private OAuthClient oAuthClient;
    @Mock
    private OAuth2AccessTokenWithAdditionalInfo token;

    private CloudFoundryTokenProvider tokenProvider;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        tokenProvider = new CloudFoundryTokenProvider(oAuthClient);
    }

    @Test
    void testGetTokenDelegatesToOAuthClient() {
        Mockito.when(oAuthClient.getToken())
               .thenReturn(token);

        OAuth2AccessTokenWithAdditionalInfo result = tokenProvider.getToken();

        Assertions.assertSame(token, result);
        Mockito.verify(oAuthClient)
               .getToken();
    }
}
