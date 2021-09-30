package org.cloudfoundry.multiapps.controller.api.model;

import java.time.ZonedDateTime;

import org.cloudfoundry.multiapps.common.Nullable;
import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

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
    @JsonSerialize(using = ZonedDateTimeSerializer.class)
    @JsonDeserialize(using = ZonedDateTimeDeserializer.class)
    @JsonProperty("timestamp")
    ZonedDateTime getTimestamp();

    @Nullable
    @ApiModelProperty
    @JsonProperty("type")
    MessageType getType();

}
