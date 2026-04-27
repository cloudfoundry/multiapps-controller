package org.cloudfoundry.multiapps.controller.persistence.services;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.persistence.Messages;
import org.cloudfoundry.multiapps.controller.persistence.model.ExternalOperationLogEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableExternalOperationLogEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableOperationLogEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.LogLevel;
import org.cloudfoundry.multiapps.controller.persistence.model.LoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.OperationLogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import static com.azure.core.http.ContentType.APPLICATION_JSON;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

@Named("operationLogsExporter")
public class OperationLogsExporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationLogsExporter.class);
    private static final long MAX_LIMIT_REQUEST_SIZE_BYTES = 3 * 1024 * 1024 + 512 * 1024; // 3.5MB
    private static final Map<String, WebClient> clientCache = new ConcurrentHashMap<>();
    private static final Pattern MESSAGE_LOG_DATE_PATTERN = Pattern.compile("^#([^#\\r\\n]*)#", Pattern.MULTILINE);
    private static final Pattern MESSAGE_LOG_LEVEL_PATTERN = Pattern.compile("^#[^#\\r\\n]*#[^#\\r\\n]*#([^#\\r\\n]*)#", Pattern.MULTILINE);

    private static final String MESSAGE_SPLITTING_REGEX = "(?m)^#[^#\\r\\n]*#[^#\\r\\n]*#[^#\\r\\n]*#[^#\\r\\n]*#[^#\\r\\n]*#(?:\\r?\\n)?";

    private final ProcessLogsPersistenceService processLogsPersistenceService;

    public OperationLogsExporter(ProcessLogsPersistenceService processLogsPersistenceService) {
        this.processLogsPersistenceService = processLogsPersistenceService;
    }

    public void sendLogsToCloudLoggingService(LoggingConfiguration loggingConfiguration, String message) {
        List<List<ExternalOperationLogEntry>> externalOperationLogEntryBatches = getExternalOperationLogEntryBatches(loggingConfiguration,
                                                                                                                     message);

        WebClient cloudLogginServiceWebClient = getCloudLogginServiceWebClient(loggingConfiguration);
        if (cloudLogginServiceWebClient == null) {
            return;
        }

        sendLogsToCloudLoggingService(externalOperationLogEntryBatches, cloudLogginServiceWebClient, loggingConfiguration);
    }

    public OperationLogEntry sendLogsToCloudLoggingService(LoggingConfiguration loggingConfiguration, OperationLogEntry operationLogEntry) {
        if (loggingConfiguration == null) {
            return null;
        }
        List<List<ExternalOperationLogEntry>> externalOperationLogEntryBatches = getExternalOperationLogEntryBatches(loggingConfiguration,
                                                                                                                     operationLogEntry);

        WebClient cloudLogginServiceWebClient = getCloudLogginServiceWebClient(loggingConfiguration);
        if (cloudLogginServiceWebClient == null) {
            return null;
        }

        sendLogsToCloudLoggingService(externalOperationLogEntryBatches, cloudLogginServiceWebClient, loggingConfiguration);
        return ImmutableOperationLogEntry.copyOf(operationLogEntry)
                                         .withIsSendToCloudLoggingService(true);
    }

    public void removeClientFromCache(String operationId) {
        clientCache.remove(operationId);
    }

    private List<List<ExternalOperationLogEntry>> getExternalOperationLogEntryBatches(LoggingConfiguration loggingConfiguration,
                                                                                      String message) {
        Map<LogLevel, List<LogLogLog>> operationLogs = getLogsFromOperationLogEntry(message);
        Map<LogLevel, List<LogLogLog>> filteredOperationLogs = removeLogsWithUnwantedLogLevel(loggingConfiguration, operationLogs);
        List<ExternalOperationLogEntry> externalOperationLogEntries = new ArrayList<>();
        String logName = message.substring(message.indexOf("."), message.indexOf("#", message.indexOf(".") + 1));

        for (Map.Entry<LogLevel, List<LogLogLog>> operationLog : filteredOperationLogs.entrySet()) {
            for (LogLogLog log : operationLog.getValue()) {
                externalOperationLogEntries.add(convertToExternalLogEntry(loggingConfiguration, log, operationLog.getKey(), logName));
            }
        }
        return getLogEntryBatches(externalOperationLogEntries);
    }

    private List<List<ExternalOperationLogEntry>> getExternalOperationLogEntryBatches(LoggingConfiguration loggingConfiguration,
                                                                                      OperationLogEntry operationLogEntry) {
        Map<LogLevel, List<LogLogLog>> operationLogs = getLogsFromOperationLogEntry(operationLogEntry.getOperationLog());
        Map<LogLevel, List<LogLogLog>> filteredOperationLogs = removeLogsWithUnwantedLogLevel(loggingConfiguration, operationLogs);
        List<ExternalOperationLogEntry> externalOperationLogEntries = new ArrayList<>();

        for (Map.Entry<LogLevel, List<LogLogLog>> operationLog : filteredOperationLogs.entrySet()) {
            for (LogLogLog log : operationLog.getValue()) {
                externalOperationLogEntries.add(convertToExternalLogEntry(operationLogEntry, log, operationLog.getKey()));
            }
        }
        return getLogEntryBatches(externalOperationLogEntries);
    }

    private Map<LogLevel, List<LogLogLog>> removeLogsWithUnwantedLogLevel(LoggingConfiguration loggingConfiguration,
                                                                          Map<LogLevel, List<LogLogLog>> operationLogs) {
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

    private WebClient getCloudLogginServiceWebClient(LoggingConfiguration loggingConfiguration) {
        WebClient webClient = null;

        if (!clientCache.containsKey(loggingConfiguration.getOperationId())) {
            webClient = createWebClientWithMtls(loggingConfiguration);
            clientCache.put(loggingConfiguration.getOperationId(), webClient);
        } else {
            webClient = clientCache.get(loggingConfiguration.getOperationId());
        }

        LOGGER.debug(MessageFormat.format(Messages.CREATING_WEBCLIENT_WITH_MTLS_CONFIGURATION_FOR_ENDPOINT_1,
                                          loggingConfiguration.getEndpointUrl()));
        return webClient;
    }

    private void sendLogsToCloudLoggingService(List<List<ExternalOperationLogEntry>> externalOperationLogEntryBatches,
                                               WebClient webClient, LoggingConfiguration loggingConfiguration) {
        for (List<ExternalOperationLogEntry> logEntryBatch : externalOperationLogEntryBatches) {
            ResponseEntity<Void> response = executeSendLongHttpRequest(webClient, logEntryBatch);
            if (hasRequestFailed(response)) {
                logErrorOrThrowExceptionBasedOnFailSafe(loggingConfiguration, Messages.FAILED_TO_SEND_LOG_MESSAGE_TO_CLS);
            }
        }
    }

    private boolean hasRequestFailed(ResponseEntity<Void> response) {
        if (response == null) {
            return false;
        }
        int statusCode = response.getStatusCode()
                                 .value();
        return statusCode < 200 || statusCode > 299;
    }

    private ResponseEntity<Void> executeSendLongHttpRequest(WebClient webClient, List<ExternalOperationLogEntry> logEntryBatch) {
        return webClient.post()
                        .header(CONTENT_TYPE, APPLICATION_JSON)
                        .bodyValue(JsonUtil.toJson(logEntryBatch))
                        .retrieve()
                        .toBodilessEntity()
                        .block();
    }

    private List<List<ExternalOperationLogEntry>> getLogEntryBatches(List<ExternalOperationLogEntry> externalLogEntries) {
        List<List<ExternalOperationLogEntry>> batches = new ArrayList<>();
        List<ExternalOperationLogEntry> currentBatch = new ArrayList<>();
        long currentChunkSize = 0L;

        for (ExternalOperationLogEntry entry : externalLogEntries) {
            String entryJson = JsonUtil.toJson(entry);
            int entrySize = entryJson.getBytes().length;

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

    private Map<LogLevel, List<LogLogLog>> getLogsFromOperationLogEntry(String message) {
        Map<LogLevel, List<LogLogLog>> logsMap = new HashMap<>();
        getMessagesToLog(message, logsMap);
        return logsMap;
    }

    private void getMessagesToLog(String log, Map<LogLevel, List<LogLogLog>> logsMap) {
        String[] messages = log.split(MESSAGE_SPLITTING_REGEX);

        List<String> logLevels = getLogLevels(log);
        List<LocalDateTime> dateLevels = getLogDate(log);
        if (logLevels.isEmpty()) {
            return;
        }

        int levelIndex = 0;
        for (String message : messages) {
            if (message.isBlank()) {
                continue;
            }

            String cleanedMessage = extractMessage(message);
            String level = logLevels.get(levelIndex);
            LocalDateTime date = dateLevels.get(levelIndex);

            logsMap.computeIfAbsent(LogLevel.get(level), key -> new ArrayList<>())
                   .add(new LogLogLog(cleanedMessage, date));
            levelIndex++;
        }
    }

    private List<String> getLogLevels(String log) {
        Matcher matcher = MESSAGE_LOG_LEVEL_PATTERN.matcher(log);
        List<String> logLevels = new ArrayList<>();

        while (matcher.find()) {
            logLevels.add(matcher.group(1));
        }

        return logLevels;
    }

    private List<LocalDateTime> getLogDate(String log) {
        Matcher matcher = MESSAGE_LOG_DATE_PATTERN.matcher(log);
        List<LocalDateTime> logLevels = new ArrayList<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy MM dd HH:mm:ss.SSS");

        while (matcher.find()) {
            LocalDateTime dateTime = LocalDateTime.parse(matcher.group(1), formatter);
            logLevels.add(dateTime);
        }

        return logLevels;
    }

    private String extractMessage(String message) {
        String trimmed = message.substring(message.indexOf("]") + 1)
                                .trim();
        return trimmed.substring(0, trimmed.length() - 1);
    }

    public List<OperationLogEntry> getUnsendProcessLogs(LoggingConfiguration loggingConfiguration) {
        try {
            return processLogsPersistenceService.listOperationLogsBySpaceAndOperationIdAndIsSendToCloudLoggingService(
                loggingConfiguration.getTargetSpace(), loggingConfiguration.getOperationId());
        } catch (FileStorageException e) {
            logErrorOrThrowExceptionBasedOnFailSafe(loggingConfiguration, e.getMessage());
            return List.of();
        }
    }

    private WebClient createWebClientWithMtls(LoggingConfiguration loggingConfiguration) {
        SslContext sslContext = getSslContext(loggingConfiguration);
        if (sslContext == null) {
            return null;
        }
        HttpClient httpClient = HttpClient.create()
                                          .secure(sslSpec -> sslSpec.sslContext(sslContext));

        return WebClient.builder()
                        .baseUrl(loggingConfiguration.getEndpointUrl())
                        .clientConnector(new ReactorClientHttpConnector(httpClient))
                        .build();
    }

    private SslContext getSslContext(LoggingConfiguration loggingConfiguration) {
        try {
            InputStream serverCaStream = getCredentialInputStream(loggingConfiguration.getServerCa());
            InputStream clientCertStream = getCredentialInputStream(loggingConfiguration.getClientCert());
            InputStream clientKeyStream = getCredentialInputStream(loggingConfiguration.getClientKey());
            return SslContextBuilder.forClient()
                                    .keyManager(clientCertStream, clientKeyStream)
                                    .trustManager(serverCaStream)
                                    .build();
        } catch (IOException e) {
            logErrorOrThrowExceptionBasedOnFailSafe(loggingConfiguration, e.getMessage());
            return null;
        }
    }

    private void logErrorOrThrowExceptionBasedOnFailSafe(LoggingConfiguration loggingConfiguration, String message) {
        if (loggingConfiguration.isFailSafe()) {
            LOGGER.error(message);
        } else {
            throw new SLException(message);
        }
    }

    private InputStream getCredentialInputStream(String credential) {
        return new ByteArrayInputStream((credential.getBytes(StandardCharsets.UTF_8)));
    }

    private ExternalOperationLogEntry convertToExternalLogEntry(OperationLogEntry operationLogEntry, LogLogLog operationLog,
                                                                LogLevel level) {
        return ImmutableExternalOperationLogEntry.builder()
                                                 .timestamp(String.valueOf(operationLog.dateTime()
                                                                                       .atOffset(ZoneOffset.UTC)))
                                                 .message(operationLog.log())
                                                 .id(UUID.randomUUID()
                                                         .toString())
                                                 .operationLogName(operationLogEntry.getOperationLogName())
                                                 .correlationId(operationLogEntry.getOperationId())
                                                 .level(level.name())
                                                 .build();
    }

    private ExternalOperationLogEntry convertToExternalLogEntry(LoggingConfiguration loggingConfiguration, LogLogLog operationLog,
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

    private record LogLogLog(String log, LocalDateTime dateTime) {

    }
}
