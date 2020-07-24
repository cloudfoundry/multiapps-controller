package com.sap.cloud.lm.sl.cf.process.jobs;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.token.TokenStore;

public class TokensCleanerTest {

    @Mock
    private TokenStore tokenStore;
    @InjectMocks
    private TokensCleaner cleaner;

    @BeforeEach
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testExecute() {
        OAuth2AccessToken expiredToken = mock(OAuth2AccessToken.class);
        when(expiredToken.isExpired()).thenReturn(true);
        OAuth2AccessToken token = mock(OAuth2AccessToken.class);
        when(token.isExpired()).thenReturn(false);

        when(tokenStore.findTokensByClientId(anyString())).thenReturn(Arrays.asList(expiredToken, token));

        cleaner.execute(null);
        verify(tokenStore).removeAccessToken(expiredToken);
        verify(tokenStore, never()).removeAccessToken(token);
    }

}
