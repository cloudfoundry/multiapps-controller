package com.sap.cloud.lm.sl.cf.web.security;

public class AuthorizationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int statusCode;

    public AuthorizationException(int statusCode) {
        this(statusCode, null);
    }

    public AuthorizationException(int statusCode, String message) {
        this(statusCode, message, null);
    }

    public AuthorizationException(int statusCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

}
