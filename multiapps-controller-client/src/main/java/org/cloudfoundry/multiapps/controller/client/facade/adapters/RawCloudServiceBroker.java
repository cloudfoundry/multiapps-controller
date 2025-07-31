package org.cloudfoundry.multiapps.controller.client.facade.adapters;

import java.util.Optional;

import org.cloudfoundry.client.v3.Relationship;
import org.cloudfoundry.client.v3.ToOneRelationship;
import org.cloudfoundry.client.v3.servicebrokers.ServiceBroker;
import org.cloudfoundry.client.v3.servicebrokers.ServiceBrokerRelationships;
import org.immutables.value.Value;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceBroker;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudServiceBroker;

@Value.Immutable
public abstract class RawCloudServiceBroker extends RawCloudEntity<CloudServiceBroker> {

    @Value.Parameter
    public abstract ServiceBroker getServiceBroker();

    @Override
    public CloudServiceBroker derive() {
        ServiceBroker serviceBroker = getServiceBroker();
        String spaceGuid = getSpaceGuid(serviceBroker);
        return ImmutableCloudServiceBroker.builder()
                                          .metadata(parseResourceMetadata(serviceBroker))
                                          .name(serviceBroker.getName())
                                          .url(serviceBroker.getUrl())
                                          .spaceGuid(spaceGuid)
                                          .build();
    }

    private String getSpaceGuid(ServiceBroker serviceBroker) {
        return Optional.ofNullable(serviceBroker.getRelationships())
                       .map(ServiceBrokerRelationships::getSpace)
                       .map(ToOneRelationship::getData)
                       .map(Relationship::getId)
                       .orElse(null);
    }

}
