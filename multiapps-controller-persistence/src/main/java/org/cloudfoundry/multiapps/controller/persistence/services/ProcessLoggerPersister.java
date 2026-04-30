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
import org.springframework.scheduling.annotation.Async;

@Named("processLoggerPersister")
public class ProcessLoggerPersister {

    private final ProcessLoggerProvider processLoggerProvider;
    private final ProcessLogsPersistenceService processLogsPersistenceService;

    @Inject
    public ProcessLoggerPersister(ProcessLoggerProvider processLoggerProvider,
                                  ProcessLogsPersistenceService processLogsPersistenceService) {
        this.processLoggerProvider = processLoggerProvider;
        this.processLogsPersistenceService = processLogsPersistenceService;
    }

    @Async("asyncExecutor")
    public void persistLogs(String correlationId, String taskId) {

        List<ProcessLogger> processLoggers = processLoggerProvider.getExistingLoggers(correlationId, taskId);
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
                                                                            .withModified(LocalDateTime.now());

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
