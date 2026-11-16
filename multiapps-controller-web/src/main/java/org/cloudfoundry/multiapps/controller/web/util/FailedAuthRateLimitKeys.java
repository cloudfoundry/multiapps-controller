package org.cloudfoundry.multiapps.controller.web.util;

/**
 * Derives stable {@code long} bucket keys for failed-authentication rate limiting.
 * <p>
 * Keys are computed from the SHA-384 digest of a namespaced input string and are therefore deterministic across restarts and JVMs. The IP
 * and user namespaces are disjoint by construction and never collide with each other or with the operation rate limit namespaces.
 * Truncating the 384-bit digest to its first 64 bits keeps the collision probability negligible.
 */
public final class FailedAuthRateLimitKeys {

    private static final String IP_NAMESPACE_PREFIX = "failed-auth-ip:";
    private static final String USER_NAMESPACE_PREFIX = "failed-auth-user:";

    private FailedAuthRateLimitKeys() {
    }

    public static long ipKey(String ip) {
        return RateLimitHashing.hashToLong(IP_NAMESPACE_PREFIX + ip);
    }

    public static long userKey(String username) {
        return RateLimitHashing.hashToLong(USER_NAMESPACE_PREFIX + username);
    }
}
