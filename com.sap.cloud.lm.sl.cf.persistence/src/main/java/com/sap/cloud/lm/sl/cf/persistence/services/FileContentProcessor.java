package com.sap.cloud.lm.sl.cf.persistence.services;

import java.io.InputStream;

/**
 * An interface for reading the content of an uploaded file
 *
 * @author i031908
 *
 */
public interface FileContentProcessor {

    public static final int DEFAULT_BUFFER_SIZE = 4 * 1024;

    /**
     * Processes the content of an uploaded file.
     *
     * @param is an input stream representing the file content
     * @throws Exception
     */
    public void processFileContent(InputStream is) throws Exception; // NOPMD

}
