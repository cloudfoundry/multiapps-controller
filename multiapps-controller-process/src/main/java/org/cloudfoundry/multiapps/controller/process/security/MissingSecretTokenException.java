package org.cloudfoundry.multiapps.controller.process.security;

public class MissingSecretTokenException extends RuntimeException {

    public MissingSecretTokenException(String message) {
        super(message);
    }

    public MissingSecretTokenException(String message, Throwable cause) {
        super(message, cause);
    }

    public MissingSecretTokenException(Throwable cause) {
        super(cause);
    }

}
