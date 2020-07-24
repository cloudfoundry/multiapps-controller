package org.cloudfoundry.multiapps.controller.persistence.util;

public class Configuration {

    private static final long DEFAULT_MAX_UPLOAD_SIZE = 4 * 1024 * 1024 * 1024L; // 4GB

    private final long maxUploadSize;

    public Configuration() {
        this(DEFAULT_MAX_UPLOAD_SIZE);
    }

    public Configuration(long maxUploadSize) {
        this.maxUploadSize = maxUploadSize;
    }

    public long getMaxUploadSize() {
        return maxUploadSize;
    }

}
