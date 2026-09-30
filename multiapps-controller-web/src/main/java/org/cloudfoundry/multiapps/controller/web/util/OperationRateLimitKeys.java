package org.cloudfoundry.multiapps.controller.web.util;

/**
 * Derives stable {@code long} bucket keys for operation rate limiting.
 * <p>
 * Keys are computed from the SHA-384 digest of a namespaced input string and are therefore deterministic across restarts and JVMs. The
 * space and user namespaces are disjoint by construction, so a space key can never collide with a user key. Truncating the 384-bit digest
 * to its first 64 bits keeps the collision probability negligible for the number of distinct spaces and users a single landscape handles.
 */
public final class OperationRateLimitKeys {

    private static final String SPACE_NAMESPACE_PREFIX = "space:";
    private static final String USER_NAMESPACE_PREFIX = "user:";
    private static final String SEGMENT_SEPARATOR = ":";

    private OperationRateLimitKeys() {
    }

    public static long spaceKey(String spaceGuid) {
        return RateLimitHashing.hashToLong(SPACE_NAMESPACE_PREFIX + spaceGuid);
    }

    public static long userKey(String spaceGuid, String user) {
        return RateLimitHashing.hashToLong(USER_NAMESPACE_PREFIX + spaceGuid + SEGMENT_SEPARATOR + user);
    }
}
