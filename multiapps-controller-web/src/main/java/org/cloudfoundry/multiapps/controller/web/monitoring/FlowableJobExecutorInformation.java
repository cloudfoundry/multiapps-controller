package org.cloudfoundry.multiapps.controller.web.monitoring;

import java.time.Instant;

import javax.inject.Inject;
import javax.inject.Named;

import org.flowable.job.service.impl.asyncexecutor.DefaultAsyncJobExecutor;

@Named
public class FlowableJobExecutorInformation {

    private static final int CACHE_TIMEOUT_IN_SECONDS = 60;

    protected Instant lastUpdateTime;

    private DefaultAsyncJobExecutor jobExecutor;

    private int currentJobExecutorQueueSize = 0;

    @Inject
    public FlowableJobExecutorInformation(DefaultAsyncJobExecutor jobExecutor) {
        this.jobExecutor = jobExecutor;
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
        currentJobExecutorQueueSize = jobExecutor.getThreadPoolQueue()
                                                 .size();
        updateTime();
    }

    protected void updateTime() {
        lastUpdateTime = Instant.now();
    }

}
