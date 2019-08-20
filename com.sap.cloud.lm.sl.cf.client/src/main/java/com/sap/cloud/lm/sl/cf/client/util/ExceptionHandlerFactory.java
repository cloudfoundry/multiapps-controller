package com.sap.cloud.lm.sl.cf.client.util;

import java.io.IOException;
import java.util.Set;

import org.cloudfoundry.client.lib.CloudOperationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;

import com.sap.cloud.lm.sl.common.SLException;

public class ExceptionHandlerFactory {

    public ExceptionHandler getExceptionHandler(Exception e, Set<HttpStatus> httpStatusesToIgnore, boolean isFailSafe) {
        if (e instanceof ResourceAccessException || e instanceof IOException) {
            return new IgnoringExceptionHandler();
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
