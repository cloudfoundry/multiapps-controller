package org.cloudfoundry.multiapps.controller.client.lib.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sap.cloudfoundry.client.facade.domain.CloudRoute;
import org.immutables.value.Value;

import java.util.List;

@JsonSerialize(as = ImmutableCloudRouteExtended.class)
@JsonDeserialize(as = ImmutableCloudRouteExtended.class)
@Value.Immutable
public abstract class CloudRouteExtended extends CloudRoute {

    public abstract List<String> getBoundServiceInstanceGuids();

}
