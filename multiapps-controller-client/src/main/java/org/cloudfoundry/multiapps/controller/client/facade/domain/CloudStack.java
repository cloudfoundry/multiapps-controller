package org.cloudfoundry.multiapps.controller.client.facade.domain;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.cloudfoundry.multiapps.controller.client.facade.Nullable;

@Value.Immutable
@JsonSerialize(as = ImmutableCloudStack.class)
@JsonDeserialize(as = ImmutableCloudStack.class)
public abstract class CloudStack extends CloudEntity implements Derivable<CloudStack> {

    @Nullable
    public abstract String getDescription();

    @Override
    public CloudStack derive() {
        return this;
    }

}
