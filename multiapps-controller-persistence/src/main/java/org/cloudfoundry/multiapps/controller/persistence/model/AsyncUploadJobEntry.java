package org.cloudfoundry.multiapps.controller.persistence.model;

import java.text.MessageFormat;
import java.time.LocalDateTime;

import org.cloudfoundry.multiapps.common.Nullable;
import org.immutables.value.Value;

@Value.Immutable
public interface AsyncUploadJobEntry {

    String STALE_JOB_DETAILS_FORMAT = "Stale job details - id: {0}, state: {1}, updatedAt: {2}, addedAt: {3}, startedAt: {4}, bytesRead: {5}, url: {6}, space: {7}, namespace: {8}, user: {9}, instance: {10}";

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
}
