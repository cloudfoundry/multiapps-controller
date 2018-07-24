package com.sap.cloud.lm.sl.cf.web.monitoring;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.persistence.services.FileSystemFileService;

@Component
public class Metrics implements MetricsMBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(Metrics.class);


    private ApplicationConfiguration appConfigurations;

    private FileSystemFileService fileSystemService;

    @Inject
    public Metrics(ApplicationConfiguration appConfigurations, FileSystemFileService fss) {
        this.appConfigurations = appConfigurations;
        this.fileSystemService = fss;
        if (fss == null) {
            LOGGER.info("No metrics for file system service will be collected - no such service found.");
        }
        LOGGER.info("Storage Path {} detected", getFssStoragePath());
    }

    private String getFssStoragePath() {
        if (fileSystemService == null) {
            return "";
        }
        return fileSystemService.getStoragePath();
    }

    private boolean shouldCollectCentralServiceMetrics() {
        return appConfigurations.getApplicationInstanceIndex() == 0;
    }

    @Override
    public double getUsedFssSpace() {
        if (!shouldCollectCentralServiceMetrics() || getFssStoragePath().isEmpty()) {
            LOGGER.debug("Not collecting metrics for FSS on path: {}", getFssStoragePath());
            return 0d;
        }
        return FssMonitor.instance.calculateUsedSpace(fileSystemService.getStoragePath());
    }

    @Override
    public double getUsedContainerSpace() {
        return FssMonitor.instance.calculateUsedSpace(".");
    }
}
