package org.cloudfoundry.multiapps.controller.persistence.services;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

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
    public void persistLogs(String correlationId, String activityId) {
        List<ProcessLogger> processLoggers = processLoggerProvider.getExistingLoggers(correlationId, activityId);
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
                                                     .getOperationLogName(),
                                        logMessage);
            }

            processLoggerProvider.removeProcessLoggerFromCache(processLogger);
        }

        for (var processLogsMessage : processLogsMessages.entrySet()) {
            OperationLogEntry operationLogEntry = ImmutableOperationLogEntry.copyOf(processLoggers.get(0)
                                                                                                  .getOperationLogEntry())
                                                                            .withOperationLogName(processLogsMessage.getKey())
                                                                            .withOperationLog(processLogsMessage.toString())
                                                                            .withModified(LocalDateTime.now());
            processLogsPersistenceService.persistLog(operationLogEntry);
        }
    }
}
