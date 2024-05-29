package org.cloudfoundry.multiapps.controller.persistence.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.cloudfoundry.multiapps.common.Nullable;
import org.immutables.value.Value;

import java.time.LocalDateTime;

@Value.Immutable
@JsonSerialize(as = ImmutableOperationLogEntry.class)
@JsonDeserialize(as = ImmutableOperationLogEntry.class)
public interface OperationLogEntry {

    @Nullable
    String getId();

    @Nullable
    String getSpace();

    @Nullable
    String getNamespace();

    @Nullable
    LocalDateTime getModified();

    @Nullable
    String getOperationId();

    @Nullable
    String getOperationLog();

    @Nullable
    String getOperationLogName();
}
