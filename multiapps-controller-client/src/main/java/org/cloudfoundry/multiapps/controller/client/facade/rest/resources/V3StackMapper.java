package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudStack;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudStack;

/**
 * Maps the {@link V3Stack} wire model to the project's {@link CloudStack} domain object. Mirrors the OSS
 * {@code RawCloudStack} adapter field-for-field, so both client implementations yield identical domain objects.
 */
public final class V3StackMapper {

    private V3StackMapper() {
    }

    public static CloudStack toCloudStack(V3Stack stack) {
        return ImmutableCloudStack.builder()
                                  .metadata(V3ResourceMappers.parseMetadata(stack.guid(), stack.createdAt(), stack.updatedAt()))
                                  .name(stack.name())
                                  .description(stack.description())
                                  .build();
    }

}
