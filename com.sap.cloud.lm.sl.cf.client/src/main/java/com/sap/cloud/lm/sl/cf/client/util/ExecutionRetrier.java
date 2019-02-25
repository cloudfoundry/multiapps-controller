package com.sap.cloud.lm.sl.cf.client.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import org.springframework.http.HttpStatus;

import com.sap.cloud.lm.sl.common.util.CommonUtil;

public class ExecutionRetrier {

    private static final long DEFAULT_RETRY_COUNT = 3;
    private static final long DEFAULT_WAIT_TIME_BETWEEN_RETRIES_IN_MILLIS = 5000;

    private boolean failSafe;
    private long retryCount = DEFAULT_RETRY_COUNT;
    private long waitTimeBetweenRetriesInMillis = DEFAULT_WAIT_TIME_BETWEEN_RETRIES_IN_MILLIS;

    public ExecutionRetrier failSafe() {
        this.failSafe = true;
        return this;
    }

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
            } catch (Exception e) {
                new ExceptionHandlerFactory().geExceptionHandler(e, httpStatuses, failSafe)
                    .handleException(e);
            }
            CommonUtil.sleep(waitTimeBetweenRetriesInMillis);
        }
        return executeWithGenericExceptionHandler(supplier);
    }

    private <T> T executeWithGenericExceptionHandler(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            new GenericExceptionHandler(failSafe).handleException(e);
        }
        return null;
    }

    public void executeWithRetry(Runnable runnable, HttpStatus... httpStatusesToIgnore) {
        executeWithRetry(() -> {
            runnable.run();
            return null;
        }, httpStatusesToIgnore);
    }

}
