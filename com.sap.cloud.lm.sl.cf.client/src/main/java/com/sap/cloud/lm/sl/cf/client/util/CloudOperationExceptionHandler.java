package com.sap.cloud.lm.sl.cf.client.util;

import java.text.MessageFormat;
import java.util.Set;

import org.apache.commons.compress.utils.Sets;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

public class CloudOperationExceptionHandler implements ExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudOperationExceptionHandler.class);

    private static final Set<HttpStatus> statusesToIgnore = Sets.newHashSet(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.BAD_GATEWAY,
                                                                            HttpStatus.SERVICE_UNAVAILABLE, HttpStatus.GATEWAY_TIMEOUT,
                                                                            HttpStatus.REQUEST_TIMEOUT);

    private Set<HttpStatus> httpStatuses;
    private boolean isFailSafe;

    public CloudOperationExceptionHandler(boolean isFailSafe, Set<HttpStatus> httpStatuses) {
        this.httpStatuses = httpStatuses;
        this.isFailSafe = isFailSafe;
    }

    @Override
    public void handleException(Exception e) {
        CloudOperationException cloudOperationException = (CloudOperationException) e;
        if (!shouldIgnoreException(cloudOperationException, httpStatuses)) {
            throw cloudOperationException;
        }
        LOGGER.warn(MessageFormat.format("Retrying failed request with status: {0} and message: {1}",
                                         cloudOperationException.getStatusCode(), cloudOperationException.getMessage()));
    }

    private boolean shouldIgnoreException(CloudOperationException e, Set<HttpStatus> httpStatusesToIgnore) {
        if (isFailSafe) {
            return true;
        }
        for (HttpStatus status : httpStatusesToIgnore) {
            if (e.getStatusCode()
                 .equals(status)) {
                return true;
            }
        }
        return statusesToIgnore.contains(e.getStatusCode());
    }
}
