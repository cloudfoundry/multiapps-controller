package org.cloudfoundry.multiapps.controller.api.model;

import org.cloudfoundry.multiapps.common.Nullable;
import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableUserCredentials.class)
@JsonDeserialize(as = ImmutableUserCredentials.class)
public interface UserCredentials {

    @Nullable
    @Value.Parameter
    @JsonProperty("username")
    String getUsername();

    @Nullable
    @Value.Parameter
    @JsonProperty("password")
    String getPassword();
    //this could potentially contain a TLS certificate as well, if the remote endpoint is a custom registry/repository
}
