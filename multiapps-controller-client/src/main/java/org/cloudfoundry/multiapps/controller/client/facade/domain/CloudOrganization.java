package org.cloudfoundry.multiapps.controller.client.facade.domain;

import org.immutables.value.Value;

import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableCloudOrganization.class)
@JsonDeserialize(as = ImmutableCloudOrganization.class)
public abstract class CloudOrganization extends CloudEntity implements Derivable<CloudOrganization> {

    @Override
    public CloudOrganization derive() {
        return this;
    }

}
