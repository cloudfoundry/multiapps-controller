package org.cloudfoundry.multiapps.controller.api.model;

import org.cloudfoundry.multiapps.common.Nullable;
import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

import io.swagger.annotations.ApiModelProperty;

@Value.Immutable
@JsonSerialize(as = ImmutableInfo.class)
@JsonDeserialize(as = ImmutableInfo.class)
@JsonPropertyOrder({ "api_version" })
public interface Info {

    @Nullable
    @ApiModelProperty
    @JsonProperty("api_version")
    Integer getApiVersion();

}
