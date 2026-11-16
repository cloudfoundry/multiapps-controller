package org.cloudfoundry.multiapps.controller.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.annotations.ApiModelProperty;
import org.cloudfoundry.multiapps.common.Nullable;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableFileUrl.class)
@JsonDeserialize(as = ImmutableFileUrl.class)
public interface FileUrl {

    @Value.Parameter
    @ApiModelProperty(accessMode = ApiModelProperty.AccessMode.READ_WRITE)
    @JsonProperty("file_url")
    String getFileUrl();

    @Nullable
    @Value.Parameter
    @ApiModelProperty(accessMode = ApiModelProperty.AccessMode.READ_WRITE)
    @JsonProperty("credentials")
    UserCredentials getUserCredentials();
    //this could potentially contain a TLS certificate as well, if the remote endpoint is a custom registry/repository
}
