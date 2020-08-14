package org.cloudfoundry.multiapps.controller.core.util;

import java.util.function.Consumer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SafeExecutorTest {

    @SuppressWarnings("unchecked")
    private final Consumer<Exception> exceptionHandler = Mockito.mock(Consumer.class);

    @Test
    void testWithoutExceptions() {
        SafeExecutor safeExecutor = new SafeExecutor(exceptionHandler);
        safeExecutor.execute(() -> {
        });
        Mockito.verifyNoInteractions(exceptionHandler);
    }

    @Test
    void testWithException() {
        SafeExecutor safeExecutor = new SafeExecutor(exceptionHandler);
        Exception e = new Exception();
        safeExecutor.execute(() -> {
            throw e;
        });
        Mockito.verify(exceptionHandler)
               .accept(e);
    }

    @Test
    void testWithDefaultExceptionHandler() {
        SafeExecutor safeExecutor = new SafeExecutor();
        Exception e = new Exception();
        Assertions.assertDoesNotThrow(() -> safeExecutor.execute(() -> {
            throw e;
        }));
    }

}
