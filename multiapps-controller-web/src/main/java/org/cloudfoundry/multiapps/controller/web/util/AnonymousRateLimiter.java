package org.cloudfoundry.multiapps.controller.web.util;

import java.time.Duration;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.process.util.BucketStore;

/**
 * Throttles requests by source IP regardless of authentication. This covers paths that bypass the security filter chain (such as
 * {@code /public/**}) and anonymous callers. Counters are backed by the cluster-aware {@link BucketStore}, so a single landscape-wide limit
 * is enforced instead of a per-instance one.
 */
@Named
public class AnonymousRateLimiter {

    private static final Duration REFILL_PERIOD = Duration.ofHours(1);
    private static final long TOKENS_PER_REQUEST = 1;

    private final BucketStore bucketStore;
    private final int capacity;
    private final BucketConfiguration ipTokenBucketConfiguration;

    @Inject
    public AnonymousRateLimiter(ApplicationConfiguration applicationConfiguration, BucketStore bucketStore) {
        this.bucketStore = bucketStore;
        this.capacity = applicationConfiguration.getAnonymousRateLimitPerIpCapacity();
        this.ipTokenBucketConfiguration = buildBucketConfiguration(capacity,
                                                                   applicationConfiguration.getAnonymousRateLimitPerIpRefillPerHour());
    }

    public int getCapacity() {
        return capacity;
    }

    public ConsumptionProbe tryConsume(String ip) {
        Bucket bucket = bucketStore.getBucket(AnonymousRateLimitKeys.ipKey(ip), ipTokenBucketConfiguration);
        return bucket.tryConsumeAndReturnRemaining(TOKENS_PER_REQUEST);
    }

    private BucketConfiguration buildBucketConfiguration(int capacity, int refillTokensPerHour) {
        Bandwidth bandwidth = Bandwidth.builder()
                                       .capacity(capacity)
                                       .refillGreedy(refillTokensPerHour, REFILL_PERIOD)
                                       .build();
        return BucketConfiguration.builder()
                                  .addLimit(bandwidth)
                                  .build();
    }
}
