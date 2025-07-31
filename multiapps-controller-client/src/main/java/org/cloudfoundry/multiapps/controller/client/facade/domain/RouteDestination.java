package org.cloudfoundry.multiapps.controller.client.facade.domain;

import java.util.UUID;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.cloudfoundry.multiapps.controller.client.facade.Nullable;

@Value.Immutable
@JsonSerialize(as = ImmutableRouteDestination.class)
@JsonDeserialize(as = ImmutableRouteDestination.class)
public abstract class RouteDestination extends CloudEntity implements Derivable<RouteDestination> {

    public abstract UUID getApplicationGuid();

    @Nullable
    public abstract Integer getPort();

    @Nullable
    public abstract Integer getWeight();

    public abstract String getProtocol();

    @Override
    public RouteDestination derive() {
        return this;
    }
}
