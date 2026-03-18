package org.cloudfoundry.multiapps.controller.persistence.services;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.SSLException;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.persistence.model.ExternalOperationLogEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableExternalOperationLogEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.LoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.OperationLogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    //    @Async("cloudLoggingServiceAsyncExecutor")
    public void sendLogsToCloudLoggingService(LoggingConfiguration loggingConfiguration, OperationLogEntry operationLogEntry) {
        if (loggingConfiguration == null) {
            return;
        }
        //        Map<String, List<String>> logs = getLogsFromOperationLogEntry(loggingConfiguration, operationLogEntry);
        Map<String, List<String>> logs = getLogsFromOperationLogEntry(loggingConfiguration, operationLogEntry);
        List<ExternalOperationLogEntry> externalOperationLogEntries = new ArrayList<>();

        for (Map.Entry<String, List<String>> log : logs.entrySet()) {
            for (String logg : log.getValue()) {
                ExternalOperationLogEntry externalOperationLogEntry = convertToExternalLogEntry(loggingConfiguration.getOperationId(),
                                                                                                operationLogEntry.getModified(), logg,
                                                                                                operationLogEntry.getOperationLogName(),
                                                                                                log.getKey());
                externalOperationLogEntries.add(externalOperationLogEntry);
            }
        }
        List<List<ExternalOperationLogEntry>> externalOperationLogEntryBatches = getLogEntryBatches(externalOperationLogEntries);
        WebClient webClient = null;
        if (!clientCache.containsKey(loggingConfiguration.getOperationId())) {
            try {
                webClient = createWebClientWithMtls(loggingConfiguration);
                clientCache.put(loggingConfiguration.getOperationId(), webClient);

            } catch (SSLException e) {
                throw new RuntimeException(e);
            }
        } else {
            webClient = clientCache.get(loggingConfiguration.getOperationId());
        }
        //        List<List<ExternalOperationLogEntry>> logEntryBatches = getLogEntryBatches(externalLogEntries);

        for (List<ExternalOperationLogEntry> logEntryBatch : externalOperationLogEntryBatches) {
            webClient.post()
                     .header("Content-Type", CONTENT_TYPE_JSON)
                     .bodyValue(JsonUtil.toJson(logEntryBatch))
                     .retrieve()
                     .bodyToMono(Void.class)
                     .block();
        }

    }

    private ExternalOperationLogEntry convertToExternalLogEntry(String operationId, LocalDateTime timestamp, String operationLog,
                                                                String operationLogName, String level) {
        return ImmutableExternalOperationLogEntry.builder()
                                                 .timestamp(String.valueOf(timestamp.atOffset(ZoneOffset.UTC)))
                                                 .message(operationLog)
                                                 .id(UUID.randomUUID()
                                                         .toString())
                                                 .operationLogName(operationLogName)
                                                 .correlationId(operationId)
                                                 .level(level)
                                                 .build();
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

    //    private Map<String, List<String>> getLogsFromOperationLogEntry(LoggingConfiguration loggingConfiguration,
    //                                                                   OperationLogEntry operationLogEntry) {
    private Map<String, List<String>> getLogsFromOperationLogEntry(LoggingConfiguration loggingConfiguration,
                                                                   OperationLogEntry operationLogEntry) {
        List<String> logs = new ArrayList<>();
        Map<String, List<String>> logsMap = new HashMap<>();

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

            Matcher matcher = pattern.matcher(ope.getOperationLog());

            List<String> logLevels = new ArrayList<>();

            if (ope.getOperationLog()
                   .contains("Step")) {
                System.out.println("asda");
            }
            while (matcher.find()) {
                logLevels.add(matcher.group(1));
            }

            for (int i = 0; i < splittedString.length; i++) {
                String s = splittedString[i];
                if (!s.isEmpty() && !s.isBlank()) {
                    s = s.substring(s.indexOf("]") + 1)
                         .trim();
                    s = s.substring(0, s.length() - 1);

                    String level = logLevels.get(i - 1);

                    if (logsMap.containsKey(level)) {
                        logsMap.get(level)
                               .add(s);
                    } else {
                        List<String> log = new ArrayList<>();
                        log.add(s);
                        logsMap.put(level, log);
                    }

                    logs.add(s);
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

        List<String> logLevels = new ArrayList<>();

        Matcher matcher = pattern.matcher(operationLogEntry.getOperationLog());
        while (matcher.find()) {
            logLevels.add(matcher.group(1));
        }

        if (operationLogEntry.getOperationLog()
                             .contains("Step")) {
            System.out.println("asda");
        }

        for (int i = 0; i < splittedString.length; i++) {
            String s = splittedString[i];
            if (!s.isEmpty() && !s.isBlank()) {
                s = s.substring(s.indexOf("]") + 1)
                     .trim();
                s = s.substring(0, s.length() - 1);

                String level = logLevels.get(i - 1);

                if (logsMap.containsKey(level)) {
                    logsMap.get(level)
                           .add(s);
                } else {
                    List<String> log = new ArrayList<>();
                    log.add(s);
                    logsMap.put(level, log);
                }

                logs.add(s);
            }
        }
        return logsMap;
    }

    private WebClient createWebClientWithMtls(LoggingConfiguration loggingConfiguration) throws SSLException {
        LOGGER.debug("Creating WebClient with mTLS configuration for endpoint: {}", loggingConfiguration.getEndpointUrl());

        InputStream serverCaStream = new ByteArrayInputStream(loggingConfiguration.getServerCa()
                                                                                  .getBytes(StandardCharsets.UTF_8));
        InputStream clientCertStream = new ByteArrayInputStream(loggingConfiguration.getClientCert()
                                                                                    .getBytes(StandardCharsets.UTF_8));
        InputStream clientKeyStream = new ByteArrayInputStream(loggingConfiguration.getClientKey()
                                                                                   .getBytes(StandardCharsets.UTF_8));

        SslContext sslContext = SslContextBuilder.forClient()
                                                 .keyManager(clientCertStream, clientKeyStream)
                                                 .trustManager(serverCaStream)
                                                 .build();

        HttpClient httpClient = HttpClient.create()
                                          .secure(sslSpec -> sslSpec.sslContext(sslContext));

        return WebClient.builder()
                        .baseUrl(loggingConfiguration.getEndpointUrl())
                        .clientConnector(new ReactorClientHttpConnector(httpClient))
                        .build();
    }
}
