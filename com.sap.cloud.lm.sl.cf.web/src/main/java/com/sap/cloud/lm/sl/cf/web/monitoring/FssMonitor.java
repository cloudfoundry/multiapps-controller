package com.sap.cloud.lm.sl.cf.web.monitoring;

import java.io.File;
import java.time.LocalTime;
import java.util.Hashtable;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;

@Component
public class FssMonitor {

    Map<File, Long> usedSpaceMap = new Hashtable<>(1);
    Map<File, LocalTime> updateTimesMap = new Hashtable<>(1);

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
        LocalTime lastChecked = updateTimesMap.get(filePath);
        LocalTime invalidateDeadline = LocalTime.now()
            .minusMinutes(updateTimeoutMinutes);
        return invalidateDeadline.isBefore(lastChecked);
    }

    private long getUsedSpace(File filePath) {
        updateTimesMap.put(filePath, LocalTime.now());
        long usedSpace = FileUtils.sizeOf(filePath);
        usedSpaceMap.put(filePath, usedSpace);
        return usedSpace;
    }
}
