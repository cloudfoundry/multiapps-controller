package org.cloudfoundry.multiapps.controller.persistence.services;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.persistence.model.ExternalOperationLogEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableExternalOperationLogEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableOperationLogEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.LoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.OperationLogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Named("operationLogsExporter")
public class OperationLogsExporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationLogsExporter.class);
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final long MAX_LIMIT_REQUEST_SIZE_BYTES = 3 * 1024 * 1024 + 512 * 1024; // 3.5MB
    private static final Map<String, WebClient> clientCache = new ConcurrentHashMap<>();
    private final Pattern pattern = Pattern.compile("^#[^#\\r\\n]*#[^#\\r\\n]*#([^#\\r\\n]*)#", Pattern.MULTILINE);

    private final ProcessLogsPersistenceService processLogsPersistenceService;

    public OperationLogsExporter(ProcessLogsPersistenceService processLogsPersistenceService) {
        this.processLogsPersistenceService = processLogsPersistenceService;
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
                                                                                      OperationLogEntry operationLogEntry) {
        Map<String, List<String>> operationLogs = getLogsFromOperationLogEntry(loggingConfiguration, operationLogEntry);
        List<ExternalOperationLogEntry> externalOperationLogEntries = new ArrayList<>();

        for (Map.Entry<String, List<String>> operationLog : operationLogs.entrySet()) {
            for (String log : operationLog.getValue()) {
                externalOperationLogEntries.add(convertToExternalLogEntry(operationLogEntry, log, operationLog.getKey()));
            }
        }
        return getLogEntryBatches(externalOperationLogEntries);
    }

    private WebClient getCloudLogginServiceWebClient(LoggingConfiguration loggingConfiguration) {
        WebClient webClient = null;

        if (!clientCache.containsKey(loggingConfiguration.getOperationId())) {
            webClient = createWebClientWithMtls(loggingConfiguration);
            clientCache.put(loggingConfiguration.getOperationId(), webClient);
        } else {
            webClient = clientCache.get(loggingConfiguration.getOperationId());
        }

        return webClient;
    }

    private void sendLogsToCloudLoggingService(List<List<ExternalOperationLogEntry>> externalOperationLogEntryBatches,
                                               WebClient webClient, LoggingConfiguration loggingConfiguration) {
        for (List<ExternalOperationLogEntry> logEntryBatch : externalOperationLogEntryBatches) {
            ResponseEntity<Void> response = webClient.post()
                                                     .header("Content-Type", CONTENT_TYPE_JSON)
                                                     .bodyValue(JsonUtil.toJson(logEntryBatch))
                                                     .retrieve()
                                                     .toBodilessEntity()
                                                     .block();
            if (response.getStatusCode()
                        .value() > 299 || response.getStatusCode()
                                                  .value() < 200) {
                if (!loggingConfiguration.isFailSafe()) {
                    throw new SLException("Something went wrong");
                }
            }
        }
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

    private Map<String, List<String>> getLogsFromOperationLogEntry(LoggingConfiguration loggingConfiguration,
                                                                   OperationLogEntry operationLogEntry) {
        List<OperationLogEntry> operationLogEntries = getUnsendProcessLogs(loggingConfiguration);
        Map<String, List<String>> logsMap = new HashMap<>();

        for (OperationLogEntry ope : operationLogEntries) {
            doSomethingMethod(ope.getOperationLog(), logsMap);
            try {
                processLogsPersistenceService.updateIsSendToCloudLoggingService(ope.getId(), true);
            } catch (FileStorageException e) {
                throw new RuntimeException(e);
            }
        }

        doSomethingMethod(operationLogEntry.getOperationLog(), logsMap);
        return logsMap;
    }

    private void doSomethingMethod(String log, Map<String, List<String>> logsMap) {
        String[] messages = log.split("(?m)^#[^#\\r\\n]*#[^#\\r\\n]*#[^#\\r\\n]*#[^#\\r\\n]*#[^#\\r\\n]*#(?:\\r?\\n)?");

        List<String> logLevels = getLogLevels(log);

        int levelIndex = 0;
        for (String message : messages) {
            if (message.isBlank()) {
                continue;
            }

            String cleanedMessage = extractMessage(message);
            String level = logLevels.get(levelIndex);

            logsMap.computeIfAbsent(level, key -> new ArrayList<>())
                   .add(cleanedMessage);
            levelIndex++;
        }
    }

    private List<String> getLogLevels(String log) {
        Matcher matcher = pattern.matcher(log);
        List<String> logLevels = new ArrayList<>();

        while (matcher.find()) {
            logLevels.add(matcher.group(1));
        }

        return logLevels;
    }

    private String extractMessage(String message) {
        String trimmed = message.substring(message.indexOf("]") + 1)
                                .trim();
        return trimmed.substring(0, trimmed.length() - 1);
    }

    private List<OperationLogEntry> getUnsendProcessLogs(LoggingConfiguration loggingConfiguration) {
        try {
            return processLogsPersistenceService.listOperationLogsBySpaceAndOperationIdAndIsSendToCloudLoggingService(
                loggingConfiguration.getTargetSpace(), loggingConfiguration.getOperationId());
        } catch (FileStorageException e) {
            throw new RuntimeException(e);
        }
    }

    private WebClient createWebClientWithMtls(LoggingConfiguration loggingConfiguration) {
        LOGGER.debug("Creating WebClient with mTLS configuration for endpoint: {}", loggingConfiguration.getEndpointUrl());

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
            if (!loggingConfiguration.isFailSafe()) {
                throw new SLException(e);
            } else {
                LOGGER.error("ERROR");
                return null;
            }
        }
    }

    private InputStream getCredentialInputStream(String credential) {
        return new ByteArrayInputStream((credential.getBytes(StandardCharsets.UTF_8)));
    }

    private ExternalOperationLogEntry convertToExternalLogEntry(OperationLogEntry operationLogEntry, String operationLog, String level) {
        return ImmutableExternalOperationLogEntry.builder()
                                                 .timestamp(String.valueOf(operationLogEntry.getModified()
                                                                                            .atOffset(ZoneOffset.UTC)))
                                                 .message(operationLog)
                                                 .id(UUID.randomUUID()
                                                         .toString())
                                                 .operationLogName(operationLogEntry.getOperationLogName())
                                                 .correlationId(operationLogEntry.getOperationId())
                                                 .level(level)
                                                 .build();
    }
}
