package org.cloudfoundry.multiapps.controller.core.security.token.parsing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.InternalAuthenticationServiceException;

class RSAValidationStrategyTest {

    private static final String VALID_TOKEN = "eyJhbGciOiJSUzI1NiIsImprdSI6Imh0dHBzOi8vdWFhLmNmLnNhcC5oYW5hLm9uZGVtYW5kLmNvbS90b2tlbl9rZXlzIiwia2lkIjoia2V5LTEiLCJ0eXAiOiJKV1QifQ.eyJqdGkiOiIzNzM5YTVjNWIzMWE0ZTY3YjgxMzlhZmU3MDQwYWUzNiIsInN1YiI6IjNmNzJhYjFhLTc3MmEtNGNiMC04NGZjLTA0YThiYmU1YTY1YyIsInNjb3BlIjpbImNsb3VkX2NvbnRyb2xsZXIucmVhZCIsInBhc3N3b3JkLndyaXRlIiwiY2xvdWRfY29udHJvbGxlci53cml0ZSIsIm9wZW5pZCIsInVhYS51c2VyIl0sImNsaWVudF9pZCI6ImNmIiwiY2lkIjoiY2YiLCJhenAiOiJjZiIsInJldm9jYWJsZSI6dHJ1ZSwiZ3JhbnRfdHlwZSI6InBhc3N3b3JkIiwidXNlcl9pZCI6IjNmNzJhYjFhLTc3MmEtNGNiMC04NGZjLTA0YThiYmU1YTY1YyIsIm9yaWdpbiI6InNhcC5pZHMiLCJ1c2VyX25hbWUiOiJpdmFuLmRpbWl0cm92QHNhcC5jb20iLCJlbWFpbCI6Iml2YW4uZGltaXRyb3ZAc2FwLmNvbSIsImF1dGhfdGltZSI6MTYyMTMzMDMzOSwicmV2X3NpZyI6ImIzYzc0MDZhIiwiaWF0IjoxNjIxMzQ1MzE5LCJleHAiOjE2MjEzNDU5MTksImlzcyI6Imh0dHBzOi8vdWFhLmNmLnNhcC5oYW5hLm9uZGVtYW5kLmNvbS9vYXV0aC90b2tlbiIsInppZCI6InVhYSIsImF1ZCI6WyJjbG91ZF9jb250cm9sbGVyIiwicGFzc3dvcmQiLCJjZiIsInVhYSIsIm9wZW5pZCJdfQ.eDNu8DHsXcTrT618uAKy-gxSedmCjWM_UV1r73jGi9gg0wOaLFx07x1smP-p_9FSiyAIPHuoc8-1PtP2BK9QdNoKIUFEXr55wAwCLdTB5AqVxo5xQS3clUNRRNgEkejYumMLU3QPNVgKcom-hyT-peZ0O7Kr3HN_KPHiAZ04-kx52uLxtyVaXpP8Lp4OjFIssUkSnb131jFPcDjdYZHSf-fgLU5OjVLJV8MyZzhAxgxO2_FTYU7UEO7T-5ZIDTlipktjP0So_xjcnXbqKo0Oo5uzDMiOGZ7m8a-Bwqg02fQ4Bzvq86ylEmXBI6C7mOeI4oRZAjR18NWwLVXxYJc3IQ";
    private static final String UNWRAPPED_PUBLIC_KEY = "-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArnSd/Sxq+AqXqtc6fYYc\nyvgvrN2g+dd643AsTYjLZJxVBBdxADyhW/R408tRyrcZzlkC1pAbyxD3cc5tglmH\nEiAl6dWd8rCVQeWeQMma+vDR2WkpqmhUGqdy13Y/esezrqhvc2RNCCtrtCFf3i3J\nRzh9hS7pnWlqzzyRaVETOcpS9Q5jeyytZrxXjZAYX77QCDgQdpELNhL/iz0Gzval\nGT2h4Jdb3i7y7Co/9cfm3jAuS8N1qbqsR/o6rzzeIrjDCuI5/BLL3Bph76TvgdHT\ne/tN+iRNG5xlg0460GyjSwQqp1mR21+sVFmTE2Flc2fAI2473B93Xk6IEoEI96eq\nYwIDAQAB\n-----END PUBLIC KEY-----";

    private final ValidationStrategy rsaValidationStrategy = new RSAValidationStrategy();

    @Test
    void testValidationWithValidToken() {
        boolean validationResult = rsaValidationStrategy.validateToken(VALID_TOKEN, UNWRAPPED_PUBLIC_KEY);
        assertTrue(validationResult);
    }

    @Test
    void testValidationWithTokenSignedWithAnotherKey() {
        boolean validationResult = rsaValidationStrategy.validateToken("eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMn0.POstGetfAytaZS82wHcjoTyoqhMyxXiWdR7Nn7A29DNSl0EiXLdwJ6xC6AfgZWF1bOsS_TuYI3OG85AmiExREkrS6tDfTQ2B3WXlrr-wp5AokiRbz3_oB4OxG-W9KcEEbDRcZc0nH3L7LzYptiy1PtAylQGxHTWZXtGz4ht0bAecBgmpdgXMguEIcoqPJ1n3pIWk_dUZegpqx0Lka21H6XxUTxiy8OcaarA8zdnPUnV6AmNP3ecFawIFYdvJB_cm-GvpCSbr8G8y_Mllj8f4x9nBH8pQux89_6gUY618iYv7tuPWBFfEbLxtF2pZS6YC1aSfLQxeNe8djT9YjpvRZA",
                                                                       UNWRAPPED_PUBLIC_KEY);
        assertFalse(validationResult);
    }

    @Test
    void testValidationWithInvalidToken() {
        Exception exception = assertThrows(InternalAuthenticationServiceException.class,
                                           () -> rsaValidationStrategy.validateToken("invalid", UNWRAPPED_PUBLIC_KEY));
        assertEquals("Invalid serialized unsecured/JWS/JWE object: Missing part delimiters", exception.getMessage());
    }
}
