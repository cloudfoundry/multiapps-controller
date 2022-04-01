package org.cloudfoundry.multiapps.controller.core.model;

import org.cloudfoundry.multiapps.common.Nullable;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceKey;

@Value.Immutable
@JsonSerialize(as = ImmutableDeployedMtaServiceKey.class)
@JsonDeserialize(builder = ImmutableDeployedMtaServiceKey.Builder.class)
public abstract class DeployedMtaServiceKey extends CloudServiceKey {

    @Nullable
    public abstract String getResourceName();

}
