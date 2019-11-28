package com.sap.cloud.lm.sl.cf.web.monitoring;

import java.lang.Thread.State;
import java.text.MessageFormat;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudFoundryClientThreadInformation {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudFoundryClientThreadInformation.class);

    private static final String PREFIX = "cloudfoundry-client-";

    private int runningThreads;
    private int totalThreads;

    private CloudFoundryClientThreadInformation() {
    }

    public static CloudFoundryClientThreadInformation get() {
        CloudFoundryClientThreadInformation threadMonitor = new CloudFoundryClientThreadInformation();
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
        if (name.startsWith(PREFIX)) {
            totalThreads++;
            if (state.equals(State.RUNNABLE)) {
                runningThreads++;
            }
        }
    }

    public int getRunningThreads() {
        return runningThreads;
    }

    public int getTotalThreads() {
        return totalThreads;
    }

}
