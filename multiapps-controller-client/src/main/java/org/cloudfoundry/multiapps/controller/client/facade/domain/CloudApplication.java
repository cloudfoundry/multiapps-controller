package org.cloudfoundry.multiapps.controller.client.facade.domain;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.cloudfoundry.multiapps.controller.client.facade.Nullable;

@Value.Immutable
@JsonSerialize(as = ImmutableCloudApplication.class)
@JsonDeserialize(as = ImmutableCloudApplication.class)
public abstract class CloudApplication extends CloudEntity implements Derivable<CloudApplication> {

    public enum State {
        STARTED, STOPPED
    }

    @Nullable
    public abstract State getState();

    @Nullable
    public abstract Lifecycle getLifecycle();

    @Nullable
    public abstract CloudSpace getSpace();

    @Override
    public CloudApplication derive() {
        return this;
    }

}
