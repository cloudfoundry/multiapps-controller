package com.sap.cloud.lm.sl.cf.web.monitoring;

import java.lang.Thread.State;
import java.text.MessageFormat;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowableThreadInformation {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowableThreadInformation.class);

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
        LOGGER.trace("Fetching stack trace information...");
        long beforeTime = System.currentTimeMillis();
        Map<Thread, StackTraceElement[]> allThreads = Thread.getAllStackTraces();
        LOGGER.trace(MessageFormat.format("Stack trace information retrieved in {0} milliseconds.",
                                          System.currentTimeMillis() - beforeTime));
        for (Thread thread : allThreads.keySet()) {
            threadMonitor.processThreadInformation(thread.getName(), thread.getState());
        }
        return threadMonitor;
    }

    private void processThreadInformation(String name, State state) {
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
