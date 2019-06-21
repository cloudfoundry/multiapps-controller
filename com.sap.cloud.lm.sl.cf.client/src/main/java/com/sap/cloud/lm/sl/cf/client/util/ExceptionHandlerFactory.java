package com.sap.cloud.lm.sl.cf.client.util;

import java.util.Set;

import org.cloudfoundry.client.lib.CloudOperationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;

import com.sap.cloud.lm.sl.common.SLException;

import io.netty.handler.timeout.TimeoutException;

public class ExceptionHandlerFactory {

    public ExceptionHandler getExceptionHandler(Exception e, Set<HttpStatus> httpStatusesToIgnore, boolean isFailSafe) {
        if (e instanceof ResourceAccessException) {
            return new ResourceAccessExceptionHandler();
        }

        if (e instanceof TimeoutException) {
            return new ResourceAccessExceptionHandler();
        }

        if (e instanceof CloudOperationException) {
            return new CloudOperationExceptionHandler(isFailSafe, httpStatusesToIgnore);
        }

        if (e instanceof SLException) {
            return new SLExceptionHandler();
        }

        return new GenericExceptionHandler(isFailSafe);
    }

}
