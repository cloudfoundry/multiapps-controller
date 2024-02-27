package com.sap.cloud.lm.sl.cf.persistence.services;

public class FileStorageException extends Exception {

    private static final long serialVersionUID = -4798385554251279267L;

    public FileStorageException(Throwable cause) {
        super(cause);
    }

    public FileStorageException(String message) {
        super(message);
    }

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }

}
