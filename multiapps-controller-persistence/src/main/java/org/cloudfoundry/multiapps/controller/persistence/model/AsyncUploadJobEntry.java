package org.cloudfoundry.multiapps.controller.persistence.model;

import java.time.LocalDateTime;

import org.cloudfoundry.multiapps.common.Nullable;
import org.immutables.value.Value;

@Value.Immutable
public interface AsyncUploadJobEntry {

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

    Integer getInstanceIndex();
}
