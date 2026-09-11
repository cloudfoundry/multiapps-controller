package org.cloudfoundry.multiapps.controller.web.util;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.primitives.Longs;

/**
 * Derives stable {@code long} bucket keys for operation rate limiting.
 * <p>
 * Keys are computed from the SHA-256 digest of a namespaced input string and are therefore deterministic across restarts and JVMs. The
 * space and user namespaces are disjoint by construction, so a space key can never collide with a user key. Truncating the 256-bit digest
 * to its first 64 bits keeps the collision probability negligible for the number of distinct spaces and users a single landscape handles.
 */
public final class OperationRateLimitKeys {

    private static final String SPACE_NAMESPACE_PREFIX = "space:";
    private static final String USER_NAMESPACE_PREFIX = "user:";
    private static final String SEGMENT_SEPARATOR = ":";
    private static final HashFunction HASH_FUNCTION = Hashing.sha256();

    private OperationRateLimitKeys() {
    }

    public static long spaceKey(String spaceGuid) {
        return hashToLong(SPACE_NAMESPACE_PREFIX + spaceGuid);
    }

    public static long userKey(String spaceGuid, String user) {
        return hashToLong(USER_NAMESPACE_PREFIX + spaceGuid + SEGMENT_SEPARATOR + user);
    }

    private static long hashToLong(String input) {
        byte[] digest = HASH_FUNCTION.hashString(input, UTF_8)
                                     .asBytes();
        return Longs.fromBytes(digest[0], digest[1], digest[2], digest[3], digest[4], digest[5], digest[6], digest[7]);
    }
}
