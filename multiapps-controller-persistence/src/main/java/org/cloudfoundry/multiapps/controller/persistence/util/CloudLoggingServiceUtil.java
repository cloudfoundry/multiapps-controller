package org.cloudfoundry.multiapps.controller.persistence.util;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.persistence.model.LoggingConfiguration;
import org.slf4j.Logger;

public class CloudLoggingServiceUtil {

    private CloudLoggingServiceUtil() {
    }

    public static void logErrorOrThrowExceptionBasedOnFailSafe(LoggingConfiguration loggingConfiguration, Logger logger, String message) {
        if (loggingConfiguration.isFailSafe()) {
            logger.error(message);
        } else {
            throw new SLException(message);
        }
    }
}
