package org.cloudfoundry.multiapps.controller.core.security.encryption;

public class AESEncryptionException extends RuntimeException {

    public AESEncryptionException(String message) {
        super(message);
    }

    public AESEncryptionException(String message, Throwable cause) {
        super(message, cause);
    }

    public AESEncryptionException(Throwable cause) {
        super(cause);
    }
    
}
