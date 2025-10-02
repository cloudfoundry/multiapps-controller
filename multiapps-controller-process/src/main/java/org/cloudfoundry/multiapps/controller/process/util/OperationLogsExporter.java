package org.cloudfoundry.multiapps.controller.process.util;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.persistence.model.ExternalOperationLogEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableExternalOperationLogEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.OperationLogEntry;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.persistence.services.ProcessLogsPersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;

public class OperationLogsExporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationLogsExporter.class);
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final long MAX_LIMIT_REQUEST_SIZE_BYTES = 3 * 1024 * 1024 + 512 * 1024; // 3.5MB

    private final ProcessLogsPersistenceService processLogsPersistenceService;
    private final WebClient webClient;

    public OperationLogsExporter(ProcessLogsPersistenceService processLogsPersistenceService, WebClient webClient) {
        this.processLogsPersistenceService = processLogsPersistenceService;
        this.webClient = webClient;
    }

    public void exportLogs(String spaceId, String operationId, String endpoint, String serverCa, String clientCert, String clientKey)
        throws FileStorageException {
        LOGGER.info("Export logs for operation {} in space {}", operationId, spaceId);
        List<ExternalOperationLogEntry> externalLogEntries = getExternalLogEntries(spaceId, operationId);
        List<List<ExternalOperationLogEntry>> logEntryBatches = getLogEntryBatches(externalLogEntries);
        LOGGER.info("Exporting {} log entries into {} batches", externalLogEntries.size(), logEntryBatches.size());
        for (List<ExternalOperationLogEntry> logEntryBatch : logEntryBatches) {
            webClient.post()
                     .header("Content-Type", CONTENT_TYPE_JSON)
                     .bodyValue(JsonUtil.toJson(logEntryBatch))
                     .retrieve()
                     .bodyToMono(Void.class)
                     .block();
        }

    }

    private List<ExternalOperationLogEntry> getExternalLogEntries(String spaceId, String operationId) throws FileStorageException {
        LOGGER.debug("Retrieving log entries for operation {} in space {}", operationId, spaceId);

        List<OperationLogEntry> operationLogEntries = processLogsPersistenceService.listOperationLogsBySpaceAndOperationId(spaceId,
                                                                                                                           operationId);

        return operationLogEntries.stream()
                                  .map(logEntry -> convertToExternalLogEntry(operationId, logEntry))
                                  .collect(Collectors.toList());
    }

    private ExternalOperationLogEntry convertToExternalLogEntry(String operationId, OperationLogEntry logEntry) {
        return ImmutableExternalOperationLogEntry.builder()
                                                 .timestamp(String.valueOf(logEntry.getModified()
                                                                                   .atOffset(ZoneOffset.UTC)))
                                                 .message(logEntry.getOperationLog())
                                                 .correlationId(operationId)
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

}
