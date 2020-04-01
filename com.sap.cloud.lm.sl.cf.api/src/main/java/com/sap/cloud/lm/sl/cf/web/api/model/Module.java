package com.sap.cloud.lm.sl.cf.web.api.model;

import java.util.Date;
import java.util.List;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sap.cloud.lm.sl.common.Nullable;

import io.swagger.annotations.ApiModelProperty;

@Value.Immutable
@JsonSerialize(as = ImmutableModule.class)
@JsonDeserialize(as = ImmutableModule.class)
public interface Module {

    @Nullable
    @ApiModelProperty
    @JsonProperty("moduleName")
    String getModuleName();

    @Nullable
    @ApiModelProperty
    @JsonProperty("appName")
    String getAppName();

    @Nullable
    @ApiModelProperty
    @JsonProperty("createdOn")
    Date getCreatedOn();

    @Nullable
    @ApiModelProperty
    @JsonProperty("updatedOn")
    Date getUpdatedOn();

    // FIXME: This name should be changed in the API as it contains a typo.
    @ApiModelProperty
    @JsonProperty("providedDendencyNames")
    List<String> getProvidedDendencyNames();

    @ApiModelProperty
    @JsonProperty("services")
    List<String> getServices();

    @ApiModelProperty
    @JsonProperty("uris")
    List<String> getUris();

}
