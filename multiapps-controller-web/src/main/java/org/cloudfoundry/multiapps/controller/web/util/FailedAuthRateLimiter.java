package org.cloudfoundry.multiapps.controller.web.util;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.EstimationProbe;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.process.util.BucketStore;
import org.cloudfoundry.multiapps.controller.web.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Throttles repeated failed authentication attempts to curb credential brute-forcing and denial-of-service. Each failed attempt consumes a
 * token from a per-source-IP bucket and, when the attempted username is known, a per-username bucket. Both buckets are backed by the
 * cluster-aware {@link BucketStore}, so counters are shared across instances. When either bucket is exhausted an
 * {@link OperationRateLimitExceededException} is raised. Throttling is a no-op unless explicitly enabled.
 */
@Named
public class FailedAuthRateLimiter {

    private static final Logger LOGGER = LoggerFactory.getLogger(FailedAuthRateLimiter.class);
    private static final Duration REFILL_PERIOD = Duration.ofHours(1);
    private static final long TOKENS_PER_ATTEMPT = 1;

    private final ApplicationConfiguration applicationConfiguration;
    private final BucketStore bucketStore;
    private final BucketConfiguration ipTokenBucketConfiguration;
    private final BucketConfiguration userTokenBucketConfiguration;

    @Inject
    public FailedAuthRateLimiter(ApplicationConfiguration applicationConfiguration, BucketStore bucketStore) {
        this.applicationConfiguration = applicationConfiguration;
        this.bucketStore = bucketStore;
        this.ipTokenBucketConfiguration = buildBucketConfiguration(applicationConfiguration.getFailedAuthRateLimitPerIpCapacity(),
                                                                   applicationConfiguration.getFailedAuthRateLimitPerIpRefillPerHour());
        this.userTokenBucketConfiguration = buildBucketConfiguration(applicationConfiguration.getFailedAuthRateLimitPerUserCapacity(),
                                                                     applicationConfiguration.getFailedAuthRateLimitPerUserRefillPerHour());
    }

    /**
     * Rejects the request if the caller has already exhausted the configured limit, without consuming a token. This is a pre-check run before
     * any expensive downstream work (such as a live token exchange), so that a caller who is already over the limit is turned away cheaply. The
     * username may be {@code null} when it cannot be determined, in which case only the per-IP bucket is inspected.
     */
    public void checkAllowed(String ip, String username) {
        if (!applicationConfiguration.isFailedAuthRateLimitingEnabled()) {
            return;
        }
        checkIpAllowed(ip);
        if (username != null) {
            checkUserAllowed(username);
        }
    }

    /**
     * Records a failed authentication attempt by consuming a token from the per-IP bucket and, when the username is known, the per-username
     * bucket. The username may be {@code null} when it cannot be determined (for example a malformed bearer token), in which case only the
     * per-IP bucket is consumed. Throws if consuming the token exhausts the configured limit.
     */
    public void recordFailedAttempt(String ip, String username) {
        if (!applicationConfiguration.isFailedAuthRateLimitingEnabled()) {
            return;
        }
        consumeIpToken(ip);
        if (username != null) {
            consumeUserToken(username);
        }
    }

    private void checkIpAllowed(String ip) {
        Bucket bucket = bucketStore.getBucket(FailedAuthRateLimitKeys.ipKey(ip), ipTokenBucketConfiguration);
        checkToken(bucket);
    }

    private void checkUserAllowed(String username) {
        Bucket bucket = bucketStore.getBucket(FailedAuthRateLimitKeys.userKey(username), userTokenBucketConfiguration);
        checkToken(bucket);
    }

    private void consumeIpToken(String ip) {
        Bucket bucket = bucketStore.getBucket(FailedAuthRateLimitKeys.ipKey(ip), ipTokenBucketConfiguration);
        consumeToken(bucket);
    }

    private void consumeUserToken(String username) {
        Bucket bucket = bucketStore.getBucket(FailedAuthRateLimitKeys.userKey(username), userTokenBucketConfiguration);
        consumeToken(bucket);
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

    private void checkToken(Bucket bucket) {
        EstimationProbe probe = bucket.estimateAbilityToConsume(TOKENS_PER_ATTEMPT);
        if (!probe.canBeConsumed()) {
            long retryAfterSeconds = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());
            LOGGER.info(Messages.FAILED_AUTH_RATE_LIMIT_EXCEEDED);
            throw new OperationRateLimitExceededException(Messages.FAILED_AUTH_RATE_LIMIT_EXCEEDED, retryAfterSeconds);
        }
    }

    private void consumeToken(Bucket bucket) {
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(TOKENS_PER_ATTEMPT);
        if (!probe.isConsumed()) {
            long retryAfterSeconds = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());
            LOGGER.info(Messages.FAILED_AUTH_RATE_LIMIT_EXCEEDED);
            throw new OperationRateLimitExceededException(Messages.FAILED_AUTH_RATE_LIMIT_EXCEEDED, retryAfterSeconds);
        }
    }
}
