package com.sap.cloud.lm.sl.cf.web.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.Map;

public abstract class ThreadInformation {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThreadInformation.class);

    protected void processThreadsInformation() {
        LOGGER.trace("Fetching stack trace information...");
        long beforeTime = System.currentTimeMillis();
        Map<Thread, StackTraceElement[]> allThreads = Thread.getAllStackTraces();
        LOGGER.trace(MessageFormat.format("Stack trace information retrieved in {0} milliseconds.",
                                          System.currentTimeMillis() - beforeTime));
        for (Thread thread : allThreads.keySet()) {
            processThreadInformation(thread.getName(), thread.getState());
        }
    }

    protected abstract void processThreadInformation(String name, Thread.State state);

}
