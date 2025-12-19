package org.cloudfoundry.multiapps.controller.process.security.resolver;

public class MissingCredentialsFromUserProvidedServiceEncryptionRelated extends RuntimeException {

    public MissingCredentialsFromUserProvidedServiceEncryptionRelated(String message) {
        super(message);
    }

    public MissingCredentialsFromUserProvidedServiceEncryptionRelated(String message, Throwable cause) {
        super(message, cause);
    }

    public MissingCredentialsFromUserProvidedServiceEncryptionRelated(Throwable cause) {
        super(cause);
    }

}
