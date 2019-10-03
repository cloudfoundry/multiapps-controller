package com.sap.cloud.lm.sl.cf.web.api.model;

import java.util.Date;
import java.util.List;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sap.cloud.lm.sl.cf.web.api.Nullable;

import io.swagger.annotations.ApiModelProperty;

@Value.Immutable
@JsonSerialize(as = ImmutableModule.class)
@JsonDeserialize(as = ImmutableModule.class)
public interface Module {

    @Nullable
    @ApiModelProperty(value = "")
    @JsonProperty("moduleName")
    String getModuleName();

    @Nullable
    @ApiModelProperty(value = "")
    @JsonProperty("appName")
    String getAppName();

    @Nullable
    @ApiModelProperty(value = "")
    @JsonProperty("createdOn")
    Date getCreatedOn();

    @Nullable
    @ApiModelProperty(value = "")
    @JsonProperty("updatedOn")
    Date getUpdatedOn();

    @ApiModelProperty(value = "")
    @JsonProperty("providedDendencyNames")
    List<String> getProvidedDendencyNames();

    @ApiModelProperty(value = "")
    @JsonProperty("services")
    List<String> getServices();

    @ApiModelProperty(value = "")
    @JsonProperty("uris")
    List<String> getUris();

}
