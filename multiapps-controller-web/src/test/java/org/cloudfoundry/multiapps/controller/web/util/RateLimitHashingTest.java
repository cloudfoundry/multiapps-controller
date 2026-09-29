package org.cloudfoundry.multiapps.controller.web.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class RateLimitHashingTest {

    private static final String INPUT = "hello";
    private static final String OTHER_INPUT = "world";
    private static final String EXPECTED_HEX = "59e1748777448c69de6b800d7a33bbfb9ff1b463e44354c3553bcdb9c666fa90125a3c79f90397bdf5f6a13de828684f";
    private static final long EXPECTED_LONG = 6476585864305871977L;

    @Test
    void testHashToHexMatchesKnownSha384Digest() {
        assertEquals(EXPECTED_HEX, RateLimitHashing.hashToHex(INPUT));
    }

    @Test
    void testHashToLongMatchesFirstEightDigestBytes() {
        assertEquals(EXPECTED_LONG, RateLimitHashing.hashToLong(INPUT));
    }

    @Test
    void testHashToHexIsFullSha384Length() {
        assertEquals(96, RateLimitHashing.hashToHex(INPUT)
                                         .length());
    }

    @Test
    void testHashToHexIsDeterministic() {
        assertEquals(RateLimitHashing.hashToHex(INPUT), RateLimitHashing.hashToHex(INPUT));
    }

    @Test
    void testHashToLongIsDeterministic() {
        assertEquals(RateLimitHashing.hashToLong(INPUT), RateLimitHashing.hashToLong(INPUT));
    }

    @Test
    void testHashToHexDiffersForDifferentInput() {
        assertNotEquals(RateLimitHashing.hashToHex(INPUT), RateLimitHashing.hashToHex(OTHER_INPUT));
    }

    @Test
    void testHashToLongDiffersForDifferentInput() {
        assertNotEquals(RateLimitHashing.hashToLong(INPUT), RateLimitHashing.hashToLong(OTHER_INPUT));
    }

    @Test
    void testHashToLongMatchesPrefixOfHex() {
        String hexPrefix = RateLimitHashing.hashToHex(INPUT)
                                           .substring(0, 16);
        assertEquals(Long.parseUnsignedLong(hexPrefix, 16), RateLimitHashing.hashToLong(INPUT));
    }
}
