package org.cloudfoundry.multiapps.controller.client.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;

class ResilientCloudOperationExecutorTest {

    private ResilientCloudOperationExecutor executor;
    private List<Long> sleepCalls;
    private AtomicInteger attempts;

    @BeforeEach
    void setUp() {
        sleepCalls = new ArrayList<>();
        attempts = new AtomicInteger();
        executor = new ResilientCloudOperationExecutor().withRetryCount(3)
                                                        .withWaitTimeBetweenRetriesInMillis(0)
                                                        .withSleeper(sleepCalls::add);
    }

    @Test
    void testRetriesOnDefaultIgnoredStatuses() {
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

    static Stream<Arguments> retryAfterHeaderCappingCases() {
        return Stream.of(Arguments.of("below cap", 2L, 2_000L),
                         Arguments.of("above cap", 300L, 120_000L),
                         Arguments.of("at cap", 120L, 120_000L));
    }

    @ParameterizedTest(name = "{0}: retryAfter={1}s → sleep={2}ms")
    @MethodSource("retryAfterHeaderCappingCases")
    void testRetryAfterHeaderCapping(String description, long retryAfterSeconds, long expectedSleepMillis) {
        Supplier<String> operation = () -> {
            if (attempts.incrementAndGet() == 1) {
                throw new CloudOperationException(HttpStatus.TOO_MANY_REQUESTS,
                                                  HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                                                  null, null, retryAfterSeconds);
            }
            return "ok";
        };

        executor.execute(operation);

        Assertions.assertEquals(1, sleepCalls.size());
        Assertions.assertEquals(expectedSleepMillis, sleepCalls.get(0));
    }

    @Test
    void testRateLimitFallbackWhenRetryAfterAbsent() {
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
        executor = new ResilientCloudOperationExecutor().withRetryCount(3)
                                                        .withWaitTimeBetweenRetriesInMillis(5_000L)
                                                        .withSleeper(sleepCalls::add);
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
        executor = new ResilientCloudOperationExecutor().withRetryCount(4)
                                                        .withWaitTimeBetweenRetriesInMillis(0)
                                                        .withSleeper(sleepCalls::add)
                                                        .withRandomDelaySupplier(() -> deterministicDelay);
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
        executor = new ResilientCloudOperationExecutor().withRetryCount(3)
                                                        .withWaitTimeBetweenRetriesInMillis(0)
                                                        .withSleeper(sleepCalls::add)
                                                        .withRandomDelaySupplier(() -> 0L);
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
        executor = new ResilientCloudOperationExecutor().withRetryCount(3)
                                                        .withWaitTimeBetweenRetriesInMillis(7_000L)
                                                        .withSleeper(sleepCalls::add);
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

