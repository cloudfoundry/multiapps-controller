package com.sap.cloud.lm.sl.cf.core.util;

import org.slf4j.MDC;

import com.sap.cloud.lm.sl.cf.core.Constants;

public final class LoggingUtil {

    private LoggingUtil() {

    }

    public static void logWithCorrelationId(String correlationId, Runnable runnable) {
        MDC.put(Constants.ATTR_CORRELATION_ID, correlationId);
        runnable.run();
        MDC.remove(Constants.ATTR_CORRELATION_ID);
    }

}
