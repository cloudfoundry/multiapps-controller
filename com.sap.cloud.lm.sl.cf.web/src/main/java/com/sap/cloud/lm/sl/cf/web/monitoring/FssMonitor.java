package com.sap.cloud.lm.sl.cf.web.monitoring;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;

@Component
public class FssMonitor {

    Map<File, Long> usedSpaceMap = new ConcurrentHashMap<>(1);
    Map<File, LocalDateTime> updateTimesMap = new ConcurrentHashMap<>(1);
    private static final Logger LOGGER = LoggerFactory.getLogger(FssMonitor.class);

    private Integer updateTimeoutMinutes;

    @Inject
    public FssMonitor(ApplicationConfiguration appConfigurations) {
        this.updateTimeoutMinutes = appConfigurations.getFssCacheUpdateTimeoutMinutes();
    }

    public long calculateUsedSpace(String path) {
        File filePath = new File(path);
        if (!updateTimesMap.containsKey(filePath)) {
            return getUsedSpace(filePath);
        }
        if (isCacheValid(filePath)) {
            return usedSpaceMap.get(filePath);
        }
        return getUsedSpace(filePath);
    }

    private boolean isCacheValid(File filePath) {
        LocalDateTime lastChecked = updateTimesMap.get(filePath);
        LocalDateTime invalidateDeadline = LocalDateTime.now()
                                                        .minusMinutes(updateTimeoutMinutes);
        return invalidateDeadline.isBefore(lastChecked);
    }

    private long getUsedSpace(File path) {
        final long startTime = System.currentTimeMillis();
        LOGGER.debug("Calculating space for path {}.", path);
        updateTimesMap.put(path, LocalDateTime.now());
        long usedSpace = FileUtils.sizeOf(path);
        usedSpaceMap.put(path, usedSpace);
        LOGGER.info("Calculated space for path {} : {} bytes in {} ms", path, usedSpace, System.currentTimeMillis() - startTime);
        return usedSpace;
    }
}
