package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudDomain;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudDomain;

/**
 * Maps the {@link V3Domain} wire model to the project's {@link CloudDomain} domain object. Mirrors the OSS {@code RawCloudDomain} adapter
 * field-for-field (metadata from the resource envelope + {@code name}), so both client implementations yield identical domain objects.
 */
public final class V3DomainMapper {

    private V3DomainMapper() {
    }

    public static CloudDomain toCloudDomain(V3Domain domain) {
        return ImmutableCloudDomain.builder()
                                   .metadata(V3ResourceMappers.parseMetadata(domain.guid(), domain.createdAt(), domain.updatedAt()))
                                   .name(domain.name())
                                   .build();
    }

}
