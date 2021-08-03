package org.cloudfoundry.multiapps.controller.process.executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

public class CancelExpiredTasksThread extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(CancelExpiredTasksThread.class);
    private Map<Future<?>, Long> tasksWithStartTime = new ConcurrentHashMap<>();
    private static volatile boolean isActive = true;
    private long cancelWaitTimeInMillis;
    private long cancelTimeoutInMillis;

    public CancelExpiredTasksThread(long cancelTimeoutInMillis, long cancelWaitTimeInMillis) {
        this.cancelTimeoutInMillis = cancelTimeoutInMillis;
        this.cancelWaitTimeInMillis = cancelWaitTimeInMillis;
    }

    @Override
    public void run() {
        try {
            while (isActive) {
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
        tasksWithStartTime.put(task, System.currentTimeMillis());
    }

    public void shutdown() {
        isActive = false;
    }

    private void removeDoneTasks() {
        tasksWithStartTime.keySet()
                      .removeIf(Future::isDone);
    }

    private void cancelTimeoutTasks() {
        long now = System.currentTimeMillis();
        tasksWithStartTime.forEach((task, startTime) -> {
            if (shouldBeCanceled(startTime, now)) {
                task.cancel(true);
            }
        });
    }

    private boolean shouldBeCanceled(long startTime, long now) {
        return startTime + cancelTimeoutInMillis <= now;
    }
}
