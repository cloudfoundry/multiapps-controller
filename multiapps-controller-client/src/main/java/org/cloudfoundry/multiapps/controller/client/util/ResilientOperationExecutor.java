package org.cloudfoundry.multiapps.controller.client.util;

import java.text.MessageFormat;
import java.util.function.Supplier;

import org.cloudfoundry.multiapps.common.util.MiscUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.timeout.TimeoutException;

public class ResilientOperationExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResilientOperationExecutor.class);

    private static final long DEFAULT_RETRY_COUNT = 3;
    protected static final long DEFAULT_WAIT_TIME_BETWEEN_RETRIES_IN_MILLIS = 5000;
    protected static final long MAX_WAIT_TIME_BETWEEN_RETRIES_IN_MILLIS = 30000;

    private long retryCount = DEFAULT_RETRY_COUNT;
    private long waitTimeBetweenRetriesInMillis = DEFAULT_WAIT_TIME_BETWEEN_RETRIES_IN_MILLIS;

    public ResilientOperationExecutor withRetryCount(long retryCount) {
        this.retryCount = retryCount;
        return this;
    }

    public ResilientOperationExecutor withWaitTimeBetweenRetriesInMillis(long waitTimeBetweenRetriesInMillis) {
        this.waitTimeBetweenRetriesInMillis = waitTimeBetweenRetriesInMillis;
        return this;
    }

    public void execute(Runnable operation) {
        execute(() -> {
            operation.run();
            return null;
        });
    }

    public <T> T execute(Supplier<T> operation) {
        for (int i = 1; i < retryCount; i++) {
            try {
                return operation.get();
            } catch (RuntimeException e) {
                handle(e);
                MiscUtil.sleep(waitTimeBetweenRetriesInMillis);
            }
        }
        return operation.get();
    }

    protected void handle(RuntimeException e) {
        if (e instanceof TimeoutException) {
            LOGGER.warn("Retrying operation that failed with exceeded timeout while waiting response from Cloud Controller", e);
            return;
        }
        LOGGER.warn(MessageFormat.format("Retrying operation that failed with message: {0}", e.getMessage()), e);
    }

}
