package org.cloudfoundry.multiapps.controller.persistence.services;

import java.io.IOException;
import java.io.InputStream;

/**
 * An interface for consuming the content of a file.
 *
 */
public interface FileContentConsumer {

    /**
     * Consume the content of a file.
     *
     * @param inputStream an input stream representing the file content
     */
    void consume(InputStream inputStream) throws IOException;

}
