package org.cloudfoundry.multiapps.controller.client.facade.adapters;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@JsonRootName("envelopes")
@JsonDeserialize(as = ImmutableApplicationLogsResponse.class)
public interface ApplicationLogsResponse {

    @JsonProperty("batch")
    List<ApplicationLogEntity> getLogs();
}
