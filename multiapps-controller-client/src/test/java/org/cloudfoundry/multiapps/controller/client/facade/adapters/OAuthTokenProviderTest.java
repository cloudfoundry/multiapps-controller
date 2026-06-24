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

import reactor.core.publisher.Mono;

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
    void testGetTokenIsLazyAndDoesNotInvokeOAuthClientUntilSubscribed() {
        Mockito.when(oAuthClient.getToken())
               .thenReturn(token);
        Mockito.when(token.getAuthorizationHeaderValue())
               .thenReturn("Bearer abc123");

        Mono<String> mono = provider.getToken(connectionContext);

        Mockito.verify(oAuthClient, Mockito.never())
               .getToken();

        String result = mono.block();

        Assertions.assertEquals("Bearer abc123", result);
        Mockito.verify(oAuthClient)
               .getToken();
    }

    @Test
    void testGetTokenInvokesOAuthClientOncePerSubscription() {
        Mockito.when(oAuthClient.getToken())
               .thenReturn(token);
        Mockito.when(token.getAuthorizationHeaderValue())
               .thenReturn("Bearer abc123");

        Mono<String> mono = provider.getToken(connectionContext);

        mono.block();
        mono.block();

        Mockito.verify(oAuthClient, Mockito.times(2))
               .getToken();
    }

    @Test
    void testGetTokenPropagatesOAuthClientFailure() {
        RuntimeException failure = new RuntimeException("token retrieval failed");
        Mockito.when(oAuthClient.getToken())
               .thenThrow(failure);

        Mono<String> mono = provider.getToken(connectionContext);

        RuntimeException thrown = Assertions.assertThrows(RuntimeException.class, mono::block);
        Assertions.assertSame(failure, thrown);
    }
}
