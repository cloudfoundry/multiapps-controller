package org.cloudfoundry.multiapps.controller.web.monitoring;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.FileUtils;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class FssMonitor {

    final Map<File, Long> usedSpaceMap = new ConcurrentHashMap<>(1);
    final Map<File, LocalDateTime> updateTimesMap = new ConcurrentHashMap<>(1);
    private static final Logger LOGGER = LoggerFactory.getLogger(FssMonitor.class);

    private final Integer updateTimeoutMinutes;

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
        LOGGER.debug("Calculating space for path {}.", path);
        updateTimesMap.put(path, LocalDateTime.now());
        long startTime = System.currentTimeMillis();
        long usedSpace = FileUtils.sizeOf(path);
        long endTime = System.currentTimeMillis();
        LOGGER.info("Calculated space for path {} : {} bytes in {} ms", path, usedSpace, endTime - startTime);
        usedSpaceMap.put(path, usedSpace);
        return usedSpace;
    }
}
