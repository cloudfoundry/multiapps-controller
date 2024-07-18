package org.cloudfoundry.multiapps.controller.web.monitoring;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.core.model.CachedObject;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;

import java.nio.file.Paths;
import java.time.Duration;

@Named
public class Metrics implements MetricsMBean {

    private final FssMonitor fssMonitor;
    private final CachedObject<FlowableThreadInformation> cachedFlowableThreadMonitor;
    private final CachedObject<CloudFoundryClientThreadInformation> cachedCloudFoundryClientThreadMonitor;
    private final FlowableJobExecutorInformation flowableJobExecutorInformation;
    private final FileUploadThreadPoolInformation fileUploadThreadPoolInformation;

    @Inject
    public Metrics(ApplicationConfiguration appConfigurations, FssMonitor fssMonitor,
                   FlowableJobExecutorInformation flowableJobExecutorInformation,
                   FileUploadThreadPoolInformation fileUploadThreadPoolInformation) {
        this.fssMonitor = fssMonitor;
        this.cachedFlowableThreadMonitor = new CachedObject<>(Duration.ofSeconds(appConfigurations.getThreadMonitorCacheUpdateInSeconds()));
        this.cachedCloudFoundryClientThreadMonitor = new CachedObject<>(
            Duration.ofSeconds(appConfigurations.getThreadMonitorCacheUpdateInSeconds()));
        this.flowableJobExecutorInformation = flowableJobExecutorInformation;
        this.fileUploadThreadPoolInformation = fileUploadThreadPoolInformation;
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
        return cachedFlowableThreadMonitor.getOrRefresh(FlowableThreadInformation::get);
    }

    private CloudFoundryClientThreadInformation getCloudFoundryThreadInformation() {
        return cachedCloudFoundryClientThreadMonitor.getOrRefresh(CloudFoundryClientThreadInformation::get);
    }

    @Override
    public int getCurrentJobExecutorQueueSize() {
        return flowableJobExecutorInformation.getCurrentJobExecutorQueueSize();
    }

    @Override
    public int getFileToUploadQueueSize() {
        return fileUploadThreadPoolInformation.getFileUploadPriorityBlockingQueueSize();
    }
}
