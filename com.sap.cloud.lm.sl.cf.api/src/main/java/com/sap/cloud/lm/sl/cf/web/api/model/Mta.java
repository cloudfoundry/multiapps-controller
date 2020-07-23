package com.sap.cloud.lm.sl.cf.web.api.model;

import java.util.List;
import java.util.Set;

import org.cloudfoundry.multiapps.common.Nullable;
import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.swagger.annotations.ApiModelProperty;

@Value.Immutable
@JsonSerialize(as = ImmutableMta.class)
@JsonDeserialize(as = ImmutableMta.class)
public interface Mta {

    @Nullable
    @ApiModelProperty
    @JsonProperty("metadata")
    Metadata getMetadata();

    @ApiModelProperty
    @JsonProperty("modules")
    List<Module> getModules();

    @ApiModelProperty
    @JsonProperty("services")
    Set<String> getServices();

}
