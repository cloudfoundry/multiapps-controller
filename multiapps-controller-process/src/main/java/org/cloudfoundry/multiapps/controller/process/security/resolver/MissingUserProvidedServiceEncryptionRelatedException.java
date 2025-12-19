package org.cloudfoundry.multiapps.controller.process.security.resolver;

public class MissingUserProvidedServiceEncryptionRelatedException extends RuntimeException {

    public MissingUserProvidedServiceEncryptionRelatedException(String message) {
        super(message);
    }

    public MissingUserProvidedServiceEncryptionRelatedException(String message, Throwable cause) {
        super(message, cause);
    }

    public MissingUserProvidedServiceEncryptionRelatedException(Throwable cause) {
        super(cause);
    }

}
