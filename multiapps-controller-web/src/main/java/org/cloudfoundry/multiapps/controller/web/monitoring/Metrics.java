package org.cloudfoundry.multiapps.controller.web.monitoring;

import org.cloudfoundry.multiapps.controller.core.model.CachedObject;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;

import java.nio.file.Paths;
import javax.inject.Inject;
import javax.inject.Named;

@Named
public class Metrics implements MetricsMBean {

    private final FssMonitor fssMonitor;
    private final CachedObject<FlowableThreadInformation> cachedFlowableThreadMonitor;
    private final CachedObject<CloudFoundryClientThreadInformation> cachedCloudFoundryClientThreadMonitor;
    private final FlowableJobExecutorInformation flowableJobExecutorInformation;

    @Inject
    public Metrics(ApplicationConfiguration appConfigurations, FssMonitor fssMonitor,
                   FlowableJobExecutorInformation flowableJobExecutorInformation) {
        this.fssMonitor = fssMonitor;
        this.cachedFlowableThreadMonitor = new CachedObject<>(appConfigurations.getThreadMonitorCacheUpdateInSeconds());
        this.cachedCloudFoundryClientThreadMonitor = new CachedObject<>(appConfigurations.getThreadMonitorCacheUpdateInSeconds());
        this.flowableJobExecutorInformation = flowableJobExecutorInformation;
    }

    @Override
    public long getUsedContainerSpace() {
        String workDir = System.getProperty("user.dir");
        String parentDir = Paths.get(workDir)
                                .getParent()
                                .toString();
        return fssMonitor.calculateUsedSpace(parentDir);
    }

    @Override
    public int getRunningJobExecutorThreads() {
        return getFlowableThreadInformation().getRunningJobExecutorThreads();
    }

    @Override
    public int getTotalJobExecutorThreads() {
        return getFlowableThreadInformation().getTotalJobExecutorThreads();
    }

    @Override
    public int getRunningAsyncExecutorThreads() {
        return getFlowableThreadInformation().getRunningAsyncExecutorThreads();
    }

    @Override
    public int getTotalAsyncExecutorThreads() {
        return getFlowableThreadInformation().getTotalAsyncExecutorThreads();
    }

    @Override
    public int getRunningCloudFoundryClientThreads() {
        return getCloudFoundryThreadInformation().getRunningThreads();
    }

    @Override
    public int getTotalCloudFoundryClientThreads() {
        return getCloudFoundryThreadInformation().getTotalThreads();
    }

    private FlowableThreadInformation getFlowableThreadInformation() {
        return cachedFlowableThreadMonitor.get(FlowableThreadInformation::get);
    }

    private CloudFoundryClientThreadInformation getCloudFoundryThreadInformation() {
        return cachedCloudFoundryClientThreadMonitor.get(CloudFoundryClientThreadInformation::get);
    }

    @Override
    public int getCurrentJobExecutorQueueSize() {
        return flowableJobExecutorInformation.getCurrentJobExecutorQueueSize();
    }

}
