package org.cloudfoundry.multiapps.controller.persistence.services;

import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.inject.Named;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.persistence.Messages;
import org.cloudfoundry.multiapps.controller.persistence.model.ExternalOperationLogEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableExternalOperationLogEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.LogLevel;
import org.cloudfoundry.multiapps.controller.persistence.model.LoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.OperationLogEntry;
import org.cloudfoundry.multiapps.controller.persistence.util.CloudLoggingServiceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;

@Named("operationLogsExporter")
public class OperationLogsExporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationLogsExporter.class);

    private static final int CLIENT_CACHE_MAX_SIZE = 256;
    private static final long MAX_LIMIT_REQUEST_SIZE_BYTES = 3 * 1024 * 1024 + 512 * 1024;

    private static final Map<String, WebClient> clientCache = Collections.synchronizedMap(
        new LinkedHashMap<>(CLIENT_CACHE_MAX_SIZE, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, WebClient> eldest) {
                return size() > CLIENT_CACHE_MAX_SIZE;
            }
        });

    private final ProcessLogsPersistenceService processLogsPersistenceService;
    private final CloudLoggingServiceHttpClient cloudLoggingServiceHttpClient;
    private final CloudLoggingServiceMessageConverter cloudLoggingServiceMessageConverter;

    public OperationLogsExporter(ProcessLogsPersistenceService processLogsPersistenceService,
                                 CloudLoggingServiceHttpClient cloudLoggingServiceHttpClient,
                                 CloudLoggingServiceMessageConverter cloudLoggingServiceMessageConverter) {
        this.processLogsPersistenceService = processLogsPersistenceService;
        this.cloudLoggingServiceHttpClient = cloudLoggingServiceHttpClient;
        this.cloudLoggingServiceMessageConverter = cloudLoggingServiceMessageConverter;
    }

    public void sendLogsToCloudLoggingService(LoggingConfiguration loggingConfiguration, String message) {
        List<List<ExternalOperationLogEntry>> externalOperationLogEntryBatches = getExternalOperationLogEntryBatches(loggingConfiguration,
                                                                                                                     message);

        WebClient cloudLoggingServiceWebClient = getCloudLoggingServiceWebClient(loggingConfiguration);
        if (cloudLoggingServiceWebClient == null) {
            return;
        }

        sendLogsToCloudLoggingService(externalOperationLogEntryBatches, cloudLoggingServiceWebClient, loggingConfiguration);
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
        WebClient cloudLoggingServiceWebClient = getCloudLoggingServiceWebClient(loggingConfiguration);
        if (cloudLoggingServiceWebClient == null) {
            return;
        }
        ExternalOperationLogEntry entry = ImmutableExternalOperationLogEntry.builder()
                                                                            .timestamp(String.valueOf(LocalDateTime.now()
                                                                                                                   .atOffset(
                                                                                                                       ZoneOffset.UTC)))
                                                                            .message(message)
                                                                            .id(UUID.randomUUID()
                                                                                    .toString())
                                                                            .operationLogName(
                                                                                cloudLoggingServiceMessageConverter.extractLogName(message)
                                                                                                                   .orElse(""))
                                                                            .correlationId(loggingConfiguration.getOperationId())
                                                                            .level(level.name())
                                                                            .build();
        sendLogsToCloudLoggingService(getLogEntryBatches(List.of(entry)), cloudLoggingServiceWebClient,
                                      loggingConfiguration);
    }

    public void sendLogsToCloudLoggingService(LoggingConfiguration loggingConfiguration, OperationLogEntry operationLogEntry) {
        if (loggingConfiguration == null) {
            return;
        }
        List<List<ExternalOperationLogEntry>> externalOperationLogEntryBatches = getExternalOperationLogEntryBatches(loggingConfiguration,
                                                                                                                     operationLogEntry);

        WebClient cloudLoggingServiceWebClient = getCloudLoggingServiceWebClient(loggingConfiguration);
        if (cloudLoggingServiceWebClient == null) {
            return;
        }

        sendLogsToCloudLoggingService(externalOperationLogEntryBatches, cloudLoggingServiceWebClient, loggingConfiguration);
    }

    public List<OperationLogEntry> getUnsendProcessLogs(LoggingConfiguration loggingConfiguration) {
        try {
            return processLogsPersistenceService.listOperationLogsBySpaceAndOperationId(loggingConfiguration.getMtaSpaceId(),
                                                                                        loggingConfiguration.getOperationId());
        } catch (FileStorageException e) {
            CloudLoggingServiceUtil.logErrorOrThrowExceptionBasedOnFailSafe(loggingConfiguration, LOGGER, e.getMessage());
            return List.of();
        }
    }

    public void removeClientFromCache(String operationId) {
        clientCache.remove(operationId);
    }

    private List<List<ExternalOperationLogEntry>> getExternalOperationLogEntryBatches(LoggingConfiguration loggingConfiguration,
                                                                                      String message) {
        Map<LogLevel, List<OperationLog>> operationLogs = cloudLoggingServiceMessageConverter.getLogsFromOperationLogEntry(
            loggingConfiguration, message);
        Map<LogLevel, List<OperationLog>> filteredOperationLogs = removeLogsWithUnwantedLogLevel(loggingConfiguration, operationLogs);
        List<ExternalOperationLogEntry> externalOperationLogEntries = getExternalOperationLogEntries(loggingConfiguration,
                                                                                                     filteredOperationLogs, message);
        return getLogEntryBatches(externalOperationLogEntries);
    }

    private List<ExternalOperationLogEntry> getExternalOperationLogEntries(LoggingConfiguration loggingConfiguration,
                                                                           Map<LogLevel, List<OperationLog>> filteredOperationLogs,
                                                                           String message) {
        List<ExternalOperationLogEntry> externalOperationLogEntries = new ArrayList<>();
        String logName = cloudLoggingServiceMessageConverter.extractLogName(message)
                                                            .orElse("");

        for (Map.Entry<LogLevel, List<OperationLog>> operationLog : filteredOperationLogs.entrySet()) {
            for (OperationLog log : operationLog.getValue()) {
                externalOperationLogEntries.add(convertToExternalLogEntry(loggingConfiguration, log, operationLog.getKey(), logName));
            }
        }

        return externalOperationLogEntries;
    }

    private List<List<ExternalOperationLogEntry>> getExternalOperationLogEntryBatches(LoggingConfiguration loggingConfiguration,
                                                                                      OperationLogEntry operationLogEntry) {
        Map<LogLevel, List<OperationLog>> operationLogs = cloudLoggingServiceMessageConverter.getLogsFromOperationLogEntry(
            loggingConfiguration,
            operationLogEntry.getOperationLog());
        Map<LogLevel, List<OperationLog>> filteredOperationLogs = removeLogsWithUnwantedLogLevel(loggingConfiguration, operationLogs);
        List<ExternalOperationLogEntry> externalOperationLogEntries = new ArrayList<>();

        for (Map.Entry<LogLevel, List<OperationLog>> operationLog : filteredOperationLogs.entrySet()) {
            for (OperationLog log : operationLog.getValue()) {
                externalOperationLogEntries.add(
                    convertToExternalLogEntry(operationLogEntry, log, operationLog.getKey(), loggingConfiguration.getOperationId()));
            }
        }
        return getLogEntryBatches(externalOperationLogEntries);
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

    public List<List<ExternalOperationLogEntry>> getLogEntryBatches(List<ExternalOperationLogEntry> externalLogEntries) {
        List<List<ExternalOperationLogEntry>> batches = new ArrayList<>();
        List<ExternalOperationLogEntry> currentBatch = new ArrayList<>();
        long currentChunkSize = 0L;

        for (ExternalOperationLogEntry entry : externalLogEntries) {
            String entryJson = JsonUtil.toJson(entry);
            int entrySize = entryJson.getBytes(StandardCharsets.UTF_8).length;

            if (currentChunkSize + entrySize > MAX_LIMIT_REQUEST_SIZE_BYTES && !currentBatch.isEmpty()) {
                batches.add(new ArrayList<>(currentBatch));
                currentBatch.clear();
                currentChunkSize = 0L;
            }

            currentBatch.add(entry);
            currentChunkSize += entrySize;
        }
        if (!currentBatch.isEmpty()) {
            batches.add(currentBatch);
        }
        return batches;
    }

    private WebClient getCloudLoggingServiceWebClient(LoggingConfiguration loggingConfiguration) {
        WebClient webClient = null;

        if (!clientCache.containsKey(loggingConfiguration.getOperationId())) {
            webClient = cloudLoggingServiceHttpClient.createWebClientWithMtls(loggingConfiguration);
            clientCache.put(loggingConfiguration.getOperationId(), webClient);
            LOGGER.debug(MessageFormat.format(Messages.CREATING_WEBCLIENT_WITH_MTLS_CONFIGURATION_FOR_ENDPOINT_1,
                                              loggingConfiguration.getEndpointUrl()));
        } else {
            webClient = clientCache.get(loggingConfiguration.getOperationId());
        }

        return webClient;
    }

    private void sendLogsToCloudLoggingService(List<List<ExternalOperationLogEntry>> externalOperationLogEntryBatches,
                                               WebClient webClient, LoggingConfiguration loggingConfiguration) {
        for (List<ExternalOperationLogEntry> logEntryBatch : externalOperationLogEntryBatches) {
            cloudLoggingServiceHttpClient.sendLogsToCloudLoggingService(loggingConfiguration, webClient, logEntryBatch);
        }
    }

    private ExternalOperationLogEntry convertToExternalLogEntry(OperationLogEntry operationLogEntry, OperationLog operationLog,
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

    private ExternalOperationLogEntry convertToExternalLogEntry(LoggingConfiguration loggingConfiguration, OperationLog operationLog,
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

    public record OperationLog(String log, LocalDateTime dateTime) {

    }
}
