package com.sap.cloud.lm.sl.cf.client.util;

import java.io.IOException;
import java.util.Set;

import org.cloudfoundry.client.lib.CloudOperationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;

import com.sap.cloud.lm.sl.common.SLException;

import io.netty.channel.ChannelException;

public class ExceptionHandlerFactory {

    public ExceptionHandler getExceptionHandler(Exception e, Set<HttpStatus> httpStatusesToIgnore, boolean isFailSafe) {
        if (isIOException(e)) {
            return new IgnoringExceptionHandler();
        }
        if (e instanceof CloudOperationException) {
            return new CloudOperationExceptionHandler(isFailSafe, httpStatusesToIgnore);
        }
        if (e instanceof SLException) {
            return new IgnoringExceptionHandler();
        }
        return new GenericExceptionHandler(isFailSafe);
    }

    private boolean isIOException(Exception e) {
        return e instanceof ResourceAccessException || e instanceof IOException || e instanceof ChannelException;
    }

}
