package org.cloudfoundry.multiapps.controller.process.util;

public class ContentLengthTracker {
    private long totalSize = 0;

    public long getTotalSize() {
        return totalSize;
    }

    public void addToTotalFileSize(long sizeToAdd) {
        totalSize += sizeToAdd;
    }
}