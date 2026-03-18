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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named("processLoggerPersister")
public class ProcessLoggerPersister {

    private final ProcessLoggerProvider processLoggerProvider;
    private final ProcessLogsPersistenceService processLogsPersistenceService;
    private final OperationLogsExporter operationLogsExporter;

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessLoggerPersister.class);

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

            operationLogsExporter.sendLogsToCloudLoggingService(loggingConfiguration, operationLogEntry);
            OperationLogEntry operationLogEntry2 = logTheLogsToTheLoggerLog(loggingConfiguration, operationLogEntry);
            if (operationLogEntry2 != null) {
                operationLogEntry = operationLogEntry2;
            }

            processLogsPersistenceService.persistLog(operationLogEntry);
        }
    }

    private OperationLogEntry logTheLogsToTheLoggerLog(LoggingConfiguration loggingConfiguration, OperationLogEntry operationLogEntry) {
        if (loggingConfiguration == null) {
            return null;
        }
        List<OperationLogEntry> operationLogEntries = null;
        try {
            operationLogEntries = processLogsPersistenceService.listOperationLogsBySpaceAndOperationIdAndIsSendToCloudLoggingService(
                loggingConfiguration.getTargetSpace(),
                loggingConfiguration.getOperationId());
        } catch (FileStorageException e) {
            throw new RuntimeException(e);
        }

        for (OperationLogEntry ope : operationLogEntries) {
            String[] splittedString = ope.getOperationLog()
                                         .split("(?m)^#[^#\\r\\n]*#[^#\\r\\n]*#[^#\\r\\n]*#[^#\\r\\n]*#[^#\\r\\n]*#(?:\\r?\\n)?");
            for (String s : splittedString) {
                if (!s.isEmpty() && !s.isBlank()) {
                    s = s.substring(s.indexOf("]") + 1)
                         .trim();
                    s = s.substring(0, s.length() - 1);
                    //                    LOGGER.error(loggingConfiguration.getOperationId() + " loggging\n" + s);
                }
            }
            try {
                processLogsPersistenceService.updateIsSendToCloudLoggingService(ope.getId(), true);
            } catch (FileStorageException e) {
                throw new RuntimeException(e);
            }
        }

        String[] splittedString = operationLogEntry.getOperationLog()
                                                   .split("(?m)^#[^#\\r\\n]*#[^#\\r\\n]*#[^#\\r\\n]*#[^#\\r\\n]*#[^#\\r\\n]*#(?:\\r?\\n)?");
        for (String s : splittedString) {
            if (!s.isEmpty() && !s.isBlank()) {
                s = s.substring(s.indexOf("]") + 1)
                     .trim();
                s = s.substring(0, s.length() - 1);
                //                LOGGER.error(loggingConfiguration.getOperationId() + " loggging from somewhere else\n" + s);
            }
        }

        return ImmutableOperationLogEntry.copyOf(operationLogEntry)
                                         .withIsSendToCloudLoggingService(true);
    }
}
