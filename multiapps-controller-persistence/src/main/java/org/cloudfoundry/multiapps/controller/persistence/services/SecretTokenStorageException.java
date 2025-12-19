package org.cloudfoundry.multiapps.controller.persistence.services;

public class SecretTokenStorageException extends Exception {

    public SecretTokenStorageException(String message) {
        super(message);
    }

    public SecretTokenStorageException(Throwable cause) {
        super(cause);
    }

    public SecretTokenStorageException(String message, Throwable cause) {
        super(message, cause);
    }

}
