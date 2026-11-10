package org.cloudfoundry.multiapps.controller.api.model;

import org.cloudfoundry.multiapps.common.Nullable;
import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.swagger.annotations.ApiModelProperty;

@Value.Immutable
@JsonSerialize(as = ImmutableUserCredentials.class)
@JsonDeserialize(as = ImmutableUserCredentials.class)
public interface UserCredentials {

    @Nullable
    @Value.Parameter
    @ApiModelProperty(accessMode = ApiModelProperty.AccessMode.READ_WRITE)
    @JsonProperty("username")
    String getUsername();

    @Nullable
    @Value.Parameter
    @ApiModelProperty(accessMode = ApiModelProperty.AccessMode.READ_WRITE)
    @JsonProperty("password")
    String getPassword();
    //this could potentially contain a TLS certificate as well, if the remote endpoint is a custom registry/repository

    @Nullable
    @Value.Parameter
    @ApiModelProperty(accessMode = ApiModelProperty.AccessMode.READ_WRITE)
    @JsonProperty("token")
    String getToken();
}
