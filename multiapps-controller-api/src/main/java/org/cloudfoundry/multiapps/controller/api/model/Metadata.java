package org.cloudfoundry.multiapps.controller.api.model;

import org.cloudfoundry.multiapps.common.Nullable;
import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.swagger.annotations.ApiModelProperty;

@Value.Immutable
@JsonSerialize(as = ImmutableMetadata.class)
@JsonDeserialize(as = ImmutableMetadata.class)
public interface Metadata {

    @Nullable
    @ApiModelProperty
    @JsonProperty("id")
    String getId();

    @Nullable
    @ApiModelProperty
    @JsonProperty("version")
    String getVersion();
    
    @Nullable
    @ApiModelProperty
    @JsonProperty("namespace")
    String getNamespace();

}
