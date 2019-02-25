package com.sap.cloud.lm.sl.cf.client.util;

import java.util.Set;

import org.cloudfoundry.client.lib.CloudOperationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;

public class ExceptionHandlerFactory {

    public ExceptionHandler geExceptionHandler(Exception e, Set<HttpStatus> httpStatusesToIgnore, boolean isFailSafe) {
        if (e instanceof ResourceAccessException) {
            return new ResourceAccessExceptionHandler();
        }

        if (e instanceof CloudOperationException) {
            return new CloudOperationExceptionHandler(isFailSafe, httpStatusesToIgnore);
        }

        return new GenericExceptionHandler(isFailSafe);
    }

}
