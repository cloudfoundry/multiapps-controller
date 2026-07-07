package org.cloudfoundry.multiapps.controller.persistence.util;

import java.time.ZoneOffset;
import java.util.UUID;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.persistence.model.ExternalOperationLogEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableExternalOperationLogEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.LogLevel;
import org.cloudfoundry.multiapps.controller.persistence.model.LoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.OperationLogEntry;
import org.cloudfoundry.multiapps.controller.persistence.services.OperationLogsExporter;
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

    public static ExternalOperationLogEntry convertToExternalLogEntry(OperationLogEntry operationLogEntry,
                                                                      OperationLogsExporter.OperationLog operationLog,
                                                                      LogLevel level, String operationId) {
        return ImmutableExternalOperationLogEntry.builder()
                                                 .timestamp(String.valueOf(operationLog.dateTime()
                                                                                       .atOffset(ZoneOffset.UTC)))
                                                 .message(operationLog.log())
                                                 .id(UUID.randomUUID()
                                                         .toString())
                                                 .operationLogName(operationLogEntry.getOperationLogName())
                                                 .correlationId(operationId)
                                                 .level(level.name())
                                                 .build();
    }

    public static ExternalOperationLogEntry convertToExternalLogEntry(LoggingConfiguration loggingConfiguration,
                                                                      OperationLogsExporter.OperationLog operationLog,
                                                                      LogLevel level, String logName) {
        return ImmutableExternalOperationLogEntry.builder()
                                                 .timestamp(String.valueOf(operationLog.dateTime()
                                                                                       .atOffset(ZoneOffset.UTC)))
                                                 .message(operationLog.log())
                                                 .id(UUID.randomUUID()
                                                         .toString())
                                                 .operationLogName(logName)
                                                 .correlationId(loggingConfiguration.getOperationId())
                                                 .level(level.name())
                                                 .build();
    }
}
