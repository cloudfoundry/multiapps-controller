package com.sap.cloud.lm.sl.cf.persistence.util;

import java.io.OutputStream;

import com.sap.cloud.lm.sl.cf.persistence.model.FileEntry;
import com.sap.cloud.lm.sl.cf.persistence.processors.DefaultFileDownloadProcessor;
import com.sap.cloud.lm.sl.cf.persistence.processors.DefaultFileUploadProcessor;
import com.sap.cloud.lm.sl.cf.persistence.processors.FileDownloadProcessor;
import com.sap.cloud.lm.sl.cf.persistence.processors.FileUploadProcessor;
import com.sap.cloud.lm.sl.cf.persistence.services.FileContentProcessor;

public class DefaultConfiguration implements Configuration {

    private static final long DEFAULT_MAX_UPLOAD_SIZE = 4 * 1024 * 1024 * 1024L; // 4GB

    private final long maxUploadSize;

    public DefaultConfiguration() {
        this(DEFAULT_MAX_UPLOAD_SIZE);
    }

    public DefaultConfiguration(long maxUploadSize) {
        this.maxUploadSize = maxUploadSize;
    }

    @Override
    public FileUploadProcessor<? extends OutputStream, ? extends OutputStream> getFileUploadProcessor() {
        return new DefaultFileUploadProcessor();
    }

    @Override
    public FileDownloadProcessor getFileDownloadProcessor(FileEntry fileEntry, FileContentProcessor fileContentProcessor) {
        return new DefaultFileDownloadProcessor(fileEntry, fileContentProcessor);
    }

    @Override
    public long getMaxUploadSize() {
        return maxUploadSize;
    }

}
