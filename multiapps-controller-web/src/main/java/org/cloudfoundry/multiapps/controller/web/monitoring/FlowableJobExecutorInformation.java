package org.cloudfoundry.multiapps.controller.web.monitoring;

import java.time.Instant;

import javax.inject.Inject;
import javax.inject.Named;

import org.flowable.spring.SpringProcessEngineConfiguration;

@Named
public class FlowableJobExecutorInformation {

    private static final int CACHE_TIMEOUT_IN_SECONDS = 60;

    protected Instant lastUpdateTime;

    private SpringProcessEngineConfiguration processEngineConfiguration;

    private int currentJobExecutorQueueSize = 0;

    @Inject
    public FlowableJobExecutorInformation(SpringProcessEngineConfiguration processEngineConfiguration) {
        this.processEngineConfiguration = processEngineConfiguration;
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
        currentJobExecutorQueueSize = processEngineConfiguration.getAsyncExecutorThreadPoolQueue()
                                                                .size();
        updateTime();
    }

    protected void updateTime() {
        lastUpdateTime = Instant.now();
    }

}
