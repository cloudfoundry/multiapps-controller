package org.cloudfoundry.multiapps.controller.persistence.services;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableOperationLogEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.OperationLogEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ProcessLoggerPersisterConfiguration;
import org.springframework.scheduling.annotation.Async;

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

    @Async("asyncExecutor")
    public void persistLogs(ProcessLoggerPersisterConfiguration processLoggerPersisterConfiguration) {

        List<ProcessLogger> processLoggers = processLoggerProvider.getExistingLoggers(processLoggerPersisterConfiguration.correlationId(),
                                                                                      processLoggerPersisterConfiguration.taskId());
        Map<String, StringBuilder> processLogsMessages = getProcessLogsMessages(processLoggers);
        if (processLogsMessages.isEmpty()) {
            return;
        }

        OperationLogEntry operationLogEntryWithExistingData = processLoggers.getFirst()
                                                                            .getOperationLogEntry();

        for (var processLogsMessage : processLogsMessages.entrySet()) {
            OperationLogEntry operationLogEntry = ImmutableOperationLogEntry.copyOf(operationLogEntryWithExistingData)
                                                                            .withId(UUID.randomUUID()
                                                                                        .toString())
                                                                            .withOperationLogName(processLogsMessage.getKey())
                                                                            .withOperationLog(processLogsMessage.getValue()
                                                                                                                .toString())
                                                                            .withIsSendToCloudLoggingService(false)
                                                                            .withModified(LocalDateTime.now());

            //            operationLogsExporter.sendLogsToCloudLoggingService(processLoggerPersisterConfiguration.loggingConfiguration(),
            //                                                                operationLogEntry);
            //            if (operationLogEntry2 != null) {
            //                operationLogEntry = operationLogEntry2;
            //            }

            processLogsPersistenceService.persistLog(operationLogEntry);
        }
    }

    public Map<String, StringBuilder> getProcessLogsMessages(List<ProcessLogger> processLoggers) {
        if (processLoggers.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, StringBuilder> processLogsMessages = new HashMap<>();

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
        return processLogsMessages;
    }
}
