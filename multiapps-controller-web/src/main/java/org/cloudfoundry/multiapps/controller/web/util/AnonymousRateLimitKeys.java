package org.cloudfoundry.multiapps.controller.web.util;

/**
 * Derives stable {@code long} bucket keys for anonymous (unauthenticated / public endpoint) rate limiting.
 * <p>
 * Keys are computed from the SHA-384 digest of a namespaced input string and are therefore deterministic across restarts and JVMs. The
 * namespace is disjoint from the operation and failed-authentication namespaces, so keys never collide across rate limiters.
 */
public final class AnonymousRateLimitKeys {

    private static final String IP_NAMESPACE_PREFIX = "anonymous-ip:";

    private AnonymousRateLimitKeys() {
    }

    public static long ipKey(String ip) {
        return RateLimitHashing.hashToLong(IP_NAMESPACE_PREFIX + ip);
    }
}
