package com.sap.cloud.lm.sl.cf.web.monitoring;

public interface MetricsMBean {

    long getUsedFssSpace();

    long getUsedContainerSpace();

    int getRunningJobExecutorThreads();

    int getTotalJobExecutorThreads();

    int getRunningAsyncExecutorThreads();

    int getTotalAsyncExecutorThreads();

    int getRunningCloudFoundryClientThreads();

    int getTotalCloudFoundryClientThreads();

    int getCurrentJobExecutorQueueSize();

}
