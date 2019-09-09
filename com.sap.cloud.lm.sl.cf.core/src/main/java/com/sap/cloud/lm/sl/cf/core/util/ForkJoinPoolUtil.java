package com.sap.cloud.lm.sl.cf.core.util;

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

    public static void execute(int threads, Runnable runnable) {
        ForkJoinPool customThreadPool = new ForkJoinPool(threads);
        try {
            customThreadPool.submit(runnable)
                            .join();
        } finally {
            customThreadPool.shutdown();
        }
    }
}
