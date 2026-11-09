package org.cloudfoundry.multiapps.controller.api.model;

import org.cloudfoundry.multiapps.common.Nullable;
import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableFailureReason.class)
@JsonDeserialize(as = ImmutableFailureReason.class)
public interface FailureReason {

    enum Code {
        CONTENT_VALIDATION_ERROR,
        MODULE_STARTUP_FAILURE,
        SERVICE_OPERATION_FAILURE,
        CF_API_UNAVAILABLE,
        QUOTA_EXCEEDED,
        INTERNAL_ERROR
    }

    @Nullable
    @JsonProperty("code")
    Code getCode();

    @Nullable
    @JsonProperty("retryable")
    Boolean getRetryable();

    @Nullable
    @JsonProperty("summary")
    String getSummary();

}
