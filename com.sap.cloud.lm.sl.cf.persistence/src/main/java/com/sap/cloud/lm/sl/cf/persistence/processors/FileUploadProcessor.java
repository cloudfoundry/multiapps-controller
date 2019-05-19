package com.sap.cloud.lm.sl.cf.persistence.processors;

import java.io.IOException;
import java.io.OutputStream;

/**
 * An interface for uploading the content of a file.
 *
 * @author i072928
 *
 */
public interface FileUploadProcessor<I extends OutputStream, O extends OutputStream> {

    /**
     * Gets size of the internal buffer which will be used when uploading the file.
     *
     * @return buffer size
     */
    public int getProcessingBufferSize();

    /**
     * Creates wrapper around the original output stream.
     *
     * @param outputStream original output stream
     * @return output stream wrapper
     */
    public O createOutputStreamWrapper(I outputStream) throws IOException;

    /**
     * Writes chunk of data in the given output stream.
     *
     * @param outputStreamWrapper output stream wrapper
     * @param data chunk of file data
     * @param readToIndex last index of the data that is of interest
     * @return processed data
     * @throws Exception
     */
    public void writeFileChunk(O outputStreamWrapper, byte[] data, int readToIndex) throws IOException;

}
