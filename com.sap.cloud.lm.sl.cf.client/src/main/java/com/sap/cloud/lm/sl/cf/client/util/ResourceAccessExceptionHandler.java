package com.sap.cloud.lm.sl.cf.client.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceAccessExceptionHandler implements ExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceAccessExceptionHandler.class);

    @Override
    public void handleException(Exception e) {
        LOGGER.warn(e.getMessage(), e);
    }

}
