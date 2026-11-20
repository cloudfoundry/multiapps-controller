package org.cloudfoundry.multiapps.controller.api.model;

import java.util.List;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.swagger.annotations.ApiModelProperty;

@Value.Immutable
@JsonSerialize(as = ImmutableMtaGraph.class)
@JsonDeserialize(as = ImmutableMtaGraph.class)
public interface MtaGraph {

    @ApiModelProperty
    @JsonProperty("nodes")
    List<MtaGraphNode> getNodes();

    @ApiModelProperty
    @JsonProperty("edges")
    List<MtaGraphEdge> getEdges();

}
