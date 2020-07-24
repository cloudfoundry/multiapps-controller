package org.cloudfoundry.multiapps.controller.core.util;

import org.cloudfoundry.multiapps.controller.core.Constants;
import org.slf4j.MDC;

public final class LoggingUtil {

    private LoggingUtil() {

    }

    public static void logWithCorrelationId(String correlationId, Runnable runnable) {
        MDC.put(Constants.ATTR_CORRELATION_ID, correlationId);
        runnable.run();
        MDC.remove(Constants.ATTR_CORRELATION_ID);
    }

}
