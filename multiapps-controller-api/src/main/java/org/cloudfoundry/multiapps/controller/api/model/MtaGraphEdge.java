package org.cloudfoundry.multiapps.controller.api.model;

import org.immutables.value.Value;

import org.cloudfoundry.multiapps.common.Nullable;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.swagger.annotations.ApiModelProperty;

@Value.Immutable
@JsonSerialize(as = ImmutableMtaGraphEdge.class)
@JsonDeserialize(as = ImmutableMtaGraphEdge.class)
public interface MtaGraphEdge {

    @ApiModelProperty
    @JsonProperty("from")
    String getFrom();

    @ApiModelProperty
    @JsonProperty("to")
    String getTo();

    @ApiModelProperty
    @JsonProperty("type")
    String getType();

    @Nullable
    @ApiModelProperty
    @JsonProperty("name")
    String getName();

}
