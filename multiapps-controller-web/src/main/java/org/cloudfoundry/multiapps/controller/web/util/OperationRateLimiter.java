package org.cloudfoundry.multiapps.controller.web.util;

import java.text.MessageFormat;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Named;

import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.services.OperationService;
import org.cloudfoundry.multiapps.controller.process.util.BucketStore;
import org.cloudfoundry.multiapps.controller.web.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;

/**
 * Guards the start of MTA operations against per-space and per-user rate limits. Each start attempt consumes a single token from both the
 * space bucket and the user bucket; if either is exhausted an {@link OperationRateLimitExceededException} is raised.
 */
@Named
public class OperationRateLimiter {

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationRateLimiter.class);
    private static final Duration REFILL_PERIOD = Duration.ofHours(1);
    private static final long TOKENS_PER_OPERATION = 1;
    private static final long NO_RETRY_AFTER_SECONDS = 0;

    private final ApplicationConfiguration applicationConfiguration;
    private final OperationService operationService;
    private final BucketStore bucketStore;

    public OperationRateLimiter(ApplicationConfiguration applicationConfiguration, OperationService operationService,
                                BucketStore bucketStore) {
        this.applicationConfiguration = applicationConfiguration;
        this.operationService = operationService;
        this.bucketStore = bucketStore;
    }

    public void checkStartAllowed(String user, String spaceGuid) {
        if (!applicationConfiguration.isOperationRateLimitingEnabled()) {
            return;
        }
        checkActiveOperationCaps(user, spaceGuid);
        checkTokenBuckets(user, spaceGuid);
    }

    private void rejectAndLog(String user, String spaceGuid, String reason, long retryAfterSeconds) {
        LOGGER.info(MessageFormat.format(Messages.OPERATION_START_RATE_LIMITED_STRUCTURED, user, spaceGuid, reason));
        throw new OperationRateLimitExceededException(reason, retryAfterSeconds);
    }

    private void checkActiveOperationCaps(String user, String spaceGuid) {
        int activeOperationsPerSpace = operationService.createQuery()
                                                       .spaceId(spaceGuid)
                                                       .inNonFinalState()
                                                       .list()
                                                       .size();
        if (activeOperationsPerSpace >= applicationConfiguration.getMaxActiveOperationsPerSpace()) {
            rejectAndLog(user, spaceGuid, Messages.TOO_MANY_ACTIVE_OPERATIONS_IN_SPACE, NO_RETRY_AFTER_SECONDS);
        }
        int activeOperationsPerUser = operationService.createQuery()
                                                      .user(user)
                                                      .spaceId(spaceGuid)
                                                      .inNonFinalState()
                                                      .list()
                                                      .size();
        if (activeOperationsPerUser >= applicationConfiguration.getMaxActiveOperationsPerUser()) {
            rejectAndLog(user, spaceGuid, Messages.TOO_MANY_ACTIVE_OPERATIONS_FOR_USER, NO_RETRY_AFTER_SECONDS);
        }
    }

    private void checkTokenBuckets(String user, String spaceGuid) {
        consumeSpaceToken(spaceGuid, user);
        consumeUserToken(spaceGuid, user);
    }

    private void consumeSpaceToken(String spaceGuid, String user) {
        BucketConfiguration configuration = buildBucketConfiguration(applicationConfiguration.getOperationRateLimitPerSpaceCapacity(),
                                                                     applicationConfiguration.getOperationRateLimitPerSpaceRefillPerHour());
        Bucket bucket = bucketStore.getBucket(OperationRateLimitKeys.spaceKey(spaceGuid), configuration);
        consumeToken(bucket, user, spaceGuid);
    }

    private void consumeUserToken(String spaceGuid, String user) {
        BucketConfiguration configuration = buildBucketConfiguration(applicationConfiguration.getOperationRateLimitPerUserCapacity(),
                                                                     applicationConfiguration.getOperationRateLimitPerUserRefillPerHour());
        Bucket bucket = bucketStore.getBucket(OperationRateLimitKeys.userKey(spaceGuid, user), configuration);
        consumeToken(bucket, user, spaceGuid);
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

    private void consumeToken(Bucket bucket, String user, String spaceGuid) {
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(TOKENS_PER_OPERATION);
        if (!probe.isConsumed()) {
            long retryAfterSeconds = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());
            rejectAndLog(user, spaceGuid, Messages.OPERATION_RATE_LIMIT_EXCEEDED, retryAfterSeconds);
        }
    }
}
