package org.cloudfoundry.multiapps.controller.web.upload.resilience;

import java.text.MessageFormat;
import java.time.Duration;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.common.util.MiscUtil;
import org.cloudfoundry.multiapps.controller.client.util.CheckedSupplier;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.persistence.services.resilience.RetryableErrorClassifier;
import org.cloudfoundry.multiapps.controller.web.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class FileUploadResilientOperationExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileUploadResilientOperationExecutor.class);

    private static final int MAX_ATTEMPTS = 7;
    private static final int BACKOFF_FACTOR = 2;
    private static final long INITIAL_WAIT_TIME_IN_MILLIS = Duration.ofSeconds(10)
                                                                    .toMillis();
    private static final long MAX_WAIT_TIME_IN_MILLIS = Duration.ofSeconds(300)
                                                                .toMillis();

    private final RetryableErrorClassifier classifier;

    @Inject
    public FileUploadResilientOperationExecutor(RetryableErrorClassifier classifier) {
        this.classifier = classifier;
    }

    public <T> T execute(CheckedSupplier<T> operation) throws Exception {
        Exception lastException = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return operation.get();
            } catch (Exception e) {
                if (!isRetryableError(e)) {
                    throw e;
                }
                lastException = e;
                if (attempt < MAX_ATTEMPTS) {
                    long waitTime = calculateWaitTime(attempt);
                    LOGGER.warn(MessageFormat.format(Messages.FILE_UPLOAD_ATTEMPT_FAILED, attempt, MAX_ATTEMPTS, waitTime, e.getMessage()),
                                e);
                    sleep(waitTime);
                }
            }
        }
        LOGGER.error(MessageFormat.format(Messages.FILE_UPLOAD_ALL_ATTEMPTS_EXHAUSTED, MAX_ATTEMPTS, lastException.getMessage()),
                     lastException);
        throw lastException;
    }

    private boolean isRetryableError(Exception e) {
        Throwable cause = e instanceof FileStorageException ? e.getCause() : e;
        while (cause != null) {
            if (classifier.isRetryable(cause)) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private long calculateWaitTime(int attempt) {
        long waitTime = (long) (INITIAL_WAIT_TIME_IN_MILLIS * Math.pow(BACKOFF_FACTOR, (double) attempt - 1));
        return Math.min(waitTime, MAX_WAIT_TIME_IN_MILLIS);
    }

    protected void sleep(long millis) {
        MiscUtil.sleep(millis);
    }

}
