package org.cloudfoundry.multiapps.controller.core.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;

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

}
