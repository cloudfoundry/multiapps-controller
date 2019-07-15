package com.sap.cloud.lm.sl.cf.persistence.services;

import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;

/**
 * An interface for reading the content of an uploaded file
 *
 * @author i031908
 *
 */
public interface FileContentProcessor {

    int DEFAULT_BUFFER_SIZE = 4 * 1024;

    /**
     * Processes the content of an uploaded file.
     *
     * @param is an input stream representing the file content
     * @throws Exception
     */
    void processFileContent(InputStream is) throws NoSuchAlgorithmException, IOException, FileStorageException; // NOPMD

}
