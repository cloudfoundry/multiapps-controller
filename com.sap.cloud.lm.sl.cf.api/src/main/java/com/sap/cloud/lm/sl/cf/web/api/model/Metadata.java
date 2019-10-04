package com.sap.cloud.lm.sl.cf.web.api.model;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sap.cloud.lm.sl.cf.web.api.Nullable;

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

}
