package org.cloudfoundry.multiapps.controller.web.upload;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.persistence.services.resilience.RetryableErrorClassifier;
import org.cloudfoundry.multiapps.controller.web.upload.resilience.FileUploadResilientOperationExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileUploadResilientOperationExecutorTest {

    private static final RetryableErrorClassifier IO_RETRYABLE_CLASSIFIER =
        cause -> cause instanceof IOException || cause instanceof UncheckedIOException;

    private List<Long> recordedSleepDurations;
    private FileUploadResilientOperationExecutor executor;

    @BeforeEach
    void setUp() {
        recordedSleepDurations = new ArrayList<>();
        executor = new FileUploadResilientOperationExecutor(IO_RETRYABLE_CLASSIFIER) {
            @Override
            protected void sleep(long millis) {
                recordedSleepDurations.add(millis);
            }
        };
    }

    @Test
    void testSuccessfulFirstAttempt() throws Exception {
        var result = executor.execute(() -> "success");

        assertEquals("success", result);
        assertTrue(recordedSleepDurations.isEmpty());
    }

    @Test
    void testRetryOnTransientCauseInsideFileStorageException() throws Exception {
        var attempts = new AtomicInteger(0);

        var result = executor.execute(() -> {
            if (attempts.incrementAndGet() < 3) {
                throw new FileStorageException("Storage failed", new IOException("Connection reset"));
            }
            return "recovered";
        });

        assertEquals("recovered", result);
        assertEquals(2, recordedSleepDurations.size());
        assertEquals(10000L, recordedSleepDurations.get(0));
        assertEquals(20000L, recordedSleepDurations.get(1));
    }

    @Test
    void testNoRetryOnNonTransientCauseInsideFileStorageException() {
        var attempts = new AtomicInteger(0);

        assertThrows(FileStorageException.class, () -> executor.execute(() -> {
            attempts.incrementAndGet();
            throw new FileStorageException("Bad request", new IllegalArgumentException("invalid key"));
        }));

        assertEquals(1, attempts.get());
        assertTrue(recordedSleepDurations.isEmpty());
    }

    @Test
    void testNoRetryOnFileStorageExceptionWithoutCause() {
        var attempts = new AtomicInteger(0);

        assertThrows(FileStorageException.class, () -> executor.execute(() -> {
            attempts.incrementAndGet();
            throw new FileStorageException("no cause");
        }));

        assertEquals(1, attempts.get());
        assertTrue(recordedSleepDurations.isEmpty());
    }

    @Test
    void testNoRetryOnSLException() {
        var attempts = new AtomicInteger(0);

        var exception = assertThrows(SLException.class, () -> executor.execute(() -> {
            attempts.incrementAndGet();
            throw new SLException("Validation error");
        }));

        assertEquals(1, attempts.get());
        assertEquals("Validation error", exception.getMessage());
        assertTrue(recordedSleepDurations.isEmpty());
    }

    @Test
    void testRetryOnDirectIOException() throws Exception {
        var attempts = new AtomicInteger(0);

        var result = executor.execute(() -> {
            if (attempts.incrementAndGet() < 2) {
                throw new IOException("Network failure");
            }
            return "recovered";
        });

        assertEquals("recovered", result);
        assertEquals(1, recordedSleepDurations.size());
        assertEquals(10000L, recordedSleepDurations.get(0));
    }

    @Test
    void testExponentialBackoffTiming() throws Exception {
        var attempts = new AtomicInteger(0);

        var result = executor.execute(() -> {
            if (attempts.incrementAndGet() < 6) {
                throw new IOException("transient");
            }
            return "finally";
        });

        assertEquals("finally", result);
        assertEquals(5, recordedSleepDurations.size());
        assertEquals(10000L, recordedSleepDurations.get(0));
        assertEquals(20000L, recordedSleepDurations.get(1));
        assertEquals(40000L, recordedSleepDurations.get(2));
        assertEquals(80000L, recordedSleepDurations.get(3));
        assertEquals(160000L, recordedSleepDurations.get(4));
    }

    @Test
    void testAllAttemptsExhausted() {
        var attempts = new AtomicInteger(0);

        var exception = assertThrows(IOException.class, () -> executor.execute(() -> {
            attempts.incrementAndGet();
            throw new IOException("persistent failure");
        }));

        assertEquals(7, attempts.get());
        assertEquals("persistent failure", exception.getMessage());
        assertEquals(6, recordedSleepDurations.size());
    }

}
