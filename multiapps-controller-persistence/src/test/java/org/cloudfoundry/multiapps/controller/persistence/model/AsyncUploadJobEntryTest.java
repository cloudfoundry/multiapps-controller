package org.cloudfoundry.multiapps.controller.persistence.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Month;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AsyncUploadJobEntryTest {

    private static final String JOB_ID = "job-id";
    private static final String FILE_ID = "file-id";
    private static final String SPACE_GUID = "space-guid";
    private static final String USER = "user";
    private static final String URL = "https://user:secret@example.com/my.mtar";
    private static final LocalDateTime ADDED_AT = LocalDateTime.of(2026, Month.AUGUST, 3, 10, 0, 0);
    private static final LocalDateTime STARTED_AT = ADDED_AT.plusSeconds(30);
    private static final LocalDateTime FINISHED_AT = STARTED_AT.plusSeconds(90);

    @Test
    void testTimingsForFinishedJob() {
        AsyncUploadJobEntry job = baseBuilder().state(AsyncUploadJobEntry.State.FINISHED)
                                               .addedAt(ADDED_AT)
                                               .startedAt(STARTED_AT)
                                               .finishedAt(FINISHED_AT)
                                               .build();

        Assertions.assertEquals(Duration.ofSeconds(30), job.getQueueWaitTime());
        Assertions.assertEquals(Duration.ofSeconds(90), job.getUploadDuration());
        Assertions.assertEquals(Duration.ofSeconds(120), job.getTotalTime());
    }

    @Test
    void testTimingsAreNullWhenTimestampsMissing() {
        AsyncUploadJobEntry runningJob = baseBuilder().state(AsyncUploadJobEntry.State.RUNNING)
                                                      .addedAt(ADDED_AT)
                                                      .startedAt(STARTED_AT)
                                                      .build();

        Assertions.assertEquals(Duration.ofSeconds(30), runningJob.getQueueWaitTime());
        Assertions.assertNull(runningJob.getUploadDuration());
        Assertions.assertNull(runningJob.getTotalTime());

        AsyncUploadJobEntry initialJob = baseBuilder().state(AsyncUploadJobEntry.State.INITIAL)
                                                      .addedAt(ADDED_AT)
                                                      .build();

        Assertions.assertNull(initialJob.getQueueWaitTime());
        Assertions.assertNull(initialJob.getUploadDuration());
        Assertions.assertNull(initialJob.getTotalTime());
    }

    @Test
    void testLogSafeSummaryContainsTimingsAndFileId() {
        AsyncUploadJobEntry job = baseBuilder().state(AsyncUploadJobEntry.State.FINISHED)
                                               .mtaId("my-mta")
                                               .bytesRead(2048L)
                                               .addedAt(ADDED_AT)
                                               .startedAt(STARTED_AT)
                                               .finishedAt(FINISHED_AT)
                                               .build();

        String summary = job.buildLogSummary();

        Assertions.assertTrue(summary.contains(JOB_ID), summary);
        Assertions.assertTrue(summary.contains(FILE_ID), summary);
        Assertions.assertTrue(summary.contains("my-mta"), summary);
        Assertions.assertTrue(summary.contains("queueWaitTime: 30000 ms"), summary);
        Assertions.assertTrue(summary.contains("uploadDuration: 90000 ms"), summary);
        Assertions.assertTrue(summary.contains("totalTime: 120000 ms"), summary);
    }

    @Test
    void testLogSafeSummaryHidesSensitiveData() {
        AsyncUploadJobEntry job = baseBuilder().state(AsyncUploadJobEntry.State.RUNNING)
                                               .addedAt(ADDED_AT)
                                               .startedAt(STARTED_AT)
                                               .build();

        String summary = job.buildLogSummary();

        Assertions.assertFalse(summary.contains(URL), summary);
        Assertions.assertFalse(summary.contains("secret"), summary);
        Assertions.assertFalse(summary.contains(USER), summary);
        Assertions.assertFalse(summary.contains(SPACE_GUID), summary);
    }

    @Test
    void testLogSafeSummaryRendersMissingTimingsAsNotAvailable() {
        AsyncUploadJobEntry job = baseBuilder().state(AsyncUploadJobEntry.State.INITIAL)
                                               .addedAt(ADDED_AT)
                                               .build();

        String summary = job.buildLogSummary();

        Assertions.assertTrue(summary.contains("queueWaitTime: " + AsyncUploadJobEntry.NOT_AVAILABLE), summary);
        Assertions.assertTrue(summary.contains("uploadDuration: " + AsyncUploadJobEntry.NOT_AVAILABLE), summary);
        Assertions.assertTrue(summary.contains("totalTime: " + AsyncUploadJobEntry.NOT_AVAILABLE), summary);
    }

    private ImmutableAsyncUploadJobEntry.Builder baseBuilder() {
        return ImmutableAsyncUploadJobEntry.builder()
                                           .id(JOB_ID)
                                           .fileId(FILE_ID)
                                           .user(USER)
                                           .url(URL)
                                           .spaceGuid(SPACE_GUID)
                                           .instanceIndex(0);
    }
}
