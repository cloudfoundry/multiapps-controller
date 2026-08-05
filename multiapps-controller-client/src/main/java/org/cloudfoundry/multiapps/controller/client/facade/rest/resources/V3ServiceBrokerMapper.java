package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import java.util.Optional;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceBroker;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudServiceBroker;

/**
 * Maps the {@link V3ServiceBroker} wire model to the project's {@link CloudServiceBroker} domain object. Mirrors the OSS
 * {@code RawCloudServiceBroker} adapter field-for-field, so both client implementations yield identical domain objects.
 *
 * <p>
 * As in the OSS adapter, {@code username}/{@code password} are write-only credentials that CF never returns, so they are not mapped;
 * only {@code name}, {@code url} and the (optional) space GUID from {@code relationships.space.data.guid} are populated.
 * </p>
 */
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
