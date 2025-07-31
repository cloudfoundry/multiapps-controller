package org.cloudfoundry.multiapps.controller.client.facade;

import org.springframework.http.HttpStatus;

@SuppressWarnings("serial")
public class CloudOperationException extends CloudException {

    private final HttpStatus statusCode;
    private final String statusText;
    private final String description;

    public CloudOperationException(HttpStatus statusCode) {
        this(statusCode, statusCode.getReasonPhrase());
    }

    public CloudOperationException(HttpStatus statusCode, String statusText) {
        this(statusCode, statusText, null);
    }

    public CloudOperationException(HttpStatus statusCode, String statusText, String description) {
        this(statusCode, statusText, description, null);
    }

    public CloudOperationException(HttpStatus statusCode, String statusText, String description, Throwable cause) {
        super(getExceptionMessage(statusCode, statusText, description), cause);
        this.statusCode = statusCode;
        this.statusText = statusText;
        this.description = description;
    }

    private static String getExceptionMessage(HttpStatus statusCode, String statusText, String description) {
        if (description != null) {
            return String.format("%d %s: %s", statusCode.value(), statusText, description);
        }
        return String.format("%d %s", statusCode.value(), statusText);
    }

    public HttpStatus getStatusCode() {
        return statusCode;
    }

    public String getStatusText() {
        return statusText;
    }

    public String getDescription() {
        return description;
    }

}
