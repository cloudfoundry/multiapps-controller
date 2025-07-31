package org.cloudfoundry.multiapps.controller.client.facade.domain;

import java.util.UUID;

import org.cloudfoundry.Nullable;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableCloudServiceBinding.class)
@JsonDeserialize(as = ImmutableCloudServiceBinding.class)
public abstract class CloudServiceBinding extends CloudEntity implements Derivable<CloudServiceBinding> {

    @Nullable
    public abstract UUID getApplicationGuid();

    public abstract UUID getServiceInstanceGuid();

    @Nullable
    public abstract ServiceCredentialBindingOperation getServiceBindingOperation();

    @Override
    public CloudServiceBinding derive() {
        return this;
    }

}
