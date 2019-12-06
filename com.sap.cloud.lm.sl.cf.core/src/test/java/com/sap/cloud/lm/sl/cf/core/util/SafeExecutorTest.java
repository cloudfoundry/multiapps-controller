package com.sap.cloud.lm.sl.cf.core.util;

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class SafeExecutorTest {

    @SuppressWarnings("unchecked")
    private final Consumer<Exception> exceptionHandler = Mockito.mock(Consumer.class);

    @Test
    public void testWithoutExceptions() {
        SafeExecutor safeExecutor = new SafeExecutor(exceptionHandler);
        safeExecutor.execute(() -> {
        });
        Mockito.verifyNoInteractions(exceptionHandler);
    }

    @Test
    public void testWithException() {
        SafeExecutor safeExecutor = new SafeExecutor(exceptionHandler);
        Exception e = new Exception();
        safeExecutor.execute(() -> {
            throw e;
        });
        Mockito.verify(exceptionHandler)
               .accept(e);
    }

    @Test
    public void testWithDefaultExceptionHandler() {
        SafeExecutor safeExecutor = new SafeExecutor();
        Exception e = new Exception();
        safeExecutor.execute(() -> {
            throw e;
        });
    }

}
