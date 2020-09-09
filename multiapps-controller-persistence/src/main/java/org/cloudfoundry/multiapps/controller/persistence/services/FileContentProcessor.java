package org.cloudfoundry.multiapps.controller.persistence.services;

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
     * @return the result from the processing
     * @throws IOException in case of read/write error
     */
    T process(InputStream inputStream) throws IOException;

}
