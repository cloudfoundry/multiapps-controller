package com.sap.cloud.lm.sl.cf.web.monitoring;

import java.lang.Thread.State;

public class FlowableThreadInformation extends ThreadInformation {

    private static final String JOB_EXECUTOR_PREFIX = "flowable-async-job-executor-thread-";
    private static final String ASYNC_EXECUTOR_PREFIX = "asyncExecutor-";

    private int runningJobExecutors = 0;
    private int totalJobExecutors = 0;
    private int runningAsyncExecutors = 0;
    private int totalAsyncExecutors = 0;

    private FlowableThreadInformation() {
    }

    public static FlowableThreadInformation get() {
        FlowableThreadInformation threadMonitor = new FlowableThreadInformation();
        threadMonitor.processThreadsInformation();
        return threadMonitor;
    }

    @Override
    protected void processThreadInformation(String name, State state) {
        if (name.startsWith(JOB_EXECUTOR_PREFIX)) {
            totalJobExecutors++;
            if (state.equals(State.RUNNABLE)) {
                runningJobExecutors++;
            }
        }
        if (name.startsWith(ASYNC_EXECUTOR_PREFIX)) {
            totalAsyncExecutors++;
            if (state.equals(State.RUNNABLE)) {
                runningAsyncExecutors++;
            }
        }
    }

    public int getTotalAsyncExecutorThreads() {
        return totalAsyncExecutors;
    }

    public int getRunningAsyncExecutorThreads() {
        return runningAsyncExecutors;
    }

    public int getTotalJobExecutorThreads() {
        return totalJobExecutors;
    }

    public int getRunningJobExecutorThreads() {
        return runningJobExecutors;
    }

}
