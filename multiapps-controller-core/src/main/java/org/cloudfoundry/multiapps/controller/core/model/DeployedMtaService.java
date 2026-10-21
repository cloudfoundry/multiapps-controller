package org.cloudfoundry.multiapps.controller.core.model;

import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;
import org.cloudfoundry.multiapps.common.Nullable;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceInstance;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableDeployedMtaService.class)
@JsonDeserialize(builder = ImmutableDeployedMtaService.Builder.class)
public abstract class DeployedMtaService extends CloudServiceInstance {

    @Nullable
    public abstract String getResourceName();

}
