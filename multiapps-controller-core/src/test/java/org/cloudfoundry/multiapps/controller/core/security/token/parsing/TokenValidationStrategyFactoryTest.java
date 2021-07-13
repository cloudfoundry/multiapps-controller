package org.cloudfoundry.multiapps.controller.core.security.token.parsing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.InternalAuthenticationServiceException;

class TokenValidationStrategyFactoryTest {

    private final TokenValidationStrategyFactory tokenValidationStrategyFactory = new TokenValidationStrategyFactory();

    @Test
    void testCreateRS256() {
        ValidationStrategy validationStrategy = tokenValidationStrategyFactory.createStrategy("RS256");
        assertTrue(validationStrategy instanceof RSAValidationStrategy);
    }

    @Test
    void testCreateSHA256withRSA() {
        ValidationStrategy validationStrategy = tokenValidationStrategyFactory.createStrategy("SHA256withRSA");
        assertTrue(validationStrategy instanceof RSAValidationStrategy);
    }

    @Test
    void testCreateHS256() {
        ValidationStrategy validationStrategy = tokenValidationStrategyFactory.createStrategy("HS256");
        assertTrue(validationStrategy instanceof MACValidationStrategy);
    }

    @Test
    void testCreateHMACSHA256() {
        ValidationStrategy validationStrategy = tokenValidationStrategyFactory.createStrategy("HMACSHA256");
        assertTrue(validationStrategy instanceof MACValidationStrategy);
    }

    @Test
    void testCreateWithUnsupportedAlgorithm() {
        Exception exception = assertThrows(InternalAuthenticationServiceException.class,
                                           () -> tokenValidationStrategyFactory.createStrategy("invalid"));
        assertEquals("Unsupported algorithm: \"invalid\"", exception.getMessage());
    }

}
