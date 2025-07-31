package org.cloudfoundry.multiapps.controller.client.facade.adapters;

import org.cloudfoundry.client.v3.domains.Domain;
import org.immutables.value.Value;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudDomain;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudDomain;

@Value.Immutable
public abstract class RawCloudDomain extends RawCloudEntity<CloudDomain> {

    @Value.Parameter
    public abstract Domain getResource();

    @Override
    public CloudDomain derive() {
        Domain resource = getResource();
        return ImmutableCloudDomain.builder()
                                   .metadata(parseResourceMetadata(resource))
                                   .name(resource.getName())
                                   .build();
    }

}
