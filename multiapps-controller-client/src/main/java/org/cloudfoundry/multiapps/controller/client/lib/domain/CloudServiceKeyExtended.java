package org.cloudfoundry.multiapps.controller.client.lib.domain;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceKey;

@Value.Immutable
@JsonSerialize(as = ImmutableCloudServiceKeyExtended.class)
@JsonDeserialize(as = ImmutableCloudServiceKeyExtended.class)
public abstract class CloudServiceKeyExtended extends CloudServiceKey {

    @Override
    public abstract CloudServiceInstanceExtended getServiceInstance();

    @Override
    public CloudServiceKeyExtended derive() {
        return this;
    }

}
