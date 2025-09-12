package org.cloudfoundry.multiapps.controller.persistence.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.cloudfoundry.multiapps.common.Nullable;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableExternalOperationLogEntry.class)
@JsonDeserialize(as = ImmutableExternalOperationLogEntry.class)
public interface ExternalOperationLogEntry {

    @JsonProperty("msg")
    String getMessage();

    @JsonProperty("date")
    String getTimestamp();

    @JsonProperty("correlation_id")
    @Nullable
    String getCorrelationId();

}
