package com.sap.cloud.lm.sl.cf.persistence.processors;

import java.io.InputStream;

import com.sap.cloud.lm.sl.cf.persistence.model.FileEntry;

/**
 * An interface for reading the content of an uploaded file.
 *
 * @author i072928
 *
 */
public interface FileDownloadProcessor {

    /**
     * Gets file entry object representing uploaded file.
     *
     * @return file entry
     */
    public FileEntry getFileEntry();

    /**
     * Processes file content from the input stream.
     *
     * @param blobStream input stream
     * @throws Exception
     */
    public void processContent(InputStream blobStream) throws Exception;

}
