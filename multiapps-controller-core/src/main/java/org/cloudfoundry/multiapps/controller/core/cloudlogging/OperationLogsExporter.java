package org.cloudfoundry.multiapps.controller.core.cloudlogging;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.persistence.model.ExternalOperationLogEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.LogLevel;
import org.cloudfoundry.multiapps.controller.persistence.model.LoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.OperationLogEntry;

@Named("operationLogsExporter")
public class OperationLogsExporter {

    private final CloudLoggingServiceHttpClient cloudLoggingServiceHttpClient;
    private final ExternalOperationLogEntryFactory externalOperationLogEntryFactory;
    private final ExternalOperationLogEntryBatcher externalOperationLogEntryBatcher;

    @Inject
    public OperationLogsExporter(CloudLoggingServiceHttpClient cloudLoggingServiceHttpClient,
                                 ExternalOperationLogEntryFactory externalOperationLogEntryFactory,
                                 ExternalOperationLogEntryBatcher externalOperationLogEntryBatcher) {
        this.cloudLoggingServiceHttpClient = cloudLoggingServiceHttpClient;
        this.externalOperationLogEntryFactory = externalOperationLogEntryFactory;
        this.externalOperationLogEntryBatcher = externalOperationLogEntryBatcher;
    }

    public void sendLogsToCloudLoggingService(LoggingConfiguration loggingConfiguration, String message) {
        List<ExternalOperationLogEntry> entries = externalOperationLogEntryFactory.fromMessage(loggingConfiguration, message);
        sendInBatches(entries, loggingConfiguration);
    }

    public void info(LoggingConfiguration loggingConfiguration, String message) {
        sendLogsToCloudLoggingService(loggingConfiguration, message, LogLevel.INFO);
    }

    public void warn(LoggingConfiguration loggingConfiguration, String message) {
        sendLogsToCloudLoggingService(loggingConfiguration, message, LogLevel.WARN);
    }

    public void error(LoggingConfiguration loggingConfiguration, String message) {
        sendLogsToCloudLoggingService(loggingConfiguration, message, LogLevel.ERROR);
    }

    public void debug(LoggingConfiguration loggingConfiguration, String message) {
        sendLogsToCloudLoggingService(loggingConfiguration, message, LogLevel.DEBUG);
    }

    public void trace(LoggingConfiguration loggingConfiguration, String message) {
        sendLogsToCloudLoggingService(loggingConfiguration, message, LogLevel.TRACE);
    }

    public void sendLogsToCloudLoggingService(LoggingConfiguration loggingConfiguration, String message, LogLevel level) {
        if (loggingConfiguration == null) {
            return;
        }
        List<LogLevel> allowedLevels = LogLevel.getLogLevelLoggingType()
                                               .get(loggingConfiguration.getLogLevel());
        if (allowedLevels == null || !allowedLevels.contains(level)) {
            return;
        }
        ExternalOperationLogEntry entry = externalOperationLogEntryFactory.fromLevelledMessage(loggingConfiguration, message, level);
        sendInBatches(List.of(entry), loggingConfiguration);
    }

    public void sendLogsToCloudLoggingService(LoggingConfiguration loggingConfiguration, OperationLogEntry operationLogEntry) {
        if (loggingConfiguration == null) {
            return;
        }
        List<ExternalOperationLogEntry> entries = externalOperationLogEntryFactory.fromOperationLogEntry(loggingConfiguration,
                                                                                                         operationLogEntry);
        sendInBatches(entries, loggingConfiguration);
    }

    private void sendInBatches(List<ExternalOperationLogEntry> entries, LoggingConfiguration loggingConfiguration) {
        for (List<ExternalOperationLogEntry> logEntryBatch : externalOperationLogEntryBatcher.batch(entries)) {
            cloudLoggingServiceHttpClient.sendLogs(loggingConfiguration, logEntryBatch);
        }
    }
}
