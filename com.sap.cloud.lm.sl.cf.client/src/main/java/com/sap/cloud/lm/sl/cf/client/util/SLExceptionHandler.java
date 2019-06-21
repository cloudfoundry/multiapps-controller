package com.sap.cloud.lm.sl.cf.client.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SLExceptionHandler implements ExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SLExceptionHandler.class);

    @Override
    public void handleException(Exception e) throws RuntimeException {
        LOGGER.warn(e.getMessage(), e);
    }
}
