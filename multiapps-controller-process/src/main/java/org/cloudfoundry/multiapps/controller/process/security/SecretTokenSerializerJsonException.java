package org.cloudfoundry.multiapps.controller.process.security;

public class SecretTokenSerializerJsonException extends RuntimeException {

    public SecretTokenSerializerJsonException(String message) {
        super(message);
    }

    public SecretTokenSerializerJsonException(String message, Throwable cause) {
        super(message, cause);
    }

    public SecretTokenSerializerJsonException(Throwable cause) {
        super(cause);
    }

}
