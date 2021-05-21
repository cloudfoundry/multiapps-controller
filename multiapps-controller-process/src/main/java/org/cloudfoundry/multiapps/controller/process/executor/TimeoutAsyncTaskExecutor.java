package org.cloudfoundry.multiapps.controller.process.executor;

import org.flowable.common.engine.impl.async.DefaultAsyncTaskExecutor;

import java.util.concurrent.Future;

public class TimeoutAsyncTaskExecutor extends DefaultAsyncTaskExecutor {

    private final CancelExpiredTasksThread cancelExpiredTasksThread;

    public TimeoutAsyncTaskExecutor(long timeoutInMillis, long waitTimeInMillis) {
        executorNeedsShutdown = true;
        cancelExpiredTasksThread = new CancelExpiredTasksThread(timeoutInMillis, waitTimeInMillis);
    }

    @Override
    public void execute(Runnable task) {
        Future<?> f = executorService.submit(task);
        cancelExpiredTasksThread.addTask(f);
    }

    @Override
    public void start() {
        super.start();
        cancelExpiredTasksThread.start();
    }

    @Override
    public void shutdown() {
        super.shutdown();
        cancelExpiredTasksThread.shutdown();
    }
}
