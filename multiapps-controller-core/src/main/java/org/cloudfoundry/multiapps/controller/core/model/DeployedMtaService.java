package org.cloudfoundry.multiapps.controller.core.model;

import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.cloudfoundry.multiapps.common.Nullable;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableDeployedMtaService.class)
@JsonDeserialize(builder = ImmutableDeployedMtaService.Builder.class)
public abstract class DeployedMtaService extends CloudServiceInstance {

    @Nullable
    public abstract String getResourceName();

}
