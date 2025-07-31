package org.cloudfoundry.multiapps.controller.client.lib.domain;

import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudRoute;
import org.immutables.value.Value;

@JsonSerialize(as = ImmutableCloudRouteExtended.class)
@JsonDeserialize(as = ImmutableCloudRouteExtended.class)
@Value.Immutable
public abstract class CloudRouteExtended extends CloudRoute {

    public abstract List<String> getBoundServiceInstanceGuids();

}
