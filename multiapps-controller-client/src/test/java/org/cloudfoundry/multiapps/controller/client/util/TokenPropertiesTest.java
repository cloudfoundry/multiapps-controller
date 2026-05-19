package org.cloudfoundry.multiapps.controller.client.util;

import java.util.Map;

import org.cloudfoundry.multiapps.controller.client.facade.oauth2.OAuth2AccessTokenWithAdditionalInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class TokenPropertiesTest {

    @Mock
    private OAuth2AccessTokenWithAdditionalInfo token;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
    }

    @Test
    void testGettersExposeConstructorValues() {
        TokenProperties props = new TokenProperties("client-x", "user-id-x", "user-name-x");

        Assertions.assertEquals("client-x", props.getClientId());
        Assertions.assertEquals("user-id-x", props.getUserId());
        Assertions.assertEquals("user-name-x", props.getUserName());
    }

    @Test
    void testFromTokenReadsAllAdditionalInfoKeys() {
        Mockito.when(token.getAdditionalInfo())
               .thenReturn(Map.of("client_id", "c1",
                                  "user_id", "u1",
                                  "user_name", "alice"));

        TokenProperties props = TokenProperties.fromToken(token);

        Assertions.assertEquals("c1", props.getClientId());
        Assertions.assertEquals("u1", props.getUserId());
        Assertions.assertEquals("alice", props.getUserName());
    }

    @Test
    void testFromTokenReturnsNullsWhenAdditionalInfoEmpty() {
        Mockito.when(token.getAdditionalInfo())
               .thenReturn(Map.of());

        TokenProperties props = TokenProperties.fromToken(token);

        Assertions.assertNull(props.getClientId());
        Assertions.assertNull(props.getUserId());
        Assertions.assertNull(props.getUserName());
    }
}
