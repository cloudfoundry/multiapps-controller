package org.cloudfoundry.multiapps.controller.client.util;

import java.util.HashMap;
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
    void testFromTokenReadsAllAdditionalInfoKeys() {
        Mockito.when(token.getAdditionalInfo())
               .thenReturn(Map.of(TokenProperties.CLIENT_ID_KEY, "c1",
                                  TokenProperties.USER_ID_KEY, "u1",
                                  TokenProperties.USER_NAME_KEY, "alice"));

        TokenProperties props = TokenProperties.fromToken(token);

        Assertions.assertEquals("c1", props.getClientId());
        Assertions.assertEquals("u1", props.getUserId());
        Assertions.assertEquals("alice", props.getUserName());
    }

    @Test
    void testFromTokenReturnsNullsWhenAllKeysMissing() {
        Mockito.when(token.getAdditionalInfo())
               .thenReturn(Map.of());

        TokenProperties props = TokenProperties.fromToken(token);

        Assertions.assertNull(props.getClientId());
        Assertions.assertNull(props.getUserId());
        Assertions.assertNull(props.getUserName());
    }

    @Test
    void testFromTokenReturnsNullForIndividualMissingKeys() {
        Map<String, Object> info = new HashMap<>();
        info.put(TokenProperties.CLIENT_ID_KEY, "c1");
        Mockito.when(token.getAdditionalInfo())
               .thenReturn(info);

        TokenProperties props = TokenProperties.fromToken(token);

        Assertions.assertEquals("c1", props.getClientId());
        Assertions.assertNull(props.getUserId());
        Assertions.assertNull(props.getUserName());
    }

    @Test
    void testFromTokenThrowsWhenAdditionalInfoIsNull() {
        Mockito.when(token.getAdditionalInfo())
               .thenReturn(null);

        Assertions.assertThrows(NullPointerException.class, () -> TokenProperties.fromToken(token));
    }

    @Test
    void testFromTokenThrowsWhenValueIsNotAString() {
        Map<String, Object> info = new HashMap<>();
        info.put(TokenProperties.CLIENT_ID_KEY, Integer.valueOf(42));
        Mockito.when(token.getAdditionalInfo())
               .thenReturn(info);

        Assertions.assertThrows(ClassCastException.class, () -> TokenProperties.fromToken(token));
    }
}
