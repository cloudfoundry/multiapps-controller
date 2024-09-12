package org.cloudfoundry.multiapps.controller.process.util;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class PriorityFuture<T> implements RunnableFuture<T> {

    private final RunnableFuture<T> src;
    private final int priority;

    public PriorityFuture(RunnableFuture<T> other, int priority) {
        this.src = other;
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return src.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return src.isCancelled();
    }

    @Override
    public boolean isDone() {
        return src.isDone();
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        return src.get();
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return src.get(timeout, unit);
    }

    @Override
    public void run() {
        src.run();
    }

    public enum Priority {

        HIGHEST(1), MODERATE(2), LOWEST(3);

        private final int value;

        Priority(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }
}
