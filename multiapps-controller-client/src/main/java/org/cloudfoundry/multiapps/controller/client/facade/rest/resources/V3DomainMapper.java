package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudDomain;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudDomain;

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
