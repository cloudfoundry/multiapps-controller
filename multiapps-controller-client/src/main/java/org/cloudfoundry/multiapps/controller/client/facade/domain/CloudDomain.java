package org.cloudfoundry.multiapps.controller.client.facade.domain;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableCloudDomain.class)
@JsonDeserialize(as = ImmutableCloudDomain.class)
public abstract class CloudDomain extends CloudEntity implements Derivable<CloudDomain> {

    @Override
    public CloudDomain derive() {
        return this;
    }

}
