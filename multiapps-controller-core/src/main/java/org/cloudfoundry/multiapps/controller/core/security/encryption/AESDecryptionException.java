package org.cloudfoundry.multiapps.controller.core.security.encryption;

public class AESDecryptionException extends RuntimeException {

    public AESDecryptionException(String message) {
        super(message);
    }

    public AESDecryptionException(String message, Throwable cause) {
        super(message, cause);
    }

    public AESDecryptionException(Throwable cause) {
        super(cause);
    }
    
}
