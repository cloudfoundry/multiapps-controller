package org.cloudfoundry.multiapps.controller.client.util;

import java.text.MessageFormat;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import org.apache.commons.collections4.SetUtils;
import org.cloudfoundry.multiapps.common.util.MiscUtil;
import org.cloudfoundry.multiapps.controller.Messages;
import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

public class ResilientCloudOperationExecutor extends ResilientOperationExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResilientCloudOperationExecutor.class);

    private static final Set<HttpStatus> DEFAULT_STATUSES_TO_IGNORE = Set.of(HttpStatus.GATEWAY_TIMEOUT, HttpStatus.REQUEST_TIMEOUT,
                                                                             HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.BAD_GATEWAY,
                                                                             HttpStatus.SERVICE_UNAVAILABLE,
                                                                             HttpStatus.TOO_MANY_REQUESTS);

    private static final int DEFAULT_TIMEOUT_RETRY_WAIT_TIME_IN_MILLIS = 30 * 1000; // 30 seconds

    private static final Map<Integer, Duration> RETRY_COUNT_RESPONSE_TIME_BACKOFF = Map.of(1, Duration.ofMinutes(5), 2,
                                                                                           Duration.ofMinutes(8), 3,
                                                                                           Duration.ofMinutes(15));

    private static final long RATE_LIMIT_RETRY_AFTER_CAP_IN_SECONDS = 120L;
    private static final long RATE_LIMIT_FALLBACK_WAIT_IN_MILLIS = 60_000L;
    private static final long RANDOM_RETRY_MIN_WAIT_IN_MILLIS = 30_000L;
    private static final long RANDOM_RETRY_MAX_WAIT_IN_MILLIS = 90_000L;

    private Set<HttpStatus> additionalStatusesToIgnore = Collections.emptySet();
    private LongConsumer sleeper = MiscUtil::sleep;
    private LongSupplier randomDelaySupplier = () -> ThreadLocalRandom.current()
                                                                       .nextLong(RANDOM_RETRY_MIN_WAIT_IN_MILLIS,
                                                                                 RANDOM_RETRY_MAX_WAIT_IN_MILLIS + 1);

    @Override
    public ResilientCloudOperationExecutor withRetryCount(long retryCount) {
        return (ResilientCloudOperationExecutor) super.withRetryCount(retryCount);
    }

    @Override
    public ResilientCloudOperationExecutor withWaitTimeBetweenRetriesInMillis(long waitTimeBetweenRetriesInMillis) {
        return (ResilientCloudOperationExecutor) super.withWaitTimeBetweenRetriesInMillis(waitTimeBetweenRetriesInMillis);
    }

    public ResilientCloudOperationExecutor withStatusesToIgnore(HttpStatus... statusesToIgnore) {
        this.additionalStatusesToIgnore = new HashSet<>(Arrays.asList(statusesToIgnore));
        return this;
    }

    ResilientCloudOperationExecutor withSleeper(LongConsumer sleeper) {
        this.sleeper = sleeper;
        return this;
    }

    ResilientCloudOperationExecutor withRandomDelaySupplier(LongSupplier randomDelaySupplier) {
        this.randomDelaySupplier = randomDelaySupplier;
        return this;
    }

    @Override
    public <T> T execute(Supplier<T> operation) {
        for (long i = 1; i < retryCount; i++) {
            try {
                return operation.get();
            } catch (CloudOperationException e) {
                handle(e);
                long waitMillis = computeWaitMillis(e, i);
                sleeper.accept(waitMillis);
            }
        }
        return operation.get();
    }

    public <T> T executeWithExponentialBackoff(Function<Duration, T> operation) {
        int waitTimeBetweenRetriesInMillis = DEFAULT_TIMEOUT_RETRY_WAIT_TIME_IN_MILLIS;
        int retryIndex = 1;
        for (int i = 1; i < RETRY_COUNT_RESPONSE_TIME_BACKOFF.size(); i++) {
            try {
                return operation.apply(RETRY_COUNT_RESPONSE_TIME_BACKOFF.get(retryIndex++));
            } catch (RuntimeException e) {
                handle(e);
                if (e.getCause() instanceof java.util.concurrent.TimeoutException
                    || e instanceof io.netty.handler.timeout.TimeoutException) {
                    LOGGER.info(Messages.WAITING_MS_BEFORE_RETRYING_WITH_TIMEOUT_OF_MS, waitTimeBetweenRetriesInMillis,
                                RETRY_COUNT_RESPONSE_TIME_BACKOFF.get(retryIndex)
                                                                 .toMillis());
                    MiscUtil.sleep(waitTimeBetweenRetriesInMillis);
                    waitTimeBetweenRetriesInMillis *= 2;
                }
            }
        }
        return operation.apply(RETRY_COUNT_RESPONSE_TIME_BACKOFF.get(retryIndex));
    }

    @Override
    protected void handle(RuntimeException e) {
        if (e instanceof CloudOperationException) {
            handle((CloudOperationException) e);
        } else {
            super.handle(e);
        }
    }

    protected void handle(CloudOperationException e) {
        if (!shouldRetry(e)) {
            throw e;
        }
        LOGGER.warn(MessageFormat.format("Retrying operation that failed with status {0} and message: {1}", e.getStatusCode(),
                                         e.getMessage()),
                    e);
    }

    private long computeWaitMillis(RuntimeException e, long attemptIndex) {
        if (e instanceof CloudOperationException) {
            CloudOperationException cloudException = (CloudOperationException) e;
            if (cloudException.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                Long retryAfterSeconds = cloudException.getRetryAfterSeconds();
                if (retryAfterSeconds != null) {
                    long cappedSeconds = Math.max(1L, Math.min(retryAfterSeconds, RATE_LIMIT_RETRY_AFTER_CAP_IN_SECONDS));
                    long waitMillis = cappedSeconds * 1000L;
                    LOGGER.info(Messages.RATE_LIMITED_BY_CC_WAITING_S, retryAfterSeconds, cappedSeconds);
                    return waitMillis;
                }
                LOGGER.info(Messages.RATE_LIMITED_BY_CC_NO_HEADER_WAITING_S, RATE_LIMIT_FALLBACK_WAIT_IN_MILLIS);
                return RATE_LIMIT_FALLBACK_WAIT_IN_MILLIS;
            }
        }
        if (attemptIndex >= 2) {
            long waitMillis = randomDelaySupplier.getAsLong();
            LOGGER.info(Messages.RANDOM_WAIT_BEFORE_RETRY_S, waitMillis);
            return waitMillis;
        }
        return waitTimeBetweenRetriesInMillis;
    }

    private boolean shouldRetry(CloudOperationException e) {
        return getStatusesToIgnore().contains(e.getStatusCode());
    }

    private Set<HttpStatus> getStatusesToIgnore() {
        if (additionalStatusesToIgnore.isEmpty()) {
            return DEFAULT_STATUSES_TO_IGNORE;
        }
        return SetUtils.union(DEFAULT_STATUSES_TO_IGNORE, additionalStatusesToIgnore);
    }

}
