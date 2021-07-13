package org.cloudfoundry.multiapps.controller.core.security.token.parsers;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;

import java.util.Map;
import java.util.Optional;

import org.cloudfoundry.multiapps.controller.client.uaa.UAAClient;
import org.cloudfoundry.multiapps.controller.core.security.token.parsing.TokenValidationStrategyFactory;
import org.cloudfoundry.multiapps.controller.core.security.token.parsing.ValidationStrategy;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.InternalAuthenticationServiceException;

import com.sap.cloudfoundry.client.facade.oauth2.OAuth2AccessTokenWithAdditionalInfo;

class JwtTokenParserTest {

    @Test
    void testParseNotCorrectJwtTokenWithOnlyTwoFieldsPresented() {
        JwtTokenParser parser = new JwtTokenParserMock(mockUaaClient(), mockTokenValidationStrategyFactory());
        String notCorrectJwtToken = "test.test";
        Optional<OAuth2AccessTokenWithAdditionalInfo> token = parser.parse(notCorrectJwtToken);
        assertTrue(token.isEmpty());
    }

    @Test
    void testParseJwtTokenWithNoExpirationTime() {
        String testNotCorrectToken = "eyJhbGciOiJSUzI1NiIsImtpZCI6ImtleS0xIiwidHlwIjoiSldUIn0.ew0KICAgICJpc3MiOiAiT25saW5lIEpXVCBCdWlsZGVyIiwNCiAgICAiaWF0IjogMTUyNTMzNjQ3OSwNCiAgICAiZXhwIjogbnVsbCwNCiAgICAiYXVkIjogInd3dy5leGFtcGxlLmNvbSIsDQogICAgInN1YiI6ICJqcm9ja2V0QGV4YW1wbGUuY29tIiwNCiAgICAidXNlcl9uYW1lIjogInRlc3QiLA0KICAgICJlbWFpbCI6ICJ0ZXN0X2VtYWlsIiwNCiAgICAiY2xpZW50X2lkIjogInRlc3QtY2xpZW50LWlkIg0KfQ==.mWp4NUYyfHwuncjmgmnVEzMkttUSN0H4HV5WQJ1zgQSOtYS51_OmiXPRQfjy4tzIWkc71uwLjAVPeGTgh6Fh9P5L8didVTxdYD9cEHz0iXOkKZZlVTgf91lRFKa2u4aNMsWtPG2AtsxlD1cSjAkSa3Q2aVZ83gWwRmQKo4jXxuy08AXdEW9ooalR767V-C3F_MfkBi0mxW4bJrmurM8UiLouBoX71sXjzCy52aFumvwl5BEyRvKnN_5NTsJZmBHC28UysHgENzhKA651azbc3oDpbEGWe3fkafNbdQelsb6p9Tgbbcy2iPYvhrwOc9kfARAcbVMt-npmq0Aww02hvQ";
        JwtTokenParser parser = new JwtTokenParserMock(mockUaaClient(), mockTokenValidationStrategyFactory());
        Optional<OAuth2AccessTokenWithAdditionalInfo> token = parser.parse(testNotCorrectToken);
        assertTrue(token.isEmpty());
    }

    @Test
    void testParseJwtTokenWithCorrectedValues() {
        String correctToken = "eeyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJkY2NlNmE0MDc5ZmI0MzhhYTRiMTEwMDU5Y2I1MDU5OSIsInN1YiI6ImYwNjlhODA5LTExMDctNDRjNC1iNzQyLWI0NTNmYjYzYjFkNyIsInNjb3BlIjpbImNsb3VkX2NvbnRyb2xsZXIucmVhZCIsInBhc3N3b3JkLndyaXRlIiwiY2xvdWRfY29udHJvbGxlci53cml0ZSIsIm9wZW5pZCIsInVhYS51c2VyIl0sImNsaWVudF9pZCI6ImNmIiwiY2lkIjoiY2YiLCJhenAiOiJjZiIsImdyYW50X3R5cGUiOiJwYXNzd29yZCIsInVzZXJfaWQiOiJmMDY5YTgwOS0xMTA3LTQ0YzQtYjc0Mi1iNDUzZmI2M2IxZDciLCJvcmlnaW4iOiJsZGFwIiwidXNlcl9uYW1lIjoidGVzdF90ZXN0IiwiZW1haWwiOiJ0ZXN0X3Rlc3QiLCJyZXZfc2lnIjoiMzY1MjU5YzQiLCJpYXQiOjE1MjUzMzA4NTYsImV4cCI6MTUyNTM0MDczMiwiaXNzIjoiaHR0cHM6Ly91YWEuY2Yuc2FwLmhhbmEub25kZW1hbmQuY29tL29hdXRoL3Rva2VuIiwiemlkIjoidWFhIiwiYXVkIjpbImNsb3VkX2NvbnRyb2xsZXIiLCJwYXNzd29yZCIsImNmIiwidWFhIiwib3BlbmlkIl19.OvQpaeituiCEUK-MoSzO43datlGWX95bywpTICxxN-8";
        JwtTokenParser parser = new JwtTokenParserMock(mockUaaClient(), mockTokenValidationStrategyFactory());
        Optional<OAuth2AccessTokenWithAdditionalInfo> token = parser.parse(correctToken);
        assertTrue(token.isPresent());
        Map<String, Object> additionalInfo = token.get()
                                                  .getAdditionalInfo();
        assertFalse(additionalInfo.isEmpty());
        assertEquals("cf", additionalInfo.get("client_id"));
        assertNotNull(additionalInfo.get("exp"));
        assertNotNull(additionalInfo.get("scope"));
        assertEquals("test_test", additionalInfo.get("user_name"));
    }

