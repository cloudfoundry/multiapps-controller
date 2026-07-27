package org.cloudfoundry.multiapps.controller.web.util;

/**
 * Thrown when an operation cannot be started because a rate limit has been reached. Carries the number of seconds after which the caller may
 * retry, which is mappable to an HTTP 429 {@code Retry-After} response header.
 */
public class OperationRateLimitExceededException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final long retryAfterSeconds;

    public OperationRateLimitExceededException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
