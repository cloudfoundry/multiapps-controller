package com.sap.cloud.lm.sl.cf.web.api.model;

import java.util.Date;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sap.cloud.lm.sl.cf.web.api.Nullable;

import io.swagger.annotations.ApiModelProperty;

@Value.Immutable
@JsonSerialize(as = ImmutableLog.class)
@JsonDeserialize(as = ImmutableLog.class)
public interface Log {

    @Nullable
    @ApiModelProperty
    @JsonProperty("id")
    String getId();

    @Nullable
    @ApiModelProperty
    @JsonProperty("lastModified")
    Date getLastModified();

    @Nullable
    @ApiModelProperty
    @JsonProperty("content")
    String getContent();

    @Nullable
    @ApiModelProperty
    @JsonProperty("size")
    Long getSize();

    @Nullable
    @ApiModelProperty
    @JsonProperty("displayName")
    String getDisplayName();

    @Nullable
    @ApiModelProperty
    @JsonProperty("description")
    String getDescription();

    @Nullable
    @ApiModelProperty
    @JsonProperty("externalInfo")
    String getExternalInfo();

}
