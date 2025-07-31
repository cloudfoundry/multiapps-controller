package org.cloudfoundry.multiapps.controller.client.facade.domain;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.cloudfoundry.multiapps.controller.client.facade.Nullable;

@Value.Immutable
@JsonSerialize(as = ImmutableErrorDetails.class)
@JsonDeserialize(as = ImmutableErrorDetails.class)
public interface ErrorDetails {

    @Value.Default
    default long getCode() {
        return 0;
    }

    @Nullable
    String getDescription();

    @Nullable
    String getErrorCode();

}