    @Test
    void testWhenValidationStrategyThrowsException() {
        ValidationStrategy validationStrategy = Mockito.mock(ValidationStrategy.class);
        Mockito.when(validationStrategy.validateToken(anyString(), anyString()))
               .thenThrow(InternalAuthenticationServiceException.class);
        TokenValidationStrategyFactory tokenValidationStrategyFactory = mockTokenValidationStrategyFactory();
        Mockito.when(tokenValidationStrategyFactory.createStrategy(anyString()))
               .thenReturn(validationStrategy);
        JwtTokenParser parser = new JwtTokenParserMock(mockUaaClient(false, "RS256", "not-at-all-matters"),
                                                       true,
                                                       tokenValidationStrategyFactory);
        assertTrue(parser.parse("token-value")
                         .isEmpty());
    }

    @Test
    void testWhenValidationStrategyReturnsFalse() {
        ValidationStrategy validationStrategy = Mockito.mock(ValidationStrategy.class);
        Mockito.when(validationStrategy.validateToken(anyString(), anyString()))
               .thenReturn(false);
        TokenValidationStrategyFactory tokenValidationStrategyFactory = mockTokenValidationStrategyFactory();
        Mockito.when(tokenValidationStrategyFactory.createStrategy(anyString()))
               .thenReturn(validationStrategy);
        JwtTokenParser parser = new JwtTokenParserMock(mockUaaClient(false, "RS256", "not-at-all-matters"),
                                                       true,
                                                       tokenValidationStrategyFactory);
        assertTrue(parser.parse("token-value")
                         .isEmpty());
    }

    @Test
    void testWhenValidationStrategyReturnsTrue() {
        ValidationStrategy validationStrategy = Mockito.mock(ValidationStrategy.class);
        Mockito.when(validationStrategy.validateToken(anyString(), anyString()))
               .thenReturn(true);
        TokenValidationStrategyFactory tokenValidationStrategyFactory = Mockito.mock(TokenValidationStrategyFactory.class);
        Mockito.when(tokenValidationStrategyFactory.createStrategy(anyString()))
               .thenReturn(validationStrategy);
        JwtTokenParser parser = new JwtTokenParserMock(mockUaaClient(false, "RS256", "not-at-all-matters"),
                                                       true,
                                                       tokenValidationStrategyFactory);
        assertDoesNotThrow(() -> parser.parse("token-value"));
    }

    private UAAClient mockUaaClient() {
        return mockUaaClient(true, null, null);
    }

    private UAAClient mockUaaClient(boolean callRealReadToken, String algorithm, String algorithmValue) {
        UAAClient client = Mockito.mock(UAAClient.class);
        if (callRealReadToken) {
            Mockito.when(client.readTokenKey())
                   .thenCallRealMethod();
        }
        if (algorithm != null && algorithmValue != null) {
            Mockito.when(client.readTokenKey())
                   .thenReturn(Map.of("alg", algorithm, "value", algorithmValue));
        } else {
            if (algorithm != null) {
                Mockito.when(client.readTokenKey())
                       .thenReturn(Map.of("alg", algorithm));
            }
            if (algorithmValue != null) {
                Mockito.when(client.readTokenKey())
                       .thenReturn(Map.of("value", algorithmValue));
            }
        }
        return client;
    }

    private TokenValidationStrategyFactory mockTokenValidationStrategyFactory() {
        return Mockito.mock(TokenValidationStrategyFactory.class);
    }

    private static class JwtTokenParserMock extends JwtTokenParser {

        private final boolean shouldVerify;

        public JwtTokenParserMock(UAAClient uaaClient, TokenValidationStrategyFactory tokenValidationStrategyFactory) {
            this(uaaClient, false, tokenValidationStrategyFactory);
        }

        public JwtTokenParserMock(UAAClient uaaClient, boolean shouldVerify,
                                  TokenValidationStrategyFactory tokenValidationStrategyFactory) {
            super(uaaClient, tokenValidationStrategyFactory);
            this.shouldVerify = shouldVerify;
        }

        @Override
        protected void verifyToken(String tokenString) {
            if (shouldVerify) {
                super.verifyToken(tokenString);
            }
        }

    }
}
