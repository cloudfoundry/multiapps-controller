package org.cloudfoundry.multiapps.controller.web.monitoring;

import java.time.Instant;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.flowable.common.engine.impl.async.DefaultAsyncTaskExecutor;

@Named
public class FlowableJobExecutorInformation {

    private static final int CACHE_TIMEOUT_IN_SECONDS = 60;

    protected Instant lastUpdateTime;

    private DefaultAsyncTaskExecutor taskExecutor;

    private int currentJobExecutorQueueSize = 0;

    @Inject
    public FlowableJobExecutorInformation(DefaultAsyncTaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    public int getCurrentJobExecutorQueueSize() {
        if (lastUpdateTime == null || isPastCacheTimeout()) {
            updateCurrentJobExecutorQueueSize();
        }
        return currentJobExecutorQueueSize;
    }

    private boolean isPastCacheTimeout() {
        return Instant.now()
                      .minusSeconds(CACHE_TIMEOUT_IN_SECONDS)
                      .isAfter(lastUpdateTime);
    }

    private void updateCurrentJobExecutorQueueSize() {
        currentJobExecutorQueueSize = taskExecutor.getThreadPoolQueue()
                                                  .size();
        updateTime();
    }

    protected void updateTime() {
        lastUpdateTime = Instant.now();
    }

}
