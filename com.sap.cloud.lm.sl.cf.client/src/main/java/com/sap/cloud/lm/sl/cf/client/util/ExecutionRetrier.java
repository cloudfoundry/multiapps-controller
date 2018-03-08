package com.sap.cloud.lm.sl.cf.client.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;

public class ExecutionRetrier {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionRetrier.class);

    private static final long DEFAULT_RETRY_COUNT = 3;
    private static final long DEFAULT_WAIT_TIME_BETWEEN_RETRIES_IN_MILLIS = 5000;

    private long retryCount = DEFAULT_RETRY_COUNT;
    private long waitTimeBetweenRetriesInMillis = DEFAULT_WAIT_TIME_BETWEEN_RETRIES_IN_MILLIS;

    public ExecutionRetrier withRetryCount(long retryCount) {
        this.retryCount = retryCount;
        return this;
    }

    public ExecutionRetrier withWaitTimeBetweenRetriesInMillis(long waitTimeBetweenRetriesInMillis) {
        this.waitTimeBetweenRetriesInMillis = waitTimeBetweenRetriesInMillis;
        return this;
    }

    public <T> T executeWithRetry(Supplier<T> supplier, HttpStatus... httpStatusesToIgnore) {
        Set<HttpStatus> httpStatuses = new HashSet<>();
        httpStatuses.addAll(Arrays.asList(httpStatusesToIgnore));
        for (int i = 1; i < retryCount; i++) {
            try {
                return supplier.get();
            } catch (ResourceAccessException e) {
                LOGGER.warn(e.getMessage(), e);
            } catch (CloudFoundryException e) {
                if (!shouldIgnoreException(e, httpStatuses)) {
                    throw e;
                }
                LOGGER.warn("Retrying failed request with status: " + e.getStatusCode() + " and message: " + e.getMessage());
            }
            sleep(waitTimeBetweenRetriesInMillis);
        }
        return supplier.get();
    }

    public void executeWithRetry(Runnable runnable, HttpStatus... httpStatusesToIgnore) {
        executeWithRetry(() -> {
            runnable.run();
            return null;
        }, httpStatusesToIgnore);
    }

    private boolean shouldIgnoreException(CloudFoundryException ex, Set<HttpStatus> httpStatusesToIgnore) {
        for (HttpStatus status : httpStatusesToIgnore) {
            if (ex.getStatusCode().equals(status)) {
                return true;
            }
        }
        return ex.getStatusCode().equals(HttpStatus.INTERNAL_SERVER_ERROR) || ex.getStatusCode().equals(HttpStatus.BAD_GATEWAY)
            || ex.getStatusCode().equals(HttpStatus.SERVICE_UNAVAILABLE);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new IllegalStateException("Waiting to retry operation was interrupted", e);
        }
    }

}
