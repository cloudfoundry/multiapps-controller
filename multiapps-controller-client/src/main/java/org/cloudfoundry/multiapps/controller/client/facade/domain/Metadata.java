package org.cloudfoundry.multiapps.controller.client.facade.domain;

import java.util.Map;

import org.cloudfoundry.multiapps.controller.client.facade.AllowNulls;
import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableMetadata.class)
@JsonDeserialize(as = ImmutableMetadata.class)
public abstract class Metadata {

    @AllowNulls
    @JsonProperty("labels")
    public abstract Map<String, String> getLabels();

    @AllowNulls
    @JsonProperty("annotations")
    public abstract Map<String, String> getAnnotations();

}
