package org.cloudfoundry.multiapps.controller.process.security.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SecretTokenUtilTest {

    @Test
    void testIsTokenWhenNull() {
        assertFalse(SecretTokenUtil.isSecretToken(null));
    }

    @Test
    void testIsTokenWhenEmptyString() {
        assertFalse(SecretTokenUtil.isSecretToken(""));
    }

    @Test
    void testIsTokenWhenWrongPrefixedString() {
        assertFalse(SecretTokenUtil.isSecretToken("fake:v1:123"));
        assertFalse(SecretTokenUtil.isSecretToken("dsc:V1:123"));
        assertFalse(SecretTokenUtil.isSecretToken("dsc:v2:123"));
        assertFalse(SecretTokenUtil.isSecretToken("dsc:v1-123"));
    }

    @Test
    void testIsTokenWhenWrongPostfixedString() {
        assertFalse(SecretTokenUtil.isSecretToken("dsc:v1:12a3"));
        assertFalse(SecretTokenUtil.isSecretToken("dsc:v1:12 3"));
        assertFalse(SecretTokenUtil.isSecretToken("dsc:v1:"));
    }

    @Test
    void testIsTokenSuccess() {
        assertTrue(SecretTokenUtil.isSecretToken("dsc:v1:0"));
        assertTrue(SecretTokenUtil.isSecretToken("dsc:v1:42"));
        assertTrue(SecretTokenUtil.isSecretToken("dsc:v1:000123"));
    }

    @Test
    void testIdWhenParsingDigits() {
        assertEquals(0L, SecretTokenUtil.extractId("dsc:v1:0"));
        assertEquals(42L, SecretTokenUtil.extractId("dsc:v1:42"));
        assertEquals(123L, SecretTokenUtil.extractId("dsc:v1:000123"));
    }

    @Test
    void testIdWhenNoDigitsThrows() {
        assertThrows(NumberFormatException.class, () -> SecretTokenUtil.extractId("dsc:v1:"));
    }

    @Test
    void testIdWhenNonDigitsThrows() {
        assertThrows(NumberFormatException.class, () -> SecretTokenUtil.extractId("dsc:v1:abc"));
    }

    @Test
    void testOfSuccess() {
        assertEquals("dsc:v1:0", SecretTokenUtil.of(0));
        assertEquals("dsc:v1:99", SecretTokenUtil.of(99));
    }

    @Test
    void testFlowOfSecretTokenBuild() {
        long id = 981L;
        String token = SecretTokenUtil.of(id);
        assertTrue(SecretTokenUtil.isSecretToken(token));
        assertEquals(id, SecretTokenUtil.extractId(token));
    }

}

