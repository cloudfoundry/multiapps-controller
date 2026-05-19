package org.cloudfoundry.multiapps.controller.client.util;

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
}
