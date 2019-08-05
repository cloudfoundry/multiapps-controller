package com.sap.cloud.lm.sl.cf.persistence.processors;

import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;

import com.sap.cloud.lm.sl.cf.persistence.model.FileEntry;
import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;

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
     * @throws NoSuchAlgorithmException, IOException, FileStorageException
     */
    public void processContent(InputStream blobStream) throws NoSuchAlgorithmException, IOException, FileStorageException;

}
