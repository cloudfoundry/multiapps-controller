package com.sap.cloud.lm.sl.cf.process.util;

import java.nio.file.Path;

import com.sap.cloud.lm.sl.common.SLException;

public interface ExtractStatusCallback {

    public static final ExtractStatusCallback NONE = new ExtractStatusCallback() {
        // default implementation: do nothing
    };

    /**
     * Called after the creation of the file or the directory containing all files. The writing to the file hasn't begun yet.
     * 
     * This path is usually used as cleanup after the writing has been aborted or failed.
     * 
     * @param filePath The path to the created file
     */
    default void onFileCreated(Path filePath) {
        // default implementation: do nothing
    }

    /**
     * Called before writing bytes to the file.
     * 
     * Implementation can throw SLException to abort writing to the file.
     * 
     * @param bytes The bytes which are to be written to the file
     * @return
     */
    default void onBytesToWrite(int bytes) throws SLException {
        // default implementation: do nothing
    }

    /**
     * Called when an exception has been thrown during saving of the file(s).
     * 
     * The exception thrown by {@link #onBytesToWrite(int) onBytesToWrite} is caught and passed to this method.
     * 
     * Implementation can do a cleanup of the file/dir.
     * 
     * @param e Thrown IOException
     */
    default void onError(Exception e) {
        // default implementation: do nothing
    }
}
