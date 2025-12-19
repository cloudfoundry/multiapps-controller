package org.cloudfoundry.multiapps.controller.process.security.store;

public class SecretTokenStoringException extends RuntimeException {

    public SecretTokenStoringException(String message) {
        super(message);
    }

    public SecretTokenStoringException(String message, Throwable cause) {
        super(message, cause);
    }

    public SecretTokenStoringException(Throwable cause) {
        super(cause);
    }

}
