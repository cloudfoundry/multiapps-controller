package org.cloudfoundry.multiapps.controller.process.util;

public class ContentLengthTracker {
    private long totalSize = 0;
    private long fileSize = 0;

    public long getTotalSize() {
        return totalSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public void incrementFileSize() {
        totalSize += fileSize;
    }

}