package com.sap.cloud.lm.sl.cf.persistence.util;

import java.io.OutputStream;

import com.sap.cloud.lm.sl.cf.persistence.model.FileEntry;
import com.sap.cloud.lm.sl.cf.persistence.processors.FileDownloadProcessor;
import com.sap.cloud.lm.sl.cf.persistence.processors.FileUploadProcessor;
import com.sap.cloud.lm.sl.cf.persistence.services.FileContentProcessor;

public interface Configuration {

    /**
     * Gets file upload processor based on the file name.
     * 
     * @param filename file name
     * @return FileUploadProcessor instance
     */
    FileUploadProcessor<? extends OutputStream, ? extends OutputStream> getFileUploadProcessor();

    /**
     * Gets file download processor based on the file entry and FileContentProcessor.
     * 
     * @param fileEntry fileEntry
     * @param fileContentProcessor FileContentProcessor
     * @return FileDownloadProcessor instance
     */
    FileDownloadProcessor getFileDownloadProcessor(FileEntry fileEntry, FileContentProcessor fileContentProcessor);

    long getMaxUploadSize();

}
