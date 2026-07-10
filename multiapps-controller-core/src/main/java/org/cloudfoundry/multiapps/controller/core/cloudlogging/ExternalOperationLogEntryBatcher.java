package org.cloudfoundry.multiapps.controller.core.cloudlogging;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Named;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.persistence.model.ExternalOperationLogEntry;

@Named("externalOperationLogEntryBatcher")
public class ExternalOperationLogEntryBatcher {

    // SAP Cloud Logging ingest endpoint accepts payloads up to ~4 MB; 3.5 MB leaves headroom for JSON envelope and HTTP framing.
    private static final long MAX_LIMIT_REQUEST_SIZE_BYTES = 3 * 1024 * 1024 + 512 * 1024;

    public List<List<ExternalOperationLogEntry>> batch(List<ExternalOperationLogEntry> externalLogEntries) {
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
}
