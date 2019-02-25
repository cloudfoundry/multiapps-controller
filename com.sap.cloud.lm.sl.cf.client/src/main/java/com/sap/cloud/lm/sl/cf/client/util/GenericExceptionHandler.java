package com.sap.cloud.lm.sl.cf.client.util;

import java.text.MessageFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenericExceptionHandler implements ExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GenericExceptionHandler.class);

    private boolean isFailSafe;

    public GenericExceptionHandler(boolean isFailSafe) {
        this.isFailSafe = isFailSafe;
    }

    @Override
    public void handleException(Exception e) throws RuntimeException {
        if (!isFailSafe) {
            throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
        }
        LOGGER.warn(MessageFormat.format("Retrying failed request with message: {0}", e.getMessage()));
    }

}
