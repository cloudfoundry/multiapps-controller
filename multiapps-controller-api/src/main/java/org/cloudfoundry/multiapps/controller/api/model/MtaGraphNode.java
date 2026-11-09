package org.cloudfoundry.multiapps.controller.api.model;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.swagger.annotations.ApiModelProperty;

@Value.Immutable
@JsonSerialize(as = ImmutableMtaGraphNode.class)
@JsonDeserialize(as = ImmutableMtaGraphNode.class)
public interface MtaGraphNode {

    @ApiModelProperty
    @JsonProperty("id")
    String getId();

    @ApiModelProperty
    @JsonProperty("type")
    String getType();

}
