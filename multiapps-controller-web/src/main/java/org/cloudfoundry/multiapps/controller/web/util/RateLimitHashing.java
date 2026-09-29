package org.cloudfoundry.multiapps.controller.web.util;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.primitives.Longs;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Central SHA-384 hashing used for operation rate limiting. Both the {@code long} bucket keys and the hex digests used as JMX labels are
 * derived here so the algorithm is declared in a single place.
 */
public final class RateLimitHashing {

    private static final HashFunction HASH_FUNCTION = Hashing.sha384();

    private RateLimitHashing() {
    }

    public static long hashToLong(String input) {
        byte[] digest = hash(input).asBytes();
        return Longs.fromBytes(digest[0], digest[1], digest[2], digest[3], digest[4], digest[5], digest[6], digest[7]);
    }

    public static String hashToHex(String input) {
        return hash(input).toString();
    }

    private static HashCode hash(String input) {
        return HASH_FUNCTION.hashString(input, UTF_8);
    }
}
