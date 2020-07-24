package org.cloudfoundry.multiapps.controller.web.monitoring;

import java.lang.Thread.State;

public class CloudFoundryClientThreadInformation extends ThreadInformation {

    private static final String PREFIX = "cloudfoundry-client-";

    private int runningThreads = 0;
    private int totalThreads = 0;

    private CloudFoundryClientThreadInformation() {
    }

    public static CloudFoundryClientThreadInformation get() {
        CloudFoundryClientThreadInformation threadMonitor = new CloudFoundryClientThreadInformation();
        threadMonitor.processThreadsInformation();
        return threadMonitor;
    }

    @Override
    protected void processThreadInformation(String name, State state) {
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
