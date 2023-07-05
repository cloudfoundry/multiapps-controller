package org.cloudfoundry.multiapps.controller.persistence.model;

import org.cloudfoundry.multiapps.common.Nullable;
import org.immutables.value.Value;

import java.time.LocalDateTime;

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
