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
    @ApiModelProperty(value = "")
    @JsonProperty("id")
    String getId();

    @Nullable
    @ApiModelProperty(value = "")
    @JsonProperty("lastModified")
    Date getLastModified();

    @Nullable
    @ApiModelProperty(value = "")
    @JsonProperty("content")
    String getContent();

    @Nullable
    @ApiModelProperty(value = "")
    @JsonProperty("size")
    Long getSize();

    @Nullable
    @ApiModelProperty(value = "")
    @JsonProperty("displayName")
    String getDisplayName();

    @Nullable
    @ApiModelProperty(value = "")
    @JsonProperty("description")
    String getDescription();

    @Nullable
    @ApiModelProperty(value = "")
    @JsonProperty("externalInfo")
    String getExternalInfo();

}
