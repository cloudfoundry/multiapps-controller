package org.cloudfoundry.multiapps.controller.client.facade.domain;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.cloudfoundry.multiapps.controller.client.facade.Nullable;

@Value.Immutable
@JsonSerialize(as = ImmutableUpload.class)
@JsonDeserialize(as = ImmutableUpload.class)
public interface Upload {

    Status getStatus();

    @Nullable
    ErrorDetails getErrorDetails();

}
