package org.cloudfoundry.multiapps.controller.core.security.token.parsing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.InternalAuthenticationServiceException;

class MACValidationStrategyTest {

    private static final String VALID_TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJPbmxpbmUgSldUIEJ1aWxkZXIiLCJpYXQiOjE2MjYxNzA5MTcsImV4cCI6MTYyNjE3MDkyNSwiYXVkIjoid3d3LmV4YW1wbGUuY29tIiwic3ViIjoianJvY2tldEBleGFtcGxlLmNvbSIsIkdpdmVuTmFtZSI6IkpvaG5ueSIsIlN1cm5hbWUiOiJSb2NrZXQiLCJFbWFpbCI6Impyb2NrZXRAZXhhbXBsZS5jb20iLCJSb2xlIjpbIk1hbmFnZXIiLCJQcm9qZWN0IEFkbWluaXN0cmF0b3IiXX0.ApCrXQGcWDHo5NcFiuGL53KmS8YF327m5FnLsxGPrm0";
    private static final String VALID_KEY = "qwertyuiopasdfghjklzxcvbnm123456";

    private final ValidationStrategy macValidationStrategy = new MACValidationStrategy();

    @Test
    void testValidationWithValidToken() {
        boolean validationResult = macValidationStrategy.validateToken(VALID_TOKEN, VALID_KEY);
        assertTrue(validationResult);
    }

    @Test
    void testValidationWithTokenSignedWithAnotherKey() {
        boolean validationResult = macValidationStrategy.validateToken("eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJPbmxpbmUgSldUIEJ1aWxkZXIiLCJpYXQiOjE2MjYxNzA5MTcsImV4cCI6MTYyNjE3MDkyNSwiYXVkIjoid3d3LmV4YW1wbGUuY29tIiwic3ViIjoianJvY2tldEBleGFtcGxlLmNvbSIsIkdpdmVuTmFtZSI6IkpvaG5ueSIsIlN1cm5hbWUiOiJSb2NrZXQiLCJFbWFpbCI6Impyb2NrZXRAZXhhbXBsZS5jb20iLCJSb2xlIjpbIk1hbmFnZXIiLCJQcm9qZWN0IEFkbWluaXN0cmF0b3IiXX0.CJLVyh7tBZew3h1RwBbq0R9tHkRK_QNtFbit-_tCRY8",
                                                                       VALID_KEY);
        assertFalse(validationResult);
    }

    @Test
    void testValidationWithInvalidToken() {
        Exception exception = assertThrows(InternalAuthenticationServiceException.class,
                                           () -> macValidationStrategy.validateToken("invalid", VALID_KEY));
        assertEquals("Invalid serialized unsecured/JWS/JWE object: Missing part delimiters", exception.getMessage());
    }
}
