package org.cloudfoundry.multiapps.controller.web.upload.resilience;

import org.cloudfoundry.multiapps.common.util.MiscUtil;
import org.cloudfoundry.multiapps.controller.client.util.CheckedSupplier;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.web.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.MessageFormat;
import java.time.Duration;

public class FileUploadResilientOperationExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileUploadResilientOperationExecutor.class);

    private static final int MAX_ATTEMPTS = 6;
    private static final int BACKOFF_FACTOR = 2;
    private static final long INITIAL_WAIT_TIME_IN_MILLIS = Duration.ofSeconds(10)
                                                                    .toMillis();
    private static final long MAX_WAIT_TIME_IN_MILLIS = Duration.ofSeconds(180)
                                                                .toMillis();

    public <T> T execute(CheckedSupplier<T> operation) throws Exception {
        Exception lastException = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return operation.get();
            } catch (Exception e) {
                if (!isTransientError(e)) {
                    throw e;
                }
                lastException = e;
                if (attempt == MAX_ATTEMPTS) {
                    LOGGER.error(MessageFormat.format(Messages.FILE_UPLOAD_ALL_ATTEMPTS_EXHAUSTED, MAX_ATTEMPTS, e.getMessage()), e);
                    throw e;
                }
                long waitTime = calculateWaitTime(attempt);
                LOGGER.warn(MessageFormat.format(Messages.FILE_UPLOAD_ATTEMPT_FAILED, attempt, MAX_ATTEMPTS, waitTime, e.getMessage()), e);
                sleep(waitTime);
            }
        }
        throw lastException;
    }

    protected void sleep(long millis) {
        MiscUtil.sleep(millis);
    }

    private boolean isTransientError(Exception e) {
        if (e instanceof IOException || e instanceof UncheckedIOException) {
            return true;
        }
        if (e instanceof FileStorageException) {
            Throwable cause = e.getCause();
            return cause instanceof IOException || cause instanceof UncheckedIOException;
        }
        return false;
    }

    private long calculateWaitTime(int attempt) {
        long waitTime = INITIAL_WAIT_TIME_IN_MILLIS * (long) Math.pow(BACKOFF_FACTOR, attempt - 1);
        return Math.min(waitTime, MAX_WAIT_TIME_IN_MILLIS);
    }

}
