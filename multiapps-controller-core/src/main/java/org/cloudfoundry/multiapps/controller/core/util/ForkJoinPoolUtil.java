package org.cloudfoundry.multiapps.controller.core.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;

public class ForkJoinPoolUtil {

    private ForkJoinPoolUtil() {
    }

    public static <T> T execute(int threads, Callable<T> callable) {
        ForkJoinPool customThreadPool = new ForkJoinPool(threads);
        try {
            return customThreadPool.submit(callable)
                                   .join();
        } finally {
            customThreadPool.shutdown();
        }
    }

    public static <T> T execute(int threads, Callable<T> callable, Function<Exception, T> errorConsumer) {
        return execute(threads, () -> {
            try {
                return callable.call();
            } catch (Exception e) {
                return errorConsumer.apply(e);
            }
        });
    }

}
