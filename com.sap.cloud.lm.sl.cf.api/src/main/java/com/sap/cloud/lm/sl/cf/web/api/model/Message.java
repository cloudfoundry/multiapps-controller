package com.sap.cloud.lm.sl.cf.web.api.model;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sap.cloud.lm.sl.common.Nullable;

import io.swagger.annotations.ApiModelProperty;

@Value.Immutable
@JsonSerialize(as = ImmutableMessage.class)
@JsonDeserialize(as = ImmutableMessage.class)
public interface Message {

    @Nullable
    @ApiModelProperty
    @JsonProperty("id")
    Long getId();

    @Nullable
    @ApiModelProperty
    @JsonProperty("text")
    String getText();

    @Nullable
    @ApiModelProperty
    @JsonProperty("type")
    MessageType getType();

}
