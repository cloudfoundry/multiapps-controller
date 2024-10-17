package org.cloudfoundry.multiapps.controller.client.util;

import java.text.MessageFormat;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.collections4.SetUtils;
import org.cloudfoundry.multiapps.common.util.MiscUtil;
import org.cloudfoundry.multiapps.controller.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import com.sap.cloudfoundry.client.facade.CloudOperationException;

public class ResilientCloudOperationExecutor extends ResilientOperationExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResilientCloudOperationExecutor.class);

    private static final Set<HttpStatus> DEFAULT_STATUSES_TO_IGNORE = Set.of(HttpStatus.GATEWAY_TIMEOUT, HttpStatus.REQUEST_TIMEOUT,
                                                                             HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.BAD_GATEWAY,
                                                                             HttpStatus.SERVICE_UNAVAILABLE);

    private static final int DEFAULT_TIMEOUT_RETRY_WAIT_TIME_IN_MILLIS = 30 * 1000; // 30 seconds

    private static final Map<Integer, Duration> RETRY_COUNT_RESPONSE_TIME_BACKOFF = Map.of(1, Duration.ofMinutes(5), 2,
                                                                                           Duration.ofMinutes(8), 3,
                                                                                           Duration.ofMinutes(15));

    private Set<HttpStatus> additionalStatusesToIgnore = Collections.emptySet();

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
