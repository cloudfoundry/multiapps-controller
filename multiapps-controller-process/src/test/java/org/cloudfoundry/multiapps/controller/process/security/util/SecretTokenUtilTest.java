package org.cloudfoundry.multiapps.controller.process.security.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SecretTokenUtilTest {

    @Test
    void testIsTokenWhenNull() {
        assertFalse(SecretTokenUtil.isToken(null));
    }

    @Test
    void testIsTokenWhenEmptyString() {
        assertFalse(SecretTokenUtil.isToken(""));
    }

    @Test
    void testIsTokenWhenWrongPrefixedString() {
        assertFalse(SecretTokenUtil.isToken("fake:v1:123"));
        assertFalse(SecretTokenUtil.isToken("dsc:V1:123"));
        assertFalse(SecretTokenUtil.isToken("dsc:v2:123"));
        assertFalse(SecretTokenUtil.isToken("dsc:v1-123"));
    }

    @Test
    void testIsTokenWhenWrongPostfixedString() {
        assertFalse(SecretTokenUtil.isToken("dsc:v1:12a3"));
        assertFalse(SecretTokenUtil.isToken("dsc:v1:12 3"));
        assertFalse(SecretTokenUtil.isToken("dsc:v1:"));
    }

    @Test
    void testIsTokenSuccess() {
        assertTrue(SecretTokenUtil.isToken("dsc:v1:0"));
        assertTrue(SecretTokenUtil.isToken("dsc:v1:42"));
        assertTrue(SecretTokenUtil.isToken("dsc:v1:000123"));
    }

    @Test
    void testIdWhenParsingDigits() {
        assertEquals(0L, SecretTokenUtil.id("dsc:v1:0"));
        assertEquals(42L, SecretTokenUtil.id("dsc:v1:42"));
        assertEquals(123L, SecretTokenUtil.id("dsc:v1:000123"));
    }

    @Test
    void testIdWhenNoDigitsThrows() {
        assertThrows(NumberFormatException.class, () -> SecretTokenUtil.id("dsc:v1:"));
    }

    @Test
    void testIdWhenNonDigitsThrows() {
        assertThrows(NumberFormatException.class, () -> SecretTokenUtil.id("dsc:v1:abc"));
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
        assertTrue(SecretTokenUtil.isToken(token));
        assertEquals(id, SecretTokenUtil.id(token));
    }

}

