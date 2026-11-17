package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import java.util.Optional;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceBroker;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudServiceBroker;

public final class V3ServiceBrokerMapper {

    private V3ServiceBrokerMapper() {
    }

    public static CloudServiceBroker toCloudServiceBroker(V3ServiceBroker serviceBroker) {
        return ImmutableCloudServiceBroker.builder()
                                          .metadata(V3ResourceMappers.parseMetadata(serviceBroker.guid(), serviceBroker.createdAt(),
                                                                                    serviceBroker.updatedAt()))
                                          .name(serviceBroker.name())
                                          .url(serviceBroker.url())
                                          .spaceGuid(getSpaceGuid(serviceBroker))
                                          .build();
    }

    private static String getSpaceGuid(V3ServiceBroker serviceBroker) {
        return Optional.ofNullable(serviceBroker.relationships())
                       .map(V3ServiceBroker.V3Relationships::space)
                       .map(V3ServiceBroker.V3ToOneRelationship::data)
                       .map(V3ServiceBroker.V3RelationshipData::guid)
                       .orElse(null);
    }

}
