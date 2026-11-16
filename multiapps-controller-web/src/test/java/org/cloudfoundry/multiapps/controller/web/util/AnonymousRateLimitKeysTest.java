package org.cloudfoundry.multiapps.controller.web.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class AnonymousRateLimitKeysTest {

    private static final String IP = "203.0.113.42";
    private static final String OTHER_IP = "198.51.100.7";

    @Test
    void testIpKeyIsDeterministic() {
        assertEquals(AnonymousRateLimitKeys.ipKey(IP), AnonymousRateLimitKeys.ipKey(IP));
    }

    @Test
    void testDifferentIpsProduceDifferentKeys() {
        assertNotEquals(AnonymousRateLimitKeys.ipKey(IP), AnonymousRateLimitKeys.ipKey(OTHER_IP));
    }

    @Test
    void testAnonymousKeysAreDisjointFromFailedAuthKeys() {
        assertNotEquals(AnonymousRateLimitKeys.ipKey(IP), FailedAuthRateLimitKeys.ipKey(IP));
    }

    @Test
    void testNoCollisionsAcrossRealisticSamples() {
        List<String> ips = List.of(IP, OTHER_IP, "10.0.0.1", "2001:db8::1");
        Set<Long> keys = new HashSet<>();
        for (String ip : ips) {
            keys.add(AnonymousRateLimitKeys.ipKey(ip));
        }
        assertEquals(ips.size(), keys.size());
    }
}
