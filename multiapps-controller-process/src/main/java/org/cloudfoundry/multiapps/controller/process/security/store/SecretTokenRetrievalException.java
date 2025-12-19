package org.cloudfoundry.multiapps.controller.process.security.store;

public class SecretTokenRetrievalException extends RuntimeException {

    public SecretTokenRetrievalException(String message) {
        super(message);
    }

    public SecretTokenRetrievalException(String message, Throwable cause) {
        super(message, cause);
    }

    public SecretTokenRetrievalException(Throwable cause) {
        super(cause);
    }

}
