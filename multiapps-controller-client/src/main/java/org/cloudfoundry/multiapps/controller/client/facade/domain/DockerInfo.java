package org.cloudfoundry.multiapps.controller.client.facade.domain;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.cloudfoundry.multiapps.controller.client.facade.Nullable;

@Value.Immutable
@JsonSerialize(as = ImmutableDockerInfo.class)
@JsonDeserialize(as = ImmutableDockerInfo.class)
public interface DockerInfo {

    String getImage();

    @Nullable
    DockerCredentials getCredentials();

}
