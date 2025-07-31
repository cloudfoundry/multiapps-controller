package org.cloudfoundry.multiapps.controller.client.facade.adapters;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceInstance;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudServiceInstance;
import org.cloudfoundry.client.v3.serviceinstances.ServiceInstance;
import org.immutables.value.Value;

@Value.Immutable
public abstract class RawV3CloudServiceInstance extends RawCloudEntity<CloudServiceInstance> {

    @Value.Parameter
    public abstract ServiceInstance getServiceInstance();

    @Override
    public CloudServiceInstance derive() {
        ServiceInstance serviceInstance = getServiceInstance();
        return ImmutableCloudServiceInstance.builder()
                                            .metadata(parseResourceMetadata(serviceInstance))
                                            .v3Metadata(serviceInstance.getMetadata())
                                            .name(serviceInstance.getName())
                                            .tags(serviceInstance.getTags())
                                            .build();
    }

}
