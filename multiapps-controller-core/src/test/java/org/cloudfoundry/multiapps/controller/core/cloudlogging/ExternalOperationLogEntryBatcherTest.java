package org.cloudfoundry.multiapps.controller.core.cloudlogging;

import java.util.List;

import org.cloudfoundry.multiapps.controller.persistence.model.ExternalOperationLogEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableExternalOperationLogEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExternalOperationLogEntryBatcherTest {

    private ExternalOperationLogEntryBatcher batcher;

    @BeforeEach
    void setUp() {
        batcher = new ExternalOperationLogEntryBatcher();
    }

    @Test
    void batch_emptyInputProducesNoBatches() {
        List<List<ExternalOperationLogEntry>> batches = batcher.batch(List.of());

        assertTrue(batches.isEmpty());
    }

    @Test
    void batch_smallEntriesFitInSingleBatch() {
        List<ExternalOperationLogEntry> entries = List.of(entry("a"), entry("b"), entry("c"));

        List<List<ExternalOperationLogEntry>> batches = batcher.batch(entries);

        assertEquals(1, batches.size());
        assertEquals(3, batches.get(0)
                               .size());
    }

    @Test
    void batch_preservesEntryOrder() {
        ExternalOperationLogEntry first = entry("first");
        ExternalOperationLogEntry second = entry("second");

        List<List<ExternalOperationLogEntry>> batches = batcher.batch(List.of(first, second));

        assertEquals(first, batches.get(0)
                                   .get(0));
        assertEquals(second, batches.get(0)
                                    .get(1));
    }

    @Test
    void batch_splitsWhenCumulativeSizeExceedsLimit() {
        String largeText = "x".repeat(1024 * 1024);
        List<ExternalOperationLogEntry> entries = List.of(entry(largeText), entry(largeText), entry(largeText), entry(largeText),
                                                          entry(largeText));

        List<List<ExternalOperationLogEntry>> batches = batcher.batch(entries);

        assertTrue(batches.size() > 1);
        assertEquals(5, totalEntries(batches));
    }

    @Test
    void batch_oversizedSingleEntryStillProducesItsOwnBatch() {
        String hugeText = "y".repeat(4 * 1024 * 1024);

        List<List<ExternalOperationLogEntry>> batches = batcher.batch(List.of(entry(hugeText)));

        assertEquals(1, batches.size());
        assertEquals(1, batches.get(0)
                               .size());
    }

    private static int totalEntries(List<List<ExternalOperationLogEntry>> batches) {
        return batches.stream()
                      .mapToInt(List::size)
                      .sum();
    }

    private static ExternalOperationLogEntry entry(String message) {
        return ImmutableExternalOperationLogEntry.builder()
                                                 .message(message)
                                                 .timestamp("2024-01-15T10:30:00Z")
                                                 .id("id")
                                                 .level("INFO")
                                                 .correlationId("op-123")
                                                 .operationLogName("test-log")
                                                 .build();
    }
}
