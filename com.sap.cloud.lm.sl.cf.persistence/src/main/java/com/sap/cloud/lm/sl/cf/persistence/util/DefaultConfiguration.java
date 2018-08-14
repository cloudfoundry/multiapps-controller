package com.sap.cloud.lm.sl.cf.persistence.util;

import java.io.OutputStream;

import com.sap.cloud.lm.sl.cf.persistence.model.FileEntry;
import com.sap.cloud.lm.sl.cf.persistence.processors.DefaultFileDownloadProcessor;
import com.sap.cloud.lm.sl.cf.persistence.processors.DefaultFileUploadProcessor;
import com.sap.cloud.lm.sl.cf.persistence.processors.FileDownloadProcessor;
import com.sap.cloud.lm.sl.cf.persistence.processors.FileUploadProcessor;
import com.sap.cloud.lm.sl.cf.persistence.services.FileContentProcessor;

public class DefaultConfiguration implements Configuration {

    private static final Long DEFAULT_MAX_UPLOAD_SIZE = 4 * 1024 * 1024 * 1024l; // 4GB
    private static final boolean DEFAULT_SCAN_UPLOADS = false;

    private final long maxUploadSize;
    private final boolean scanUploads;

    public DefaultConfiguration() {
        this(DEFAULT_MAX_UPLOAD_SIZE, DEFAULT_SCAN_UPLOADS);
    }

    public DefaultConfiguration(long maxUploadSize, boolean scanUploads) {
        this.maxUploadSize = maxUploadSize;
        this.scanUploads = scanUploads;
    }

    @Override
    public FileUploadProcessor<? extends OutputStream, ? extends OutputStream> getFileUploadProcessor() {
        return new DefaultFileUploadProcessor(this.scanUploads);
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
