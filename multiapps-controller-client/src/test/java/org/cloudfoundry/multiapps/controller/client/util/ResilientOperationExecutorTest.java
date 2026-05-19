package org.cloudfoundry.multiapps.controller.client.util;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ResilientOperationExecutorTest {

    private ResilientOperationExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new ResilientOperationExecutor().withRetryCount(3)
                                                   .withWaitTimeBetweenRetriesInMillis(0);
    }

    @Test
    void testExecuteSupplierReturnsValueWhenOperationSucceedsFirstTry() {
        Supplier<String> operation = () -> "ok";

        String result = executor.execute(operation);

        Assertions.assertEquals("ok", result);
    }

    @Test
    void testExecuteSupplierRetriesUntilOperationSucceeds() {
        AtomicInteger attempts = new AtomicInteger();
        Supplier<String> operation = () -> {
            if (attempts.incrementAndGet() < 2) {
                throw new RuntimeException("transient");
            }
            return "ok-after-retry";
        };

        String result = executor.execute(operation);

        Assertions.assertEquals("ok-after-retry", result);
        Assertions.assertEquals(2, attempts.get());
    }

    @Test
    void testExecuteSupplierPropagatesExceptionWhenRetriesExhausted() {
        AtomicInteger attempts = new AtomicInteger();
        Supplier<String> operation = () -> {
            attempts.incrementAndGet();
            throw new RuntimeException("permanent");
        };

        RuntimeException thrown = Assertions.assertThrows(RuntimeException.class, () -> executor.execute(operation));
        Assertions.assertEquals("permanent", thrown.getMessage());
        Assertions.assertEquals(3, attempts.get());
    }

    @Test
    void testExecuteRunnableInvokesOperation() {
        AtomicInteger attempts = new AtomicInteger();

        executor.execute((Runnable) attempts::incrementAndGet);

        Assertions.assertEquals(1, attempts.get());
    }

    @Test
    void testExecuteCheckedSupplierRetriesAndReturnsValue() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        CheckedSupplier<String> operation = () -> {
            if (attempts.incrementAndGet() < 2) {
                throw new Exception("checked-transient");
            }
            return "ok-checked";
        };

        String result = executor.execute(operation);

        Assertions.assertEquals("ok-checked", result);
        Assertions.assertEquals(2, attempts.get());
    }
}
