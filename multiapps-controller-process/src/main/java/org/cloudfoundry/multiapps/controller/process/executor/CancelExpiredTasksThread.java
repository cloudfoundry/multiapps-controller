package org.cloudfoundry.multiapps.controller.process.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public class CancelExpiredTasksThread extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(CancelExpiredTasksThread.class);
    private Map<Future<?>, Long> tasksStartTime = new ConcurrentHashMap<>();
    private AtomicBoolean isActive = new AtomicBoolean(true);
    private long cancelWaitTimeInMillis;
    private long cancelTimeoutInMillis;

    public CancelExpiredTasksThread(long cancelTimeoutInMillis, long cancelWaitTimeInMillis) {
        this.cancelTimeoutInMillis = cancelTimeoutInMillis;
        this.cancelWaitTimeInMillis = cancelWaitTimeInMillis;
    }

    @Override
    public void run() {
        try {
            while (isActive.get()) {
                removeDoneTasks();
                cancelTimeoutTasks();
                Thread.sleep(cancelWaitTimeInMillis);
            }
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage(), e);
            Thread.currentThread()
                  .interrupt();
        }
    }

    public void addTask(Future<?> task) {
        tasksStartTime.put(task, System.currentTimeMillis());
    }

    public void shutdown() {
        isActive.set(false);
    }

    private void removeDoneTasks() {
        tasksStartTime.keySet()
                      .removeIf(Future::isDone);
    }

    private void cancelTimeoutTasks() {
        long now = System.currentTimeMillis();
        tasksStartTime.forEach((task, startTime) -> {
            if (shouldBeCanceled(startTime, now)) {
                task.cancel(true);
            }
        });
    }

    private boolean shouldBeCanceled(long startTime, long now) {
        return startTime + cancelTimeoutInMillis <= now;
    }
}
