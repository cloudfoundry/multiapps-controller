package org.cloudfoundry.multiapps.controller.client.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ResilientCloudOperationExecutorTest {

    private ResilientCloudOperationExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new ResilientCloudOperationExecutor().withRetryCount(3)
                                                        .withWaitTimeBetweenRetriesInMillis(0);
    }

    @Test
    void testRetriesOnDefaultIgnoredStatuses() {
        AtomicInteger attempts = new AtomicInteger();
        Supplier<String> operation = () -> {
            if (attempts.incrementAndGet() < 2) {
                throw new CloudOperationException(HttpStatus.BAD_GATEWAY);
            }
            return "ok";
        };

        String result = executor.execute(operation);

        Assertions.assertEquals("ok", result);
        Assertions.assertEquals(2, attempts.get());
    }

    @Test
    void testThrowsImmediatelyOnNonIgnoredStatus() {
        AtomicInteger attempts = new AtomicInteger();
        Supplier<String> operation = () -> {
            attempts.incrementAndGet();
            throw new CloudOperationException(HttpStatus.NOT_FOUND);
        };

        CloudOperationException thrown = Assertions.assertThrows(CloudOperationException.class, () -> executor.execute(operation));
        Assertions.assertEquals(HttpStatus.NOT_FOUND, thrown.getStatusCode());
        Assertions.assertEquals(1, attempts.get());
    }

    @Test
    void testWithStatusesToIgnoreAddsAdditionalRetryableStatuses() {
        AtomicInteger attempts = new AtomicInteger();
        Supplier<String> operation = () -> {
            if (attempts.incrementAndGet() < 2) {
                throw new CloudOperationException(HttpStatus.NOT_FOUND);
            }
            return "ok";
        };

        executor = (ResilientCloudOperationExecutor) executor.withStatusesToIgnore(HttpStatus.NOT_FOUND);

        String result = executor.execute(operation);

        Assertions.assertEquals("ok", result);
        Assertions.assertEquals(2, attempts.get());
    }

    @Test
    void testFluentBuildersReturnSameTypeForChaining() {
        ResilientCloudOperationExecutor chained = new ResilientCloudOperationExecutor().withRetryCount(2)
                                                                                       .withWaitTimeBetweenRetriesInMillis(0)
                                                                                       .withStatusesToIgnore(HttpStatus.I_AM_A_TEAPOT);
        Assertions.assertNotNull(chained);
    }

    @Test
    void testRetryAfterHeaderIsHonouredOnRateLimit() {
        List<Long> sleepCalls = new ArrayList<>();
        executor = new ResilientCloudOperationExecutor().withRetryCount(3)
                                                        .withSleeper(sleepCalls::add);
        AtomicInteger attempts = new AtomicInteger();
        Supplier<String> operation = () -> {
            if (attempts.incrementAndGet() == 1) {
                throw new CloudOperationException(HttpStatus.TOO_MANY_REQUESTS,
                                                  HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                                                  null, null, 2L);
            }
            return "ok";
        };

        executor.execute(operation);

        Assertions.assertEquals(1, sleepCalls.size());
        Assertions.assertEquals(2_000L, sleepCalls.get(0));
    }

    @Test
    void testRetryAfterHeaderCappedAtMaximum() {
        List<Long> sleepCalls = new ArrayList<>();
        executor = new ResilientCloudOperationExecutor().withRetryCount(3)
                                                        .withSleeper(sleepCalls::add);
        AtomicInteger attempts = new AtomicInteger();
        Supplier<String> operation = () -> {
            if (attempts.incrementAndGet() == 1) {
                throw new CloudOperationException(HttpStatus.TOO_MANY_REQUESTS,
                                                  HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                                                  null, null, 300L);
            }
            return "ok";
        };

        executor.execute(operation);

        Assertions.assertEquals(1, sleepCalls.size());
        Assertions.assertEquals(120_000L, sleepCalls.get(0));
    }

    @Test
    void testRetryAfterHeaderAtCapIsNotReduced() {
        List<Long> sleepCalls = new ArrayList<>();
        executor = new ResilientCloudOperationExecutor().withRetryCount(3)
                                                        .withSleeper(sleepCalls::add);
        AtomicInteger attempts = new AtomicInteger();
        Supplier<String> operation = () -> {
            if (attempts.incrementAndGet() == 1) {
                throw new CloudOperationException(HttpStatus.TOO_MANY_REQUESTS,
                                                  HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                                                  null, null, 120L);
            }
            return "ok";
        };

        executor.execute(operation);

        Assertions.assertEquals(1, sleepCalls.size());
        Assertions.assertEquals(120_000L, sleepCalls.get(0));
    }

    @Test
    void testRateLimitFallbackWhenRetryAfterAbsent() {
        List<Long> sleepCalls = new ArrayList<>();
        executor = new ResilientCloudOperationExecutor().withRetryCount(3)
                                                        .withSleeper(sleepCalls::add);
        AtomicInteger attempts = new AtomicInteger();
        Supplier<String> operation = () -> {
            if (attempts.incrementAndGet() == 1) {
                throw new CloudOperationException(HttpStatus.TOO_MANY_REQUESTS,
                                                  HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                                                  null, null, null);
            }
            return "ok";
        };

        executor.execute(operation);

        Assertions.assertEquals(1, sleepCalls.size());
        Assertions.assertEquals(60_000L, sleepCalls.get(0));
    }

    @Test
    void testNon429UsesExistingFixedDelay() {
        List<Long> sleepCalls = new ArrayList<>();
        executor = new ResilientCloudOperationExecutor().withRetryCount(3)
                                                        .withWaitTimeBetweenRetriesInMillis(5_000L)
                                                        .withSleeper(sleepCalls::add);
        AtomicInteger attempts = new AtomicInteger();
        Supplier<String> operation = () -> {
            if (attempts.incrementAndGet() == 1) {
                throw new CloudOperationException(HttpStatus.BAD_GATEWAY);
            }
            return "ok";
        };

        executor.execute(operation);

        Assertions.assertEquals(1, sleepCalls.size());
        Assertions.assertEquals(5_000L, sleepCalls.get(0));
    }

    @Test
    void testRandomDelayAppliedForNon429AfterFirstRetry() {
        long deterministicDelay = 45_000L;
        List<Long> sleepCalls = new ArrayList<>();
        executor = new ResilientCloudOperationExecutor().withRetryCount(4)
                                                        .withWaitTimeBetweenRetriesInMillis(0)
                                                        .withSleeper(sleepCalls::add)
                                                        .withRandomDelaySupplier(() -> deterministicDelay);
        AtomicInteger attempts = new AtomicInteger();
        Supplier<String> operation = () -> {
            if (attempts.incrementAndGet() < 3) {
                throw new CloudOperationException(HttpStatus.INTERNAL_SERVER_ERROR);
            }
            return "ok";
        };

        executor.execute(operation);

        Assertions.assertEquals(2, sleepCalls.size());
        Assertions.assertEquals(0L, sleepCalls.get(0));
        Assertions.assertEquals(deterministicDelay, sleepCalls.get(1));
    }

    @Test
    void testRetryableOperationThatNeverSucceedsIsEventuallyRethrown() {
        List<Long> sleepCalls = new ArrayList<>();
        executor = new ResilientCloudOperationExecutor().withRetryCount(3)
                                                        .withWaitTimeBetweenRetriesInMillis(0)
                                                        .withSleeper(sleepCalls::add)
                                                        .withRandomDelaySupplier(() -> 0L);
        AtomicInteger attempts = new AtomicInteger();
        Supplier<String> operation = () -> {
            attempts.incrementAndGet();
            throw new CloudOperationException(HttpStatus.BAD_GATEWAY);
        };

        Assertions.assertThrows(CloudOperationException.class, () -> executor.execute(operation));
        Assertions.assertEquals(3, attempts.get());
        Assertions.assertEquals(2, sleepCalls.size());
    }

    @Test
    void testRunnableOverloadIsRetriedThroughOverriddenExecute() {
        List<Long> sleepCalls = new ArrayList<>();
        executor = new ResilientCloudOperationExecutor().withRetryCount(3)
                                                        .withWaitTimeBetweenRetriesInMillis(7_000L)
                                                        .withSleeper(sleepCalls::add);
        AtomicInteger attempts = new AtomicInteger();
        Runnable operation = () -> {
            if (attempts.incrementAndGet() < 2) {
                throw new CloudOperationException(HttpStatus.SERVICE_UNAVAILABLE);
            }
        };

        executor.execute(operation);

        Assertions.assertEquals(2, attempts.get());
        Assertions.assertEquals(1, sleepCalls.size());
        Assertions.assertEquals(7_000L, sleepCalls.get(0));
    }
}

