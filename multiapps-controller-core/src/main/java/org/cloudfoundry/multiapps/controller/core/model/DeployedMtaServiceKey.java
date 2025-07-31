package org.cloudfoundry.multiapps.controller.core.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.cloudfoundry.multiapps.common.Nullable;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceKey;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableDeployedMtaServiceKey.class)
@JsonDeserialize(builder = ImmutableDeployedMtaServiceKey.Builder.class)
public abstract class DeployedMtaServiceKey extends CloudServiceKey {

    @Nullable
    public abstract String getResourceName();

}
