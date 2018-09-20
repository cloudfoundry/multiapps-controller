package com.sap.cloud.lm.sl.cf.web.monitoring;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.util.Hashtable;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;

@Component
public class FssMonitor {
    private static final Logger LOGGER = LoggerFactory.getLogger(FssMonitor.class);

    Map<String, Long> usedSpaceMap = new Hashtable<>(1);
    Map<String, LocalTime> updateTimesMap = new Hashtable<>(1);

    private Integer updateTimeoutMinutes;

    @Inject
    public FssMonitor(ApplicationConfiguration appConfigurations) {
        this.updateTimeoutMinutes = appConfigurations.getFssCacheUpdateTimeoutMinutes();
    }

    public long calculateUsedSpace(String path) {
        if (!updateTimesMap.containsKey(path)) {
            return getUsedSpace(path);
        }
        if (isCacheValid(path)) {
            return usedSpaceMap.get(path);
        }
        return getUsedSpace(path);
    }

    private boolean isCacheValid(String path) {
        LocalTime lastChecked = updateTimesMap.get(path);
        LocalTime invalidateDeadline = LocalTime.now()
            .minusMinutes(updateTimeoutMinutes);
        return invalidateDeadline.isBefore(lastChecked);
    }

    private long getUsedSpace(String path) {
        updateTimesMap.put(path, LocalTime.now());
        long usedSpace = 0L;
        try {
            Path filePath = Paths.get(path);
            usedSpace = Files.walk(filePath)
                .mapToLong(p -> p.toFile()
                    .length())
                .sum();
        } catch (IOException | InvalidPathException e) {
            LOGGER.warn("Cannot detect remaining space on file system service.", e);
        }
        usedSpaceMap.put(path, usedSpace);
        return usedSpace;
    }
}
