package org.cloudfoundry.multiapps.controller.process.util;

import java.util.concurrent.Callable;

public class PriorityCallable<T> implements Callable<T> {

    private final PriorityFuture.Priority priority;
    private final Callable<T> callable;

    public PriorityCallable(PriorityFuture.Priority priority, Callable<T> callable) {
        this.priority = priority;
        this.callable = callable;
    }

    @Override
    public T call() throws Exception {
        return callable.call();
    }

    public PriorityFuture.Priority getPriority() {
        return priority;
    }
}