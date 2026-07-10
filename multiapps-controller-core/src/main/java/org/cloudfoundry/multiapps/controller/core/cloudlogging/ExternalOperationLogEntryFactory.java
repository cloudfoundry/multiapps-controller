package org.cloudfoundry.multiapps.controller.core.cloudlogging;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.persistence.model.ExternalOperationLogEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableExternalOperationLogEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.LogLevel;
import org.cloudfoundry.multiapps.controller.persistence.model.LoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.OperationLog;
import org.cloudfoundry.multiapps.controller.persistence.model.OperationLogEntry;
import org.cloudfoundry.multiapps.controller.persistence.util.CloudLoggingServiceUtil;

@Named("externalOperationLogEntryFactory")
public class ExternalOperationLogEntryFactory {

    private final CloudLoggingServiceMessageConverter cloudLoggingServiceMessageConverter;

    @Inject
    public ExternalOperationLogEntryFactory(CloudLoggingServiceMessageConverter cloudLoggingServiceMessageConverter) {
        this.cloudLoggingServiceMessageConverter = cloudLoggingServiceMessageConverter;
    }

    public List<ExternalOperationLogEntry> fromMessage(LoggingConfiguration loggingConfiguration, String message) {
        Map<LogLevel, List<OperationLog>> filteredOperationLogs = getFilteredOperationLogs(loggingConfiguration, message);
        String logName = cloudLoggingServiceMessageConverter.extractLogName(message)
                                                            .orElse("");

        List<ExternalOperationLogEntry> externalOperationLogEntries = new ArrayList<>();
        for (Map.Entry<LogLevel, List<OperationLog>> operationLog : filteredOperationLogs.entrySet()) {
            for (OperationLog log : operationLog.getValue()) {
                externalOperationLogEntries.add(
                    CloudLoggingServiceUtil.convertToExternalLogEntry(loggingConfiguration, log, operationLog.getKey(), logName));
            }
        }
        return externalOperationLogEntries;
    }

    public List<ExternalOperationLogEntry> fromOperationLogEntry(LoggingConfiguration loggingConfiguration,
                                                                 OperationLogEntry operationLogEntry) {
        Map<LogLevel, List<OperationLog>> filteredOperationLogs = getFilteredOperationLogs(loggingConfiguration,
                                                                                          operationLogEntry.getOperationLog());

        List<ExternalOperationLogEntry> externalOperationLogEntries = new ArrayList<>();
        for (Map.Entry<LogLevel, List<OperationLog>> operationLog : filteredOperationLogs.entrySet()) {
            for (OperationLog log : operationLog.getValue()) {
                externalOperationLogEntries.add(
                    CloudLoggingServiceUtil.convertToExternalLogEntry(operationLogEntry, log, operationLog.getKey(),
                                                                      loggingConfiguration.getOperationId()));
            }
        }
        return externalOperationLogEntries;
    }

    public ExternalOperationLogEntry fromLevelledMessage(LoggingConfiguration loggingConfiguration, String message, LogLevel level) {
        return ImmutableExternalOperationLogEntry.builder()
                                                 .timestamp(String.valueOf(LocalDateTime.now()
                                                                                        .atOffset(ZoneOffset.UTC)))
                                                 .message(message)
                                                 .id(UUID.randomUUID()
                                                         .toString())
                                                 .operationLogName(cloudLoggingServiceMessageConverter.extractLogName(message)
                                                                                                      .orElse(""))
                                                 .correlationId(loggingConfiguration.getOperationId())
                                                 .level(level.name())
                                                 .build();
    }

    private Map<LogLevel, List<OperationLog>> getFilteredOperationLogs(LoggingConfiguration loggingConfiguration, String message) {
        Map<LogLevel, List<OperationLog>> operationLogs = cloudLoggingServiceMessageConverter.getLogsFromOperationLogEntry(
            loggingConfiguration, message);
        return removeLogsWithUnwantedLogLevel(loggingConfiguration, operationLogs);
    }

    private Map<LogLevel, List<OperationLog>> removeLogsWithUnwantedLogLevel(LoggingConfiguration loggingConfiguration,
                                                                             Map<LogLevel, List<OperationLog>> operationLogs) {
        List<LogLevel> allowedLevelsToLog = LogLevel.getLogLevelLoggingType()
                                                    .get(loggingConfiguration.getLogLevel());

        return operationLogs.entrySet()
                            .stream()
                            .filter(operationLog -> allowedLevelsToLog.contains(operationLog.getKey()))
                            .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue
                            ));
    }
}
