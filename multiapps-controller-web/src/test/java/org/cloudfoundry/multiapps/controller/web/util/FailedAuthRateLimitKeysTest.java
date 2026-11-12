package org.cloudfoundry.multiapps.controller.web.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class FailedAuthRateLimitKeysTest {

    private static final String IP = "203.0.113.42";
    private static final String OTHER_IP = "198.51.100.7";
    private static final String USER = "john.doe";
    private static final String OTHER_USER = "jane.roe";

    @Test
    void testIpKeyIsDeterministic() {
        assertEquals(FailedAuthRateLimitKeys.ipKey(IP), FailedAuthRateLimitKeys.ipKey(IP));
    }

    @Test
    void testUserKeyIsDeterministic() {
        assertEquals(FailedAuthRateLimitKeys.userKey(USER), FailedAuthRateLimitKeys.userKey(USER));
    }

    @Test
    void testIpKeyAndUserKeyAreDisjointForSameValue() {
        assertNotEquals(FailedAuthRateLimitKeys.ipKey(USER), FailedAuthRateLimitKeys.userKey(USER));
    }

    @Test
    void testDifferentIpsProduceDifferentKeys() {
        assertNotEquals(FailedAuthRateLimitKeys.ipKey(IP), FailedAuthRateLimitKeys.ipKey(OTHER_IP));
    }

    @Test
    void testDifferentUsersProduceDifferentKeys() {
        assertNotEquals(FailedAuthRateLimitKeys.userKey(USER), FailedAuthRateLimitKeys.userKey(OTHER_USER));
    }

    @Test
    void testFailedAuthKeysAreDisjointFromAnonymousKeys() {
        assertNotEquals(FailedAuthRateLimitKeys.ipKey(IP), AnonymousRateLimitKeys.ipKey(IP));
    }

    @Test
    void testNoCollisionsAcrossRealisticSamples() {
        List<String> ips = List.of(IP, OTHER_IP, "10.0.0.1", "2001:db8::1");
        List<String> users = List.of(USER, OTHER_USER, "admin", "service-account-1");
        Set<Long> keys = new HashSet<>();
        for (String ip : ips) {
            keys.add(FailedAuthRateLimitKeys.ipKey(ip));
        }
        for (String user : users) {
            keys.add(FailedAuthRateLimitKeys.userKey(user));
        }
        assertEquals(ips.size() + users.size(), keys.size());
    }
}
