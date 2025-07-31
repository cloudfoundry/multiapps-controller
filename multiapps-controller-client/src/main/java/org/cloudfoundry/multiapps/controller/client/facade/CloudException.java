package org.cloudfoundry.multiapps.controller.client.facade;

@SuppressWarnings("serial")
public class CloudException extends RuntimeException {

    public CloudException(Throwable cause) {
        super(cause);
    }

    public CloudException(String message, Throwable cause) {
        super(message, cause);
    }

    public CloudException(String message) {
        super(message);
    }

}
