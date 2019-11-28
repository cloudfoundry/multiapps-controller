package com.sap.cloud.lm.sl.cf.web.monitoring;

import java.nio.file.Paths;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.core.model.CachedObject;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.persistence.services.FileSystemFileStorage;

@Named
public class Metrics implements MetricsMBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(Metrics.class);

    private final ApplicationConfiguration appConfigurations;
    private final FileSystemFileStorage fileSystemStorage;
    private final FssMonitor fssMonitor;
    private final CachedObject<FlowableThreadInformation> cachedFlowableThreadMonitor;
    private final CachedObject<CloudFoundryClientThreadInformation> cachedCloudFoundryClientThreadMonitor;

    @Inject
    public Metrics(ApplicationConfiguration appConfigurations, FssMonitor fssMonitor, FileSystemFileStorage fss) {
        this.appConfigurations = appConfigurations;
        this.fssMonitor = fssMonitor;
        this.fileSystemStorage = fss;
        this.cachedFlowableThreadMonitor = new CachedObject<>(appConfigurations.getThreadMonitorCacheUpdateInSeconds());
        this.cachedCloudFoundryClientThreadMonitor = new CachedObject<>(appConfigurations.getThreadMonitorCacheUpdateInSeconds());
        if (fss == null) {
            LOGGER.info("No metrics for file system service will be collected - no such service found.");
        }
        LOGGER.info("Storage Path {} detected", getFssStoragePath());
    }

    private String getFssStoragePath() {
        if (fileSystemStorage == null) {
            return "";
        }
        return fileSystemStorage.getStoragePath();
    }

    private boolean shouldCollectCentralServiceMetrics() {
        return appConfigurations.getApplicationInstanceIndex() == 0;
    }

    @Override
    public long getUsedFssSpace() {
        if (!shouldCollectCentralServiceMetrics() || getFssStoragePath().isEmpty()) {
            LOGGER.debug("Not collecting metrics for FSS on path: {}", getFssStoragePath());
            return 0L;
        }
        return fssMonitor.calculateUsedSpace(fileSystemStorage.getStoragePath());
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
        return cachedFlowableThreadMonitor.get(() -> FlowableThreadInformation.get());
    }

    private CloudFoundryClientThreadInformation getCloudFoundryThreadInformation() {
        return cachedCloudFoundryClientThreadMonitor.get(() -> CloudFoundryClientThreadInformation.get());
    }

}
