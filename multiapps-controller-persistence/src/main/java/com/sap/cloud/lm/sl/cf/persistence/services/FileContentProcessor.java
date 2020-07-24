package com.sap.cloud.lm.sl.cf.persistence.services;

import java.io.IOException;
import java.io.InputStream;

/**
 * An interface for processing the content of a file.
 *
 */
public interface FileContentProcessor<T> {

    /**
     * Process the content of a file.
     *
     * @param inputStream an input stream representing the file content
     */
    T process(InputStream inputStream) throws IOException;

}
