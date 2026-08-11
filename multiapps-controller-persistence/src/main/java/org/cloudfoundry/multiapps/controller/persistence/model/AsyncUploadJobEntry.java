package org.cloudfoundry.multiapps.controller.persistence.model;

import java.text.MessageFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.cloudfoundry.multiapps.common.Nullable;
import org.immutables.value.Value;

@Value.Immutable
public interface AsyncUploadJobEntry {

    String STALE_JOB_DETAILS_FORMAT = "Stale job details - id: {0}, state: {1}, updatedAt: {2}, addedAt: {3}, startedAt: {4}, bytesRead: {5}, url: {6}, space: {7}, namespace: {8}, user: {9}, instance: {10}";

    String ASYNC_UPLOAD_JOB_SUMMARY_FORMAT = "id: {0}, state: {1}, fileId: {2}, mtaId: {3}, schemaVersion: {4}, instanceIndex: {5}, bytesRead: {6}, addedAt: {7}, startedAt: {8}, finishedAt: {9}, updatedAt: {10}, queueWaitTime: {11}, uploadDuration: {12}, totalTime: {13}, error: {14}";

    String NOT_AVAILABLE = "N/A";

    enum State {
        INITIAL, RUNNING, FINISHED, ERROR
    }

    String getId();

    State getState();

    String getUser();

    String getUrl();

    @Nullable
    LocalDateTime getAddedAt();

    @Nullable
    LocalDateTime getStartedAt();

    @Nullable
    LocalDateTime getFinishedAt();

    String getSpaceGuid();

    @Nullable
    String getNamespace();

    @Nullable
    String getError();

    @Nullable
    String getFileId();

    @Nullable
    String getMtaId();

    @Nullable
    String getSchemaVersion();

    Integer getInstanceIndex();

    @Nullable
    Long getBytesRead();

    @Nullable
    LocalDateTime getUpdatedAt();

    default String buildStaleDetailsLogMessage() {
        return MessageFormat.format(STALE_JOB_DETAILS_FORMAT, getId(), getState(), getUpdatedAt(), getAddedAt(), getStartedAt(),
                                    getBytesRead(), getUrl(), getSpaceGuid(), getNamespace(), getUser(), getInstanceIndex());
    }

    default String buildLogSummary() {
        return MessageFormat.format(ASYNC_UPLOAD_JOB_SUMMARY_FORMAT, getId(), getState(), getFileId(), getMtaId(), getSchemaVersion(),
                                    getInstanceIndex(), getBytesRead(), getAddedAt(), getStartedAt(), getFinishedAt(), getUpdatedAt(),
                                    formatDuration(getQueueWaitTime()), formatDuration(getUploadDuration()), formatDuration(getTotalTime()),
                                    getError());
    }

    @Nullable
    default Duration getQueueWaitTime() {
        return durationBetween(getAddedAt(), getStartedAt());
    }

    @Nullable
    default Duration getUploadDuration() {
        return durationBetween(getStartedAt(), getFinishedAt());
    }

    @Nullable
    default Duration getTotalTime() {
        return durationBetween(getAddedAt(), getFinishedAt());
    }

    private static Duration durationBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return null;
        }
        return Duration.between(start.toInstant(ZoneOffset.UTC), end.toInstant(ZoneOffset.UTC));
    }

    private static String formatDuration(Duration duration) {
        if (duration == null) {
            return NOT_AVAILABLE;
        }
        return duration.toMillis() + " ms";
    }
}
