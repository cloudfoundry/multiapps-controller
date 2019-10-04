package com.sap.cloud.lm.sl.cf.persistence.processors;

import java.io.IOException;
import java.io.OutputStream;

/**
 * An interface for uploading the content of a file.
 *
 * @author i072928
 *
 */
public interface FileUploadProcessor {

    /**
     * Gets size of the internal buffer which will be used when uploading the file.
     *
     * @return buffer size
     */
    int getProcessingBufferSize();

    /**
     * Writes chunk of data in the given output stream.
     *
     * @param outputStream output stream wrapper
     * @param data chunk of file data
     * @param readToIndex last index of the data that is of interest
     * @return processed data
     * @throws Exception
     */
    void writeFileChunk(OutputStream outputStream, byte[] data, int readToIndex) throws IOException;

}
