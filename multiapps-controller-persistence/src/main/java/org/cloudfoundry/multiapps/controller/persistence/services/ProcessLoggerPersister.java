package org.cloudfoundry.multiapps.controller.persistence.services;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableOperationLogEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.LoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.OperationLogEntry;

@Named("processLoggerPersister")
public class ProcessLoggerPersister {

    private final ProcessLoggerProvider processLoggerProvider;
    private final ProcessLogsPersistenceService processLogsPersistenceService;
    private final OperationLogsExporter operationLogsExporter;

    @Inject
    public ProcessLoggerPersister(ProcessLoggerProvider processLoggerProvider,
                                  ProcessLogsPersistenceService processLogsPersistenceService,
                                  OperationLogsExporter operationLogsExporter) {
        this.processLoggerProvider = processLoggerProvider;
        this.processLogsPersistenceService = processLogsPersistenceService;
        this.operationLogsExporter = operationLogsExporter;
    }

    //    @Async("asyncExecutor")
    public void persistLogs(LoggingConfiguration loggingConfiguration, String correlationId, String taskId) {
        List<ProcessLogger> processLoggers = processLoggerProvider.getExistingLoggers(correlationId, taskId);
        Map<String, StringBuilder> processLogsMessages = new HashMap<>();

        if (processLoggers.isEmpty()) {
            return;
        }

        for (ProcessLogger processLogger : processLoggers) {
            if (processLogsMessages.containsKey(processLogger.getOperationLogEntry()
                                                             .getOperationLogName())) {
                processLogsMessages.get(processLogger.getOperationLogEntry()
                                                     .getOperationLogName())
                                   .append(processLogger.getLogMessage());
            } else {
                StringBuilder logMessage = new StringBuilder();
                logMessage.append(processLogger.getLogMessage());
                processLogsMessages.put(processLogger.getOperationLogEntry()
                                                     .getOperationLogName(), logMessage);
            }

            processLoggerProvider.removeProcessLoggerFromCache(processLogger);
        }

        OperationLogEntry operationLogEntryWithExistingData = processLoggers.get(0)
                                                                            .getOperationLogEntry();

        for (var processLogsMessage : processLogsMessages.entrySet()) {
            OperationLogEntry operationLogEntry = ImmutableOperationLogEntry.copyOf(operationLogEntryWithExistingData)
                                                                            .withId(UUID.randomUUID()
                                                                                        .toString())
                                                                            .withOperationLogName(processLogsMessage.getKey())
                                                                            .withOperationLog(processLogsMessage.getValue()
                                                                                                                .toString())
                                                                            .withModified(LocalDateTime.now());

            OperationLogEntry operationLogEntry2 = operationLogsExporter.sendLogsToCloudLoggingService(loggingConfiguration,
                                                                                                       operationLogEntry);
            if (operationLogEntry2 != null) {
                operationLogEntry = operationLogEntry2;
            }

            processLogsPersistenceService.persistLog(operationLogEntry);
        }
    }
}
