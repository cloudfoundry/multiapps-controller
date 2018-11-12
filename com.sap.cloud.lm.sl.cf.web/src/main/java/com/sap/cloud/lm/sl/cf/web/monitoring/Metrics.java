package com.sap.cloud.lm.sl.cf.web.monitoring;

import java.nio.file.Paths;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.persistence.services.FileSystemFileStorage;

@Component
public class Metrics implements MetricsMBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(Metrics.class);

    private ApplicationConfiguration appConfigurations;

    private FileSystemFileStorage fileSystemStorage;

    private FssMonitor fssMonitor;

    @Inject
    public Metrics(ApplicationConfiguration appConfigurations, FssMonitor fssMonitor, FileSystemFileStorage fss) {
        this.appConfigurations = appConfigurations;
        this.fssMonitor = fssMonitor;
        this.fileSystemStorage = fss;
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
}